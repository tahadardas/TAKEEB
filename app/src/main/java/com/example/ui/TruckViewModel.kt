package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TruckEntry
import com.example.data.repository.TruckRepository
import com.example.util.ExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

enum class FilterType {
    ALL,
    HEAD_AND_TAIL,
    HEAD_ONLY,
    WITH_EXTENSIONS,
    WITHOUT_EXTENSIONS
}

enum class SortOption {
    NEWEST,
    OLDEST,
    VOLUME_HIGH_TO_LOW,
    VOLUME_LOW_TO_HIGH
}

enum class CalcTarget {
    HEAD,
    TAIL
}

data class UiMessage(val message: String, val isError: Boolean = false)

data class StatisticsData(
    val totalTrucks: Int = 0,
    val totalVolume: Double = 0.0,
    val totalHeadVolume: Double = 0.0,
    val totalTailVolume: Double = 0.0,
    val averageVolume: Double = 0.0,
    val headAndTailCount: Int = 0,
    val headOnlyCount: Int = 0,
    val withExtensionsCount: Int = 0,
    val withoutExtensionsCount: Int = 0
)

class TruckViewModel(private val repository: TruckRepository) : ViewModel() {

    // --- Search & Filter State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FilterType.ALL)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(SortOption.NEWEST)
    val selectedSort: StateFlow<SortOption> = _selectedSort.asStateFlow()

    // --- UI Event messages ---
    private val _uiEvents = MutableSharedFlow<UiMessage>()
    val uiEvents: SharedFlow<UiMessage> = _uiEvents.asSharedFlow()

    // --- Form State ---
    var editingEntryId = MutableStateFlow<Long?>(null)
        private set

    val truckNumber = MutableStateFlow("")
    val driverName = MutableStateFlow("")
    val hasHeadAndTail = MutableStateFlow(true)
    val headVolumeInput = MutableStateFlow("")
    val tailVolumeInput = MutableStateFlow("")
    val hasExtensions = MutableStateFlow(false)
    val extensionDetails = MutableStateFlow("")
    val materialType = MutableStateFlow("")
    val siteLocation = MutableStateFlow("")
    val notes = MutableStateFlow("")

    val truckNumberError = MutableStateFlow<String?>(null)
    val driverNameError = MutableStateFlow<String?>(null)
    val headVolumeError = MutableStateFlow<String?>(null)
    val tailVolumeError = MutableStateFlow<String?>(null)

    // --- Autocomplete & Learned Lists ---
    val distinctTruckNumbers: StateFlow<List<String>> = repository.distinctTruckNumbers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctDriverNames: StateFlow<List<String>> = repository.distinctDriverNames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctMaterials: StateFlow<List<String>> = repository.distinctMaterials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctSites: StateFlow<List<String>> = repository.distinctSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered auto-complete suggestions for truck number
    val truckNumberSuggestions: StateFlow<List<String>> = combine(
        truckNumber,
        distinctTruckNumbers
    ) { current, list ->
        if (current.isBlank()) {
            list.take(5)
        } else {
            list.filter { it.contains(current.trim(), ignoreCase = true) && !it.equals(current.trim(), ignoreCase = true) }.take(5)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered auto-complete suggestions for driver name
    val driverNameSuggestions: StateFlow<List<String>> = combine(
        driverName,
        distinctDriverNames
    ) { current, list ->
        if (current.isBlank()) {
            list.take(5)
        } else {
            list.filter { it.contains(current.trim(), ignoreCase = true) && !it.equals(current.trim(), ignoreCase = true) }.take(5)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Detected duplicate profile for the current entered truck number
    val existingDuplicateEntry = MutableStateFlow<TruckEntry?>(null)

    // Dialog state when user attempts to submit a duplicate truck number
    val showDuplicateConfirmDialog = MutableStateFlow(false)

    // Export & Backup Dialog / Sheet visibility
    val showExportBackupSheet = MutableStateFlow(false)

    // Loading / Exporting state
    val isExporting = MutableStateFlow(false)

    // Calculated dynamic total volume
    val calculatedTotalVolume: StateFlow<Double> = combine(
        hasHeadAndTail,
        headVolumeInput,
        tailVolumeInput
    ) { hasBoth, headStr, tailStr ->
        val head = headStr.toDoubleOrNull() ?: 0.0
        val tail = if (hasBoth) (tailStr.toDoubleOrNull() ?: 0.0) else 0.0
        head + tail
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Dimensions Calculator State ---
    val showCalcDialog = MutableStateFlow(false)
    val calcTarget = MutableStateFlow(CalcTarget.HEAD)
    val calcLength = MutableStateFlow("")
    val calcWidth = MutableStateFlow("")
    val calcHeight = MutableStateFlow("")

    val calcResultVolume: StateFlow<Double> = combine(
        calcLength,
        calcWidth,
        calcHeight
    ) { lStr, wStr, hStr ->
        val l = lStr.toDoubleOrNull() ?: 0.0
        val w = wStr.toDoubleOrNull() ?: 0.0
        val h = hStr.toDoubleOrNull() ?: 0.0
        l * w * h
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Delete confirmation dialog state
    val entryPendingDelete = MutableStateFlow<TruckEntry?>(null)

    // Active bottom navigation tab index (0: Form, 1: Records, 2: Statistics)
    val selectedTab = MutableStateFlow(0)

    // Filtered and Sorted Entries Flow
    val entries: StateFlow<List<TruckEntry>> = combine(
        repository.allEntries,
        _searchQuery,
        _selectedFilter,
        _selectedSort
    ) { all, query, filter, sort ->
        val searchFiltered = if (query.isBlank()) {
            all
        } else {
            val q = query.trim().lowercase(Locale.ROOT)
            all.filter {
                it.truckNumber.lowercase(Locale.ROOT).contains(q) ||
                        it.driverName.lowercase(Locale.ROOT).contains(q) ||
                        it.materialType.lowercase(Locale.ROOT).contains(q) ||
                        it.siteLocation.lowercase(Locale.ROOT).contains(q)
            }
        }

        val categoryFiltered = when (filter) {
            FilterType.ALL -> searchFiltered
            FilterType.HEAD_AND_TAIL -> searchFiltered.filter { it.hasHeadAndTail }
            FilterType.HEAD_ONLY -> searchFiltered.filter { !it.hasHeadAndTail }
            FilterType.WITH_EXTENSIONS -> searchFiltered.filter { it.hasExtensions }
            FilterType.WITHOUT_EXTENSIONS -> searchFiltered.filter { !it.hasExtensions }
        }

        when (sort) {
            SortOption.NEWEST -> categoryFiltered.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> categoryFiltered.sortedBy { it.createdAt }
            SortOption.VOLUME_HIGH_TO_LOW -> categoryFiltered.sortedByDescending { it.totalVolume }
            SortOption.VOLUME_LOW_TO_HIGH -> categoryFiltered.sortedBy { it.totalVolume }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregated Statistics Flow
    val statistics: StateFlow<StatisticsData> = repository.allEntries.combine(
        MutableStateFlow(Unit)
    ) { all, _ ->
        if (all.isEmpty()) {
            StatisticsData()
        } else {
            val totalTrucks = all.size
            val totalVolume = all.sumOf { it.totalVolume }
            val totalHead = all.sumOf { it.headVolume }
            val totalTail = all.sumOf { it.tailVolume }
            val avgVolume = if (totalTrucks > 0) totalVolume / totalTrucks else 0.0
            val headAndTail = all.count { it.hasHeadAndTail }
            val headOnly = all.count { !it.hasHeadAndTail }
            val withExt = all.count { it.hasExtensions }
            val withoutExt = totalTrucks - withExt

            StatisticsData(
                totalTrucks = totalTrucks,
                totalVolume = totalVolume,
                totalHeadVolume = totalHead,
                totalTailVolume = totalTail,
                averageVolume = avgVolume,
                headAndTailCount = headAndTail,
                headOnlyCount = headOnly,
                withExtensionsCount = withExt,
                withoutExtensionsCount = withoutExt
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsData())

    // Real-time check for duplicate truck number as user types
    fun onTruckNumberChange(number: String) {
        truckNumber.value = number
        if (number.isNotBlank()) {
            truckNumberError.value = null
            viewModelScope.launch {
                val found = repository.getEntryByTruckNumber(number)
                if (found != null && (editingEntryId.value == null || editingEntryId.value != found.id)) {
                    existingDuplicateEntry.value = found
                } else {
                    existingDuplicateEntry.value = null
                }
            }
        } else {
            existingDuplicateEntry.value = null
        }
    }

    /**
     * Learned profile auto-fill:
     * When selecting an existing truck or clicking 'auto-fill & update',
     * loads all fields from the previous entry and switches into UPDATE mode so it won't duplicate!
     */
    fun loadExistingTruckProfile(entry: TruckEntry) {
        editingEntryId.value = entry.id
        truckNumber.value = entry.truckNumber
        driverName.value = entry.driverName
        hasHeadAndTail.value = entry.hasHeadAndTail
        headVolumeInput.value = if (entry.headVolume > 0) String.format(Locale.US, "%.2f", entry.headVolume) else ""
        tailVolumeInput.value = if (entry.tailVolume > 0) String.format(Locale.US, "%.2f", entry.tailVolume) else ""
        hasExtensions.value = entry.hasExtensions
        extensionDetails.value = entry.extensionDetails
        if (entry.materialType.isNotBlank()) materialType.value = entry.materialType
        if (entry.siteLocation.isNotBlank()) siteLocation.value = entry.siteLocation

        existingDuplicateEntry.value = null
        showDuplicateConfirmDialog.value = false

        viewModelScope.launch {
            _uiEvents.emit(UiMessage("تم استرجاع بيانات السيارة المسجلة للتحديث بدون تكرار"))
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: FilterType) {
        _selectedFilter.value = filter
    }

    fun onSortChange(sort: SortOption) {
        _selectedSort.value = sort
    }

    fun onTabSelect(tabIndex: Int) {
        selectedTab.value = tabIndex
    }

    fun onHasHeadAndTailChange(hasBoth: Boolean) {
        hasHeadAndTail.value = hasBoth
        if (!hasBoth) {
            tailVolumeInput.value = ""
            tailVolumeError.value = null
        }
    }

    fun onHasExtensionsChange(hasExt: Boolean) {
        hasExtensions.value = hasExt
        if (!hasExt) {
            extensionDetails.value = ""
        }
    }

    fun openCalcDialog(target: CalcTarget) {
        calcTarget.value = target
        calcLength.value = ""
        calcWidth.value = ""
        calcHeight.value = ""
        showCalcDialog.value = true
    }

    fun closeCalcDialog() {
        showCalcDialog.value = false
    }

    fun applyCalcResult() {
        val vol = calcResultVolume.value
        val formatted = String.format(Locale.US, "%.2f", vol)
        if (calcTarget.value == CalcTarget.HEAD) {
            headVolumeInput.value = formatted
            headVolumeError.value = null
        } else {
            tailVolumeInput.value = formatted
            tailVolumeError.value = null
        }
        showCalcDialog.value = false
    }

    fun editEntry(entry: TruckEntry) {
        editingEntryId.value = entry.id
        truckNumber.value = entry.truckNumber
        driverName.value = entry.driverName
        hasHeadAndTail.value = entry.hasHeadAndTail
        headVolumeInput.value = if (entry.headVolume > 0) String.format(Locale.US, "%.2f", entry.headVolume) else ""
        tailVolumeInput.value = if (entry.tailVolume > 0) String.format(Locale.US, "%.2f", entry.tailVolume) else ""
        hasExtensions.value = entry.hasExtensions
        extensionDetails.value = entry.extensionDetails
        materialType.value = entry.materialType
        siteLocation.value = entry.siteLocation
        notes.value = entry.notes

        truckNumberError.value = null
        driverNameError.value = null
        headVolumeError.value = null
        tailVolumeError.value = null
        existingDuplicateEntry.value = null

        selectedTab.value = 0 // Switch to form tab
    }

    fun duplicateTruckData(entry: TruckEntry) {
        // User wants to re-enter a trip for same truck; set up form
        editingEntryId.value = entry.id // Link to existing truck to update, preventing unneeded duplicates!
        truckNumber.value = entry.truckNumber
        driverName.value = entry.driverName
        hasHeadAndTail.value = entry.hasHeadAndTail
        headVolumeInput.value = if (entry.headVolume > 0) String.format(Locale.US, "%.2f", entry.headVolume) else ""
        tailVolumeInput.value = if (entry.tailVolume > 0) String.format(Locale.US, "%.2f", entry.tailVolume) else ""
        hasExtensions.value = entry.hasExtensions
        extensionDetails.value = entry.extensionDetails
        materialType.value = entry.materialType
        siteLocation.value = entry.siteLocation
        notes.value = ""

        truckNumberError.value = null
        driverNameError.value = null
        headVolumeError.value = null
        tailVolumeError.value = null
        existingDuplicateEntry.value = null

        selectedTab.value = 0
        viewModelScope.launch {
            _uiEvents.emit(UiMessage("تم تحميل بيانات السيارة للتحديث أو تسجيل حمولة جديدة"))
        }
    }

    fun resetForm() {
        editingEntryId.value = null
        truckNumber.value = ""
        driverName.value = ""
        hasHeadAndTail.value = true
        headVolumeInput.value = ""
        tailVolumeInput.value = ""
        hasExtensions.value = false
        extensionDetails.value = ""
        materialType.value = ""
        siteLocation.value = ""
        notes.value = ""

        truckNumberError.value = null
        driverNameError.value = null
        headVolumeError.value = null
        tailVolumeError.value = null
        existingDuplicateEntry.value = null
        showDuplicateConfirmDialog.value = false
    }

    /**
     * Save entry with smart duplicate prevention:
     * Checks if truck number already exists. If it exists and user is not updating it,
     * triggers the duplicate resolution dialog.
     */
    fun saveEntry(forceUpsert: Boolean = false) {
        var isValid = true

        val tNumber = truckNumber.value.trim()
        if (tNumber.isBlank()) {
            truckNumberError.value = "يرجى كتابة رقم السيارة أو اللوحة"
            isValid = false
        } else {
            truckNumberError.value = null
        }

        val dName = driverName.value.trim()
        if (dName.isBlank()) {
            driverNameError.value = "يرجى كتابة اسم السائق"
            isValid = false
        } else {
            driverNameError.value = null
        }

        val headVol = headVolumeInput.value.trim().toDoubleOrNull()
        if (headVol == null || headVol <= 0.0) {
            headVolumeError.value = "يرجى إدخال كمية رأس صحيحة أكبر من صفر"
            isValid = false
        } else {
            headVolumeError.value = null
        }

        val both = hasHeadAndTail.value
        val tailVol: Double
        if (both) {
            val parsedTail = tailVolumeInput.value.trim().toDoubleOrNull()
            if (parsedTail == null || parsedTail <= 0.0) {
                tailVolumeError.value = "يرجى إدخال كمية ذنب صحيحة أكبر من صفر"
                isValid = false
                tailVol = 0.0
            } else {
                tailVolumeError.value = null
                tailVol = parsedTail
            }
        } else {
            tailVolumeError.value = null
            tailVol = 0.0
        }

        if (!isValid) return

        val totalVol = headVol!! + tailVol

        viewModelScope.launch {
            // Check for duplicate truck number if creating a new entry
            val currentEditId = editingEntryId.value
            if (currentEditId == null && !forceUpsert) {
                val existing = repository.getEntryByTruckNumber(tNumber)
                if (existing != null) {
                    existingDuplicateEntry.value = existing
                    showDuplicateConfirmDialog.value = true
                    return@launch
                }
            }

            // Either update existing or insert new
            val targetId = currentEditId ?: existingDuplicateEntry.value?.id

            if (targetId != null) {
                val updatedEntry = TruckEntry(
                    id = targetId,
                    truckNumber = tNumber,
                    driverName = dName,
                    hasHeadAndTail = both,
                    headVolume = headVol,
                    tailVolume = tailVol,
                    totalVolume = totalVol,
                    hasExtensions = hasExtensions.value,
                    extensionDetails = extensionDetails.value.trim(),
                    materialType = materialType.value.trim(),
                    siteLocation = siteLocation.value.trim(),
                    notes = notes.value.trim()
                )
                repository.update(updatedEntry)
                _uiEvents.emit(UiMessage("تم تحديث بيانات السيارة المسجلة بنجاح"))
            } else {
                val newEntry = TruckEntry(
                    truckNumber = tNumber,
                    driverName = dName,
                    hasHeadAndTail = both,
                    headVolume = headVol,
                    tailVolume = tailVol,
                    totalVolume = totalVol,
                    hasExtensions = hasExtensions.value,
                    extensionDetails = extensionDetails.value.trim(),
                    materialType = materialType.value.trim(),
                    siteLocation = siteLocation.value.trim(),
                    notes = notes.value.trim()
                )
                repository.insert(newEntry)
                _uiEvents.emit(UiMessage("تم حفظ بيانات السيارة والتكعيب بنجاح"))
            }

            resetForm()
            selectedTab.value = 1 // Switch to records list
        }
    }

    fun confirmDelete(entry: TruckEntry) {
        entryPendingDelete.value = entry
    }

    fun dismissDeleteDialog() {
        entryPendingDelete.value = null
    }

    fun executeDelete() {
        val entry = entryPendingDelete.value ?: return
        viewModelScope.launch {
            repository.delete(entry)
            entryPendingDelete.value = null
            _uiEvents.emit(UiMessage("تم حذف بيانات السيارة بنجاح"))
        }
    }

    // --- Export & Backup Functions ---

    fun exportToCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            isExporting.value = true
            try {
                val list = repository.getAllEntriesSnapshot()
                if (list.isEmpty()) {
                    _uiEvents.emit(UiMessage("لا توجد سيارات مسجلة لتصديرها", isError = true))
                    return@launch
                }
                val uri = ExportManager.exportToCsv(context, list)
                withContext(Dispatchers.Main) {
                    ExportManager.shareFile(
                        context = context,
                        fileUri = uri,
                        mimeType = "text/csv",
                        title = "مشاركة تقرير التكعيب (CSV / Excel)"
                    )
                }
            } catch (e: Exception) {
                _uiEvents.emit(UiMessage("حدث خطأ أثناء تصدير CSV: ${e.localizedMessage}", isError = true))
            } finally {
                isExporting.value = false
            }
        }
    }

    fun exportToPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            isExporting.value = true
            try {
                val list = repository.getAllEntriesSnapshot()
                if (list.isEmpty()) {
                    _uiEvents.emit(UiMessage("لا توجد سيارات مسجلة لتصديرها", isError = true))
                    return@launch
                }
                val uri = ExportManager.exportToPdf(context, list)
                withContext(Dispatchers.Main) {
                    ExportManager.shareFile(
                        context = context,
                        fileUri = uri,
                        mimeType = "application/pdf",
                        title = "مشاركة تقرير التكعيب (PDF)"
                    )
                }
            } catch (e: Exception) {
                _uiEvents.emit(UiMessage("حدث خطأ أثناء تصدير PDF: ${e.localizedMessage}", isError = true))
            } finally {
                isExporting.value = false
            }
        }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            isExporting.value = true
            try {
                val list = repository.getAllEntriesSnapshot()
                if (list.isEmpty()) {
                    _uiEvents.emit(UiMessage("لا توجد سيارات مسجلة لإنشاء نسخة احتياطية", isError = true))
                    return@launch
                }
                val uri = ExportManager.createBackupJson(context, list)
                withContext(Dispatchers.Main) {
                    ExportManager.shareFile(
                        context = context,
                        fileUri = uri,
                        mimeType = "application/json",
                        title = "مشاركة النسخة الاحتياطية (عبر واتساب أو غيره)"
                    )
                }
            } catch (e: Exception) {
                _uiEvents.emit(UiMessage("حدث خطأ أثناء إنشاء النسخة الاحتياطية: ${e.localizedMessage}", isError = true))
            } finally {
                isExporting.value = false
            }
        }
    }

    /**
     * Restores backup from chosen file URI:
     * MERGES and UPDATES existing records by truckNumber, ADDS new records.
     * Does NOT delete existing data!
     */
    fun restoreBackupFromUri(context: Context, fileUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isExporting.value = true
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                if (inputStream == null) {
                    _uiEvents.emit(UiMessage("تعذر فتح ملف النسخة الاحتياطية", isError = true))
                    return@launch
                }

                val jsonContent = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsedEntries = ExportManager.parseBackupJson(jsonContent)

                if (parsedEntries.isEmpty()) {
                    _uiEvents.emit(UiMessage("الملف لا يحتوي على بيانات شاحنات صالحة", isError = true))
                    return@launch
                }

                val result = repository.upsertBackupEntries(parsedEntries)
                _uiEvents.emit(
                    UiMessage("تمت الاستعادة بنجاح: تم تحديث ${result.updatedCount} سيارة، وإضافة ${result.addedCount} سيارة جديدة بدون حذف أية بيانات سابقة!")
                )
                selectedTab.value = 1 // Switch to list
            } catch (e: Exception) {
                _uiEvents.emit(UiMessage("فشلت استعادة النسخة الاحتياطية: ${e.localizedMessage}", isError = true))
            } finally {
                isExporting.value = false
            }
        }
    }
}

class TruckViewModelFactory(private val repository: TruckRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TruckViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TruckViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
