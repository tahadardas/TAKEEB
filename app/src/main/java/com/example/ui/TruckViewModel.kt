package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TruckEntry
import com.example.data.local.entity.TruckModification
import com.example.data.repository.TruckRepository
import com.example.util.ContactPickerHelper
import com.example.util.ExportManager
import com.example.util.SmsBroadcastHelper
import com.example.util.VpsSyncManager
import com.example.util.VpsSyncPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

data class DriverRecipient(
    val phone: String,
    val driverName: String,
    val truckNumber: String,
    val isCustomContact: Boolean = false
)

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

    // --- Theme State (null = System, true = Dark, false = Light) ---
    private val _isDarkTheme = MutableStateFlow<Boolean?>(null)
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    fun setDarkMode(dark: Boolean?) {
        _isDarkTheme.value = dark
    }

    fun toggleTheme(currentIsDark: Boolean) {
        _isDarkTheme.value = !currentIsDark
    }

    private var appContext: Context? = null

    // --- UI Event messages ---
    private val _uiEvents = MutableSharedFlow<UiMessage>()
    val uiEvents: SharedFlow<UiMessage> = _uiEvents.asSharedFlow()

    /**
     * Called on app startup:
     * 1. If internal database is empty (e.g. fresh install / APK update / data wipe),
     *    automatically detects and restores candidate backup from the dedicated public folder
     *    تكعيب الغضب/ملفات_البرنامج/قاعدة_بيانات_السيارات_المعتمدة.sqlite
     * 2. If internal database already contains records, updates the master backup snapshot in the public folder.
     * 3. Loads VPS settings and triggers automatic background sync if enabled.
     */
    fun autoSyncOrRestoreOnStart(context: Context) {
        appContext = context.applicationContext
        loadVpsSettings(context)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entriesSnapshot = repository.getAllEntriesSnapshot()
                if (entriesSnapshot.isEmpty()) {
                    val candidateBackup = ExportManager.findMasterOrLatestBackup(context)
                    if (candidateBackup != null && candidateBackup.exists() && candidateBackup.length() > 0L) {
                        val uri = Uri.fromFile(candidateBackup)
                        val result = ExportManager.restoreBackup(context, uri, repository)
                        if (result.success && result.totalCount > 0) {
                            _uiEvents.emit(
                                UiMessage("تم استرجاع وتحديث بيانات السيارات تلقائياً (${result.totalCount} سيارة) وحمايتها من الفقدان")
                            )
                        }
                    }
                } else {
                    ExportManager.saveMasterAutoBackup(context)
                }

                // Automatic background VPS sync if enabled
                if (VpsSyncPreferences.isAutoSyncEnabled(context) && VpsSyncPreferences.getServerUrl(context).isNotBlank()) {
                    performVpsSync(context)
                }
            } catch (e: Exception) {
                android.util.Log.w("TruckViewModel", "Auto-sync error: ${e.message}")
            }
        }
    }

    // --- VPS Server Synchronization State ---
    val showVpsSyncDialog = MutableStateFlow(false)
    val vpsServerUrl = MutableStateFlow("")
    val vpsApiKey = MutableStateFlow("")
    val vpsAutoSyncEnabled = MutableStateFlow(false)
    val isVpsSyncing = MutableStateFlow(false)
    val vpsSyncStatusMessage = MutableStateFlow<String?>(null)
    val vpsLastSyncTimeFormatted = MutableStateFlow("لم تتم المزامنة بعد")
    val vpsIsConnected = MutableStateFlow<Boolean?>(null)

    fun loadVpsSettings(context: Context) {
        vpsServerUrl.value = VpsSyncPreferences.getServerUrl(context)
        vpsApiKey.value = VpsSyncPreferences.getApiKey(context)
        vpsAutoSyncEnabled.value = VpsSyncPreferences.isAutoSyncEnabled(context)
        vpsSyncStatusMessage.value = VpsSyncPreferences.getLastSyncStatus(context)
        val lastTime = VpsSyncPreferences.getLastSyncTime(context)
        vpsLastSyncTimeFormatted.value = if (lastTime > 0) {
            java.text.SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(java.util.Date(lastTime))
        } else {
            "لم تتم المزامنة بعد"
        }
    }

    fun saveVpsSettings(context: Context, url: String, key: String, autoSync: Boolean) {
        VpsSyncPreferences.setServerUrl(context, url)
        VpsSyncPreferences.setApiKey(context, key)
        VpsSyncPreferences.setAutoSyncEnabled(context, autoSync)
        vpsServerUrl.value = url.trim().removeSuffix("/")
        vpsApiKey.value = key.trim()
        vpsAutoSyncEnabled.value = autoSync
        viewModelScope.launch {
            _uiEvents.emit(UiMessage("تم حفظ إعدادات سيرفر VPS بنجاح"))
        }
    }

    fun testVpsConnection(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            isVpsSyncing.value = true
            val (ok, message) = VpsSyncManager.testConnection(vpsServerUrl.value, vpsApiKey.value)
            vpsIsConnected.value = ok
            vpsSyncStatusMessage.value = message
            isVpsSyncing.value = false
            _uiEvents.emit(UiMessage(message, isError = !ok))
        }
    }

    fun performVpsSync(context: Context) {
        val url = vpsServerUrl.value.ifBlank { VpsSyncPreferences.getServerUrl(context) }
        val key = vpsApiKey.value.ifBlank { VpsSyncPreferences.getApiKey(context) }
        if (url.isBlank()) {
            viewModelScope.launch {
                _uiEvents.emit(UiMessage("يرجى إدخال رابط سيرفر VPS أولاً في الإعدادات", isError = true))
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            isVpsSyncing.value = true
            vpsSyncStatusMessage.value = "جاري الاتصال وتبادل البيانات والصور مع السيرفر..."
            try {
                val lastTime = VpsSyncPreferences.getLastSyncTime(context)
                val summary = VpsSyncManager.syncWithServer(
                    context = context,
                    repository = repository,
                    serverUrl = url,
                    apiKey = key,
                    lastSyncTime = lastTime
                )

                if (summary.success) {
                    VpsSyncPreferences.setLastSyncTime(context, summary.serverTime)
                    val timeStr = java.text.SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(java.util.Date(summary.serverTime))
                    vpsLastSyncTimeFormatted.value = timeStr
                    vpsIsConnected.value = true

                    val msg = buildString {
                        append("اكتملت المزامنة مع السيرفر بنجاح! ")
                        if (summary.pulledTrucks > 0) append("استُلمت ${summary.pulledTrucks} سيارة جديدة، ")
                        if (summary.downloadedPhotos > 0) append("حُمّلت ${summary.downloadedPhotos} صورة جديدة، ")
                        if (summary.uploadedPhotos > 0) append("رُفعت ${summary.uploadedPhotos} صورة، ")
                        if (summary.pushedTrucks > 0) append("رُفعت ${summary.pushedTrucks} سيارة للسيرفر.")
                        if (summary.pulledTrucks == 0 && summary.pushedTrucks == 0) append("جميع السجلات والصور متطابقة ومحدثة مع السيرفر.")
                    }
                    vpsSyncStatusMessage.value = msg
                    VpsSyncPreferences.setLastSyncStatus(context, msg, isSuccess = true)
                    _uiEvents.emit(UiMessage(msg))
                } else {
                    vpsIsConnected.value = false
                    val err = summary.errorMessage ?: "فشلت المزامنة مع السيرفر"
                    vpsSyncStatusMessage.value = err
                    VpsSyncPreferences.setLastSyncStatus(context, err, isSuccess = false)
                    _uiEvents.emit(UiMessage(err, isError = true))
                }
            } catch (e: Exception) {
                vpsIsConnected.value = false
                val err = "خطأ في المزامنة: ${e.localizedMessage}"
                vpsSyncStatusMessage.value = err
                _uiEvents.emit(UiMessage(err, isError = true))
            } finally {
                isVpsSyncing.value = false
            }
        }
    }

    // --- Form State ---
    var editingEntryId = MutableStateFlow<Long?>(null)
        private set

    val truckNumber = MutableStateFlow("")
    val driverName = MutableStateFlow("")
    val driverPhone = MutableStateFlow("")
    val hasHeadAndTail = MutableStateFlow(true)
    val headVolumeInput = MutableStateFlow("")
    val tailVolumeInput = MutableStateFlow("")
    val hasExtensions = MutableStateFlow(false)
    val extensionDetails = MutableStateFlow("")
    val materialType = MutableStateFlow("")
    val siteLocation = MutableStateFlow("")
    val notes = MutableStateFlow("")

    // Captured Camera Photos (exclusive to device camera)
    val capturedPhotos = MutableStateFlow<List<String>>(emptyList())
    val viewingPhotoPath = MutableStateFlow<String?>(null)

    val truckNumberError = MutableStateFlow<String?>(null)
    val driverNameError = MutableStateFlow<String?>(null)
    val headVolumeError = MutableStateFlow<String?>(null)
    val tailVolumeError = MutableStateFlow<String?>(null)

    // Detected duplicate phone number warning
    val existingPhoneDuplicate = MutableStateFlow<TruckEntry?>(null)

    // --- SMS Broadcast & Material Prices State ---
    val smsMessageText = MutableStateFlow("")
    val customContacts = MutableStateFlow<List<DriverRecipient>>(emptyList())
    val selectedRecipients = MutableStateFlow<Set<String>>(emptySet())
    val isSendingSms = MutableStateFlow(false)

    // Material Pricing inputs
    val priceSand = MutableStateFlow("")
    val priceGravel1 = MutableStateFlow("")
    val priceGravel2 = MutableStateFlow("")
    val priceDust = MutableStateFlow("")
    val priceBase = MutableStateFlow("")
    val priceNotes = MutableStateFlow("")

    // Derived distinct driver recipients from database entries combined with custom added contacts
    val smsRecipients: StateFlow<List<DriverRecipient>> = combine(
        repository.allEntries,
        customContacts
    ) { entries, custom ->
        val fromEntries = entries
            .filter { it.driverPhone.isNotBlank() }
            .groupBy { ContactPickerHelper.cleanPhoneNumber(it.driverPhone) }
            .map { (cleanPhone, list) ->
                val latest = list.maxByOrNull { it.createdAt } ?: list.first()
                DriverRecipient(
                    phone = cleanPhone,
                    driverName = latest.driverName,
                    truckNumber = latest.truckNumber,
                    isCustomContact = false
                )
            }
        val entryPhones = fromEntries.map { it.phone }.toSet()
        val nonDuplicatedCustom = custom.filter { it.phone !in entryPhones }
        (fromEntries + nonDuplicatedCustom).sortedBy { it.driverName }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // --- Truck Modifications State ---
    val modificationCounts: StateFlow<Map<String, Int>> = repository.getModificationCounts()
        .combine(MutableStateFlow(Unit)) { counts, _ ->
            counts.associate { it.truckNumber.trim().lowercase(Locale.ROOT) to it.count }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val selectedTruckForModifications = MutableStateFlow<TruckEntry?>(null)

    val modificationsForSelectedTruck: StateFlow<List<TruckModification>> = selectedTruckForModifications
        .flatMapLatest { truck ->
            if (truck != null) {
                repository.getModificationsForTruck(truck.truckNumber)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Form state for logging a new modification
    val showAddModificationForm = MutableStateFlow(false)
    val newModType = MutableStateFlow("إضافة وصلة")
    val newModHeadVolume = MutableStateFlow("")
    val newModTailVolume = MutableStateFlow("")
    val newModHasExtensions = MutableStateFlow(true)
    val newModExtensionDetails = MutableStateFlow("")
    val newModNotes = MutableStateFlow("")
    val newModPhotos = MutableStateFlow<List<String>>(emptyList())
    val newModHeadVolumeError = MutableStateFlow<String?>(null)
    val newModTailVolumeError = MutableStateFlow<String?>(null)

    // Dynamic calculated volume for the new modification
    val newModCalculatedTotal: StateFlow<Double> = combine(
        selectedTruckForModifications,
        newModHeadVolume,
        newModTailVolume
    ) { truck, headStr, tailStr ->
        val hasBoth = truck?.hasHeadAndTail ?: false
        val head = headStr.toDoubleOrNull() ?: 0.0
        val tail = if (hasBoth) (tailStr.toDoubleOrNull() ?: 0.0) else 0.0
        head + tail
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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

    // Real-time check for duplicate driver phone number as user types
    fun onDriverPhoneChange(phone: String) {
        driverPhone.value = phone
        if (phone.isNotBlank()) {
            viewModelScope.launch {
                val found = repository.getEntryByPhone(phone)
                if (found != null && (editingEntryId.value == null || editingEntryId.value != found.id)) {
                    existingPhoneDuplicate.value = found
                } else {
                    existingPhoneDuplicate.value = null
                }
            }
        } else {
            existingPhoneDuplicate.value = null
        }
    }

    // Camera photo management (exclusive to camera)
    fun addCapturedPhoto(filePath: String) {
        if (filePath.isNotBlank() && !capturedPhotos.value.contains(filePath)) {
            capturedPhotos.value = capturedPhotos.value + filePath
        }
    }

    fun removeCapturedPhoto(filePath: String) {
        capturedPhotos.value = capturedPhotos.value.filter { it != filePath }
    }

    fun openPhotoViewer(path: String) {
        viewingPhotoPath.value = path
    }

    fun closePhotoViewer() {
        viewingPhotoPath.value = null
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
        driverPhone.value = entry.driverPhone
        hasHeadAndTail.value = entry.hasHeadAndTail
        headVolumeInput.value = if (entry.headVolume > 0) String.format(Locale.US, "%.2f", entry.headVolume) else ""
        tailVolumeInput.value = if (entry.tailVolume > 0) String.format(Locale.US, "%.2f", entry.tailVolume) else ""
        hasExtensions.value = entry.hasExtensions
        extensionDetails.value = entry.extensionDetails
        if (entry.materialType.isNotBlank()) materialType.value = entry.materialType
        if (entry.siteLocation.isNotBlank()) siteLocation.value = entry.siteLocation
        capturedPhotos.value = entry.getPhotoList()

        existingDuplicateEntry.value = null
        existingPhoneDuplicate.value = null
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
        driverPhone.value = entry.driverPhone
        hasHeadAndTail.value = entry.hasHeadAndTail
        headVolumeInput.value = if (entry.headVolume > 0) String.format(Locale.US, "%.2f", entry.headVolume) else ""
        tailVolumeInput.value = if (entry.tailVolume > 0) String.format(Locale.US, "%.2f", entry.tailVolume) else ""
        hasExtensions.value = entry.hasExtensions
        extensionDetails.value = entry.extensionDetails
        materialType.value = entry.materialType
        siteLocation.value = entry.siteLocation
        notes.value = entry.notes
        capturedPhotos.value = entry.getPhotoList()

        truckNumberError.value = null
        driverNameError.value = null
        headVolumeError.value = null
        tailVolumeError.value = null
        existingDuplicateEntry.value = null
        existingPhoneDuplicate.value = null

        selectedTab.value = 0 // Switch to form tab
    }

    fun duplicateTruckData(entry: TruckEntry) {
        // User wants to re-enter a trip for same truck; set up form
        editingEntryId.value = entry.id // Link to existing truck to update, preventing unneeded duplicates!
        truckNumber.value = entry.truckNumber
        driverName.value = entry.driverName
        driverPhone.value = entry.driverPhone
        hasHeadAndTail.value = entry.hasHeadAndTail
        headVolumeInput.value = if (entry.headVolume > 0) String.format(Locale.US, "%.2f", entry.headVolume) else ""
        tailVolumeInput.value = if (entry.tailVolume > 0) String.format(Locale.US, "%.2f", entry.tailVolume) else ""
        hasExtensions.value = entry.hasExtensions
        extensionDetails.value = entry.extensionDetails
        materialType.value = entry.materialType
        siteLocation.value = entry.siteLocation
        notes.value = ""
        capturedPhotos.value = entry.getPhotoList()

        truckNumberError.value = null
        driverNameError.value = null
        headVolumeError.value = null
        tailVolumeError.value = null
        existingDuplicateEntry.value = null
        existingPhoneDuplicate.value = null

        selectedTab.value = 0
        viewModelScope.launch {
            _uiEvents.emit(UiMessage("تم تحميل بيانات السيارة للتحديث أو تسجيل حمولة جديدة"))
        }
    }

    fun resetForm() {
        editingEntryId.value = null
        truckNumber.value = ""
        driverName.value = ""
        driverPhone.value = ""
        hasHeadAndTail.value = true
        headVolumeInput.value = ""
        tailVolumeInput.value = ""
        hasExtensions.value = false
        extensionDetails.value = ""
        materialType.value = ""
        siteLocation.value = ""
        notes.value = ""
        capturedPhotos.value = emptyList()

        truckNumberError.value = null
        driverNameError.value = null
        headVolumeError.value = null
        tailVolumeError.value = null
        existingDuplicateEntry.value = null
        existingPhoneDuplicate.value = null
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

            val dPhone = driverPhone.value.trim()
            val photosJoined = capturedPhotos.value.joinToString(",")

            if (targetId != null) {
                val updatedEntry = TruckEntry(
                    id = targetId,
                    truckNumber = tNumber,
                    driverName = dName,
                    driverPhone = dPhone,
                    hasHeadAndTail = both,
                    headVolume = headVol,
                    tailVolume = tailVol,
                    totalVolume = totalVol,
                    hasExtensions = hasExtensions.value,
                    extensionDetails = extensionDetails.value.trim(),
                    materialType = materialType.value.trim(),
                    siteLocation = siteLocation.value.trim(),
                    notes = notes.value.trim(),
                    photoPaths = photosJoined
                )
                repository.update(updatedEntry)
                _uiEvents.emit(UiMessage("تم تحديث بيانات السيارة المسجلة بنجاح"))
            } else {
                val newEntry = TruckEntry(
                    truckNumber = tNumber,
                    driverName = dName,
                    driverPhone = dPhone,
                    hasHeadAndTail = both,
                    headVolume = headVol,
                    tailVolume = tailVol,
                    totalVolume = totalVol,
                    hasExtensions = hasExtensions.value,
                    extensionDetails = extensionDetails.value.trim(),
                    materialType = materialType.value.trim(),
                    siteLocation = siteLocation.value.trim(),
                    notes = notes.value.trim(),
                    photoPaths = photosJoined
                )
                repository.insert(newEntry)
                _uiEvents.emit(UiMessage("تم حفظ بيانات السيارة والتكعيب بنجاح"))
            }

            appContext?.let { ExportManager.saveMasterAutoBackup(it) }
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
            appContext?.let { ExportManager.saveMasterAutoBackup(it) }
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

    fun exportBackup(context: Context, includePhotos: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            isExporting.value = true
            try {
                val list = repository.getAllEntriesSnapshot()
                val mods = repository.getAllModificationsSnapshot()
                if (list.isEmpty()) {
                    _uiEvents.emit(UiMessage("لا توجد سيارات مسجلة لإنشاء نسخة احتياطية", isError = true))
                    return@launch
                }

                if (includePhotos) {
                    val uri = ExportManager.createCompressedBackupZip(context, list, mods)
                    withContext(Dispatchers.Main) {
                        ExportManager.shareFile(
                            context = context,
                            fileUri = uri,
                            mimeType = "application/zip",
                            title = "مشاركة النسخة الاحتياطية المضغوطة شاملة الصور (واتساب/ملفاتي)"
                        )
                    }
                    _uiEvents.emit(UiMessage("تم تجهيز النسخة الاحتياطية وحفظ نسخة في مجلد تكعيب الغضب في ملفاتي"))
                } else {
                    val uri = ExportManager.createBackupJson(context, list, mods)
                    withContext(Dispatchers.Main) {
                        ExportManager.shareFile(
                            context = context,
                            fileUri = uri,
                            mimeType = "application/json",
                            title = "مشاركة نسخة احتياطية سريعة (JSON)"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiEvents.emit(UiMessage("حدث خطأ أثناء إنشاء النسخة الاحتياطية: ${e.localizedMessage}", isError = true))
            } finally {
                isExporting.value = false
            }
        }
    }

    /**
     * Exports the native SQLite database file directly (.sqlite):
     * Runs WAL checkpoint, saves to user's dedicated folder in My Files,
     * and triggers instant share chooser (WhatsApp, Email, etc.).
     */
    fun exportSqliteDatabase(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            isExporting.value = true
            try {
                val list = repository.getAllEntriesSnapshot()
                if (list.isEmpty()) {
                    _uiEvents.emit(UiMessage("لا توجد بيانات مسجلة في قاعدة بيانات SQLite لتصديرها", isError = true))
                    return@launch
                }
                val uri = ExportManager.exportSqliteDatabase(context)
                withContext(Dispatchers.Main) {
                    ExportManager.shareFile(
                        context = context,
                        fileUri = uri,
                        mimeType = "application/x-sqlite3",
                        title = "مشاركة ملف قاعدة بيانات SQLite الأصلي (.sqlite)"
                    )
                }
                _uiEvents.emit(UiMessage("تم تجهيز نسخة قاعدة بيانات SQLite وحفظها في مجلد تكعيب الغضب في ملفاتي"))
            } catch (e: Exception) {
                _uiEvents.emit(UiMessage("حدث خطأ أثناء تصدير قاعدة بيانات SQLite: ${e.localizedMessage}", isError = true))
            } finally {
                isExporting.value = false
            }
        }
    }

    /**
     * Restores backup from chosen file URI:
     * Unpacks photos (if ZIP) into photo directories,
     * MERGES and UPDATES existing records by truckNumber, ADDS new records.
     * Does NOT delete existing data!
     */
    fun restoreBackupFromUri(context: Context, fileUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isExporting.value = true
            try {
                val result = ExportManager.restoreBackup(context, fileUri, repository)
                if (!result.success) {
                    _uiEvents.emit(UiMessage(result.errorMessage ?: "فشلت استعادة النسخة الاحتياطية", isError = true))
                    return@launch
                }

                val photoText = if (result.restoredPhotosCount > 0) " واستعادة ${result.restoredPhotosCount} صورة بنجاح،" else ""
                _uiEvents.emit(
                    UiMessage("تمت الاستعادة بنجاح:$photoText تم تحديث ${result.updatedCount} سيارة، وإضافة ${result.addedCount} سيارة جديدة بدون مسح أي بيانات سابقة!")
                )
                appContext?.let { ExportManager.saveMasterAutoBackup(it) }
                selectedTab.value = 1 // Switch to list
            } catch (e: Exception) {
                _uiEvents.emit(UiMessage("فشلت استعادة النسخة الاحتياطية: ${e.localizedMessage}", isError = true))
            } finally {
                isExporting.value = false
            }
        }
    }

    // --- SMS Broadcasting & Contact Picker Methods ---

    fun selectAllRecipients() {
        selectedRecipients.value = smsRecipients.value.map { it.phone }.toSet()
    }

    fun deselectAllRecipients() {
        selectedRecipients.value = emptySet()
    }

    fun toggleRecipient(phone: String) {
        val current = selectedRecipients.value.toMutableSet()
        if (current.contains(phone)) {
            current.remove(phone)
        } else {
            current.add(phone)
        }
        selectedRecipients.value = current
    }

    fun addCustomContact(name: String, rawPhone: String) {
        val clean = ContactPickerHelper.cleanPhoneNumber(rawPhone)
        if (clean.isBlank()) return
        val existing = customContacts.value.any { it.phone == clean }
        if (!existing) {
            val contact = DriverRecipient(
                phone = clean,
                driverName = if (name.isNotBlank()) name else "سائق من جهات الاتصال",
                truckNumber = "جهة اتصال",
                isCustomContact = true
            )
            customContacts.value = customContacts.value + contact
        }
        selectedRecipients.value = selectedRecipients.value + clean
        viewModelScope.launch {
            _uiEvents.emit(UiMessage("تم إرفاق ($name - $clean) إلى قائمة الإرسال"))
        }
    }

    fun removeCustomContact(phone: String) {
        customContacts.value = customContacts.value.filter { it.phone != phone }
        val updated = selectedRecipients.value.toMutableSet()
        updated.remove(phone)
        selectedRecipients.value = updated
    }

    fun generateMaterialPriceBulletin() {
        val sand = priceSand.value.trim().ifEmpty { "متوفر حسب الطلب" }
        val g1 = priceGravel1.value.trim().ifEmpty { "متوفر حسب الطلب" }
        val g2 = priceGravel2.value.trim().ifEmpty { "متوفر حسب الطلب" }
        val dust = priceDust.value.trim().ifEmpty { "متوفر حسب الطلب" }
        val base = priceBase.value.trim().ifEmpty { "متوفر حسب الطلب" }
        val extra = priceNotes.value.trim()

        val text = buildString {
            appendLine("السلام عليكم ورحمة الله، الإخوة السائقين الكرام:")
            appendLine("📋 أسعار المواد المعتمدة لدى «تكعيب الغضب»:")
            appendLine("• رمل ناعم ممتاز: $sand")
            appendLine("• بحص نمرة 1: $g1")
            appendLine("• بحص نمرة 2: $g2")
            appendLine("• بودرة حجر: $dust")
            appendLine("• تراب / كبس للردميات: $base")
            if (extra.isNotBlank()) {
                appendLine("• شروط وملاحظات: $extra")
            }
            appendLine("* التكعيب دقيق وموثق ميدانياً بالمتر المكعب.")
            appendLine("للحجز والتنسيق المباشر: 0987080330")
        }
        smsMessageText.value = text.trim()
    }

    fun applyQuickTemplate(type: Int) {
        when (type) {
            1 -> {
                smsMessageText.value = """
                    السلام عليكم ورحمة الله،
                    نشرة أسعار المواد اليوم لدى «تكعيب الغضب»:
                    • رمل ناعم ممتاز
                    • بحص بجميع مقاساته (نمرة 1 و 2)
                    • بودرة ودفان للردميات
                    التكعيب دقيق وموثق بالصور والكاميرا.
                    للاستفسار والحجز: 0987080330
                """.trimIndent()
            }
            2 -> {
                smsMessageText.value = """
                    تحية طيبة لجميع الإخوة السائقين الكرام:
                    يرجى العلم بأن التحميل متوفر ومتاح بالموقع حالياً لجميع الشاحنات (رأس أو رأس وذنب).
                    يرجى الالتزام بالقياس وتوثيق التكعيب الميداني.
                    للتنسيق: 0987080330
                """.trimIndent()
            }
            3 -> {
                smsMessageText.value = ""
            }
        }
    }

    fun openSmsApp(context: Context) {
        val available = smsRecipients.value.map { it.phone }
        val recipients = if (selectedRecipients.value.isNotEmpty()) {
            selectedRecipients.value.toList()
        } else {
            available
        }

        if (recipients.isEmpty()) {
            viewModelScope.launch {
                _uiEvents.emit(UiMessage("لا توجد أرقام مسجلة للإرسال، يرجى إضافة رقم أو تحديد مستلم", isError = true))
            }
            return
        }

        val text = smsMessageText.value.trim()
        if (text.isBlank()) {
            viewModelScope.launch {
                _uiEvents.emit(UiMessage("يرجى كتابة نص الرسالة أو اختيار قالب تسعيرة قبل الإرسال", isError = true))
            }
            return
        }

        val success = SmsBroadcastHelper.openSmsAppForRecipients(context, recipients, text)
        if (!success) {
            viewModelScope.launch {
                _uiEvents.emit(UiMessage("تعذر فتح تطبيق الرسائل تلقائياً", isError = true))
            }
        }
    }

    fun sendDirectSms(context: Context) {
        val available = smsRecipients.value.map { it.phone }
        val recipients = if (selectedRecipients.value.isNotEmpty()) {
            selectedRecipients.value.toList()
        } else {
            available
        }

        if (recipients.isEmpty()) {
            viewModelScope.launch {
                _uiEvents.emit(UiMessage("لا توجد أرقام مسجلة للإرسال", isError = true))
            }
            return
        }

        val text = smsMessageText.value.trim()
        if (text.isBlank()) {
            viewModelScope.launch {
                _uiEvents.emit(UiMessage("يرجى كتابة نص الرسالة أولاً", isError = true))
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            isSendingSms.value = true
            try {
                val sent = SmsBroadcastHelper.sendDirectSms(context, recipients, text)
                _uiEvents.emit(
                    UiMessage("تم إرسال رسائل SMS بنجاح إلى $sent سائقاً من أصل ${recipients.size}")
                )
            } catch (e: Exception) {
                _uiEvents.emit(UiMessage("فشل الإرسال المباشر: ${e.localizedMessage}", isError = true))
            } finally {
                isSendingSms.value = false
            }
        }
    }

    fun shareSmsPricing(context: Context) {
        val text = smsMessageText.value.trim()
        if (text.isBlank()) {
            viewModelScope.launch {
                _uiEvents.emit(UiMessage("يرجى كتابة نص الرسالة أو اختيار قالب تسعيرة للمشاركة", isError = true))
            }
            return
        }
        SmsBroadcastHelper.shareViaApps(context, text)
    }

    // ==========================================
    // Truck Modification Actions
    // ==========================================

    fun openModificationsDialog(entry: TruckEntry) {
        selectedTruckForModifications.value = entry
        showAddModificationForm.value = false
        clearAddModificationForm()
    }

    fun closeModificationsDialog() {
        selectedTruckForModifications.value = null
        showAddModificationForm.value = false
        clearAddModificationForm()
    }

    fun openAddModificationForm() {
        val truck = selectedTruckForModifications.value ?: return
        newModType.value = "إضافة وصلة"
        newModHeadVolume.value = String.format(Locale.US, "%.2f", truck.headVolume)
        newModTailVolume.value = if (truck.hasHeadAndTail) String.format(Locale.US, "%.2f", truck.tailVolume) else ""
        newModHasExtensions.value = true
        newModExtensionDetails.value = truck.extensionDetails
        newModNotes.value = ""
        newModPhotos.value = emptyList()
        newModHeadVolumeError.value = null
        newModTailVolumeError.value = null
        showAddModificationForm.value = true
    }

    fun closeAddModificationForm() {
        showAddModificationForm.value = false
        clearAddModificationForm()
    }

    private fun clearAddModificationForm() {
        newModType.value = "إضافة وصلة"
        newModHeadVolume.value = ""
        newModTailVolume.value = ""
        newModHasExtensions.value = true
        newModExtensionDetails.value = ""
        newModNotes.value = ""
        newModPhotos.value = emptyList()
        newModHeadVolumeError.value = null
        newModTailVolumeError.value = null
    }

    fun onNewModTypeSelect(type: String) {
        newModType.value = type
        if (type == "إضافة وصلة") {
            newModHasExtensions.value = true
        } else if (type == "إزالة الوصلات") {
            newModHasExtensions.value = false
            newModExtensionDetails.value = ""
        }
    }

    fun addNewModPhoto(filePath: String) {
        if (filePath.isNotBlank() && !newModPhotos.value.contains(filePath)) {
            newModPhotos.value = newModPhotos.value + filePath
        }
    }

    fun removeNewModPhoto(filePath: String) {
        newModPhotos.value = newModPhotos.value.filter { it != filePath }
    }

    fun saveTruckModification() {
        val truck = selectedTruckForModifications.value ?: return
        val headVol = newModHeadVolume.value.toDoubleOrNull()
        if (headVol == null || headVol <= 0.0) {
            newModHeadVolumeError.value = "يرجى إدخال كمية رأس صالحة أكبر من الصفر"
            return
        }

        val tailVol = if (truck.hasHeadAndTail) {
            val t = newModTailVolume.value.toDoubleOrNull()
            if (t == null || t <= 0.0) {
                newModTailVolumeError.value = "يرجى إدخال كمية ذنب صالحة أكبر من الصفر"
                return
            }
            t
        } else {
            0.0
        }

        val newTotal = headVol + tailVol
        val prevTotal = truck.totalVolume
        val diff = newTotal - prevTotal

        viewModelScope.launch {
            val modPhotoList = newModPhotos.value
            val modPhotosStr = modPhotoList.joinToString(",")

            val modification = TruckModification(
                truckNumber = truck.truckNumber,
                modificationDate = System.currentTimeMillis(),
                modificationType = newModType.value.trim().ifBlank { "تعديل القياسات" },
                previousHeadVolume = truck.headVolume,
                previousTailVolume = truck.tailVolume,
                previousTotalVolume = prevTotal,
                previousHasExtensions = truck.hasExtensions,
                previousExtensionDetails = truck.extensionDetails,
                newHeadVolume = headVol,
                newTailVolume = tailVol,
                newTotalVolume = newTotal,
                newHasExtensions = newModHasExtensions.value,
                newExtensionDetails = newModExtensionDetails.value.trim(),
                volumeDifference = diff,
                notes = newModNotes.value.trim(),
                photoPaths = modPhotosStr
            )

            // Combine existing photos with any new modification photos
            val combinedPhotos = (truck.getPhotoList() + modPhotoList).distinct().joinToString(",")

            val updatedTruck = truck.copy(
                headVolume = headVol,
                tailVolume = tailVol,
                totalVolume = newTotal,
                hasExtensions = newModHasExtensions.value,
                extensionDetails = newModExtensionDetails.value.trim(),
                notes = if (newModNotes.value.isNotBlank()) {
                    if (truck.notes.isNotBlank()) "${truck.notes} | [تعديل: ${newModNotes.value.trim()}]"
                    else "[تعديل: ${newModNotes.value.trim()}]"
                } else truck.notes,
                photoPaths = combinedPhotos
            )

            repository.recordTruckModification(updatedTruck, modification)

            // Update local selection to reflect latest active state
            selectedTruckForModifications.value = updatedTruck
            showAddModificationForm.value = false
            clearAddModificationForm()

            val diffSign = if (diff >= 0) "+${String.format(Locale.US, "%.2f", diff)}" else String.format(Locale.US, "%.2f", diff)
            _uiEvents.emit(
                UiMessage("تم تسجيل التعديل بنجاح! التكعيب الحالي: ${String.format(Locale.US, "%.2f", newTotal)} م³ (الفرق: $diffSign م³)")
            )
            appContext?.let { ExportManager.saveMasterAutoBackup(it) }
        }
    }

    fun deleteModification(modification: TruckModification) {
        viewModelScope.launch {
            repository.deleteModification(modification)
            appContext?.let { ExportManager.saveMasterAutoBackup(it) }
            _uiEvents.emit(UiMessage("تم حذف سجل التعديل"))
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
