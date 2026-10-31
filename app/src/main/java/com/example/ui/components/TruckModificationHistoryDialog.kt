package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.local.entity.TruckEntry
import com.example.data.local.entity.TruckModification
import com.example.ui.TruckViewModel
import com.example.ui.theme.AmberContainerLight
import com.example.ui.theme.CyanContainerLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnAmberContainerLight
import com.example.ui.theme.OnCyanContainerLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.util.AppFolderManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TruckModificationHistoryDialog(
    truck: TruckEntry,
    viewModel: TruckViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val modifications by viewModel.modificationsForSelectedTruck.collectAsState()
    val showAddForm by viewModel.showAddModificationForm.collectAsState()

    val newModType by viewModel.newModType.collectAsState()
    val newModHeadVolume by viewModel.newModHeadVolume.collectAsState()
    val newModTailVolume by viewModel.newModTailVolume.collectAsState()
    val newModHasExtensions by viewModel.newModHasExtensions.collectAsState()
    val newModExtensionDetails by viewModel.newModExtensionDetails.collectAsState()
    val newModNotes by viewModel.newModNotes.collectAsState()
    val newModPhotos by viewModel.newModPhotos.collectAsState()
    val newModHeadVolumeError by viewModel.newModHeadVolumeError.collectAsState()
    val newModTailVolumeError by viewModel.newModTailVolumeError.collectAsState()
    val newModCalculatedTotal by viewModel.newModCalculatedTotal.collectAsState()

    var photoToPreview by remember { mutableStateOf<String?>(null) }
    var modPendingDelete by remember { mutableStateOf<TruckModification?>(null) }

    // Camera photo capture handling
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            pendingPhotoFile?.let { file ->
                viewModel.addNewModPhoto(file.absolutePath)
                AppFolderManager.copyPhotoToPublic(context, file)
                Toast.makeText(context, "تم التقاط صورة التعديل وحفظها", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val modGalleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            var addedCount = 0
            uris.forEach { uri ->
                copyUriToInternalPhotos(context, uri)?.let { file ->
                    viewModel.addNewModPhoto(file.absolutePath)
                    AppFolderManager.copyPhotoToPublic(context, file)
                    addedCount++
                }
            }
            if (addedCount > 0) {
                Toast.makeText(context, "تمت إضافة $addedCount صورة للتعديل من المعرض وحفظها", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val (file, uri) = createCameraFileAndUri(context)
            if (file != null && uri != null) {
                pendingPhotoFile = file
                pendingPhotoUri = uri
                takePictureLauncher.launch(uri)
            }
        } else {
            Toast.makeText(context, "يلزم منح إذن الكاميرا لالتقاط صورة التعديل", Toast.LENGTH_LONG).show()
        }
    }

    val launchCameraCapture = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val (file, uri) = createCameraFileAndUri(context)
            if (file != null && uri != null) {
                pendingPhotoFile = file
                pendingPhotoUri = uri
                takePictureLauncher.launch(uri)
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val dateFormatter = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
    val initialDateStr = dateFormatter.format(Date(truck.createdAt))

    // Calculate earliest baseline if modifications exist
    val originalBaselineTotal = if (modifications.isNotEmpty()) {
        val oldest = modifications.minByOrNull { it.modificationDate }
        oldest?.previousTotalVolume ?: truck.totalVolume
    } else {
        truck.totalVolume
    }
    val overallDifference = truck.totalVolume - originalBaselineTotal

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("truck_modifications_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "سجل تعديلات السيارة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${modifications.size} تعديل",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "رقم السيارة: ${truck.truckNumber} | السائق: ${truck.driverName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_modifications_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Summary Card: Original State vs Current Active State
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "مقارنة حالة الشاحنة (الأصلية مقابل الحالية)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Original State Column
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = CardDefaults.outlinedCardBorder()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "الحالة الأصلية",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = String.format(Locale.US, "%.2f م³", originalBaselineTotal),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (modifications.isNotEmpty()) {
                                                    val oldest = modifications.minByOrNull { it.modificationDate }
                                                    if (oldest?.previousHasExtensions == true) "مع وصلات" else "بدون وصلات"
                                                } else {
                                                    if (truck.hasExtensions) "مع وصلات" else "بدون وصلات"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "أول تسجيل: $initialDateStr",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    // Current Active State Column
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "التكعيب الحالي النشط",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = String.format(Locale.US, "%.2f م³", truck.totalVolume),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (truck.hasExtensions) {
                                                    if (truck.extensionDetails.isNotBlank()) "مع وصلات (${truck.extensionDetails})" else "مع وصلات"
                                                } else "بدون وصلات",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (truck.hasExtensions) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))

                                            // Overall Diff Badge
                                            val diffText = if (overallDifference > 0.001) {
                                                "+${String.format(Locale.US, "%.2f", overallDifference)} م³ زيادة"
                                            } else if (overallDifference < -0.001) {
                                                "${String.format(Locale.US, "%.2f", overallDifference)} م³ نقصان"
                                            } else {
                                                "بدون تغير إجمالي"
                                            }
                                            Text(
                                                text = diffText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (overallDifference > 0) SuccessGreen else if (overallDifference < 0) ErrorRed else MaterialTheme.colorScheme.outline,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action Button: Add New Modification
                    item {
                        if (!showAddForm) {
                            Button(
                                onClick = { viewModel.openAddModificationForm() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_add_modification_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تسجيل تعديل جديد (تركيب وصلة، تكبير، تصغير الصندوق)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Form: Add New Modification (Animated)
                    item {
                        AnimatedVisibility(
                            visible = showAddForm,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "تسجيل وتوثيق تعديل جديد للشاحنة",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(onClick = { viewModel.closeAddModificationForm() }) {
                                            Icon(Icons.Default.Close, contentDescription = "إلغاء")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Quick Modification Type Selection Chips
                                    Text(
                                        text = "نوع التعديل المجرى على الشاحنة:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val modTypes = listOf(
                                            "إضافة وصلة",
                                            "تكبير الصندوق",
                                            "تصغير الصندوق",
                                            "تعديل الارتفاع",
                                            "إزالة الوصلات",
                                            "تعديل القياسات"
                                        )
                                        modTypes.forEach { type ->
                                            FilterChip(
                                                selected = newModType == type,
                                                onClick = { viewModel.onNewModTypeSelect(type) },
                                                label = { Text(type, fontSize = 12.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Volumes Inputs
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newModHeadVolume,
                                            onValueChange = {
                                                viewModel.newModHeadVolume.value = it
                                                viewModel.newModHeadVolumeError.value = null
                                            },
                                            label = { Text("كمية الرأس الجديدة (م³)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            isError = newModHeadVolumeError != null,
                                            supportingText = newModHeadVolumeError?.let { { Text(it, color = ErrorRed) } },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("new_mod_head_volume_input"),
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true
                                        )

                                        if (truck.hasHeadAndTail) {
                                            OutlinedTextField(
                                                value = newModTailVolume,
                                                onValueChange = {
                                                    viewModel.newModTailVolume.value = it
                                                    viewModel.newModTailVolumeError.value = null
                                                },
                                                label = { Text("كمية الذنب الجديدة (م³)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                isError = newModTailVolumeError != null,
                                                supportingText = newModTailVolumeError?.let { { Text(it, color = ErrorRed) } },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("new_mod_tail_volume_input"),
                                                shape = RoundedCornerShape(10.dp),
                                                singleLine = true
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Realtime preview of the modification diff
                                    val newTotal = newModCalculatedTotal
                                    val diff = newTotal - truck.totalVolume
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "الإجمالي الجديد: ${String.format(Locale.US, "%.2f", newTotal)} م³",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val sign = if (diff >= 0) "+${String.format(Locale.US, "%.2f", diff)}" else String.format(Locale.US, "%.2f", diff)
                                            Text(
                                                text = "فرق التكعيب: $sign م³",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (diff > 0) SuccessGreen else if (diff < 0) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Extension toggle and details
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Extension,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "الشاحنة تحتوي على وصلات الآن",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Switch(
                                            checked = newModHasExtensions,
                                            onCheckedChange = { viewModel.newModHasExtensions.value = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    }

                                    if (newModHasExtensions) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = newModExtensionDetails,
                                            onValueChange = { viewModel.newModExtensionDetails.value = it },
                                            label = { Text("تفاصيل الوصلة الجديدة (مثال: وصلة حديد 30 سم)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Notes on modification
                                    OutlinedTextField(
                                        value = newModNotes,
                                        onValueChange = { viewModel.newModNotes.value = it },
                                        label = { Text("ملاحظات التعديل (اسم الورشة، سبب التعديل...)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        maxLines = 2
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Modification Photos Section (Camera Only, robust responsive layout)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.CameraAlt,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "صور توثيق التعديل",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "التقاط بالكاميرا أو اختيار من المعرض",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    FilledTonalButton(
                                                        onClick = { launchCameraCapture() },
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "كاميرا",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    FilledTonalButton(
                                                        onClick = {
                                                            modGalleryPickerLauncher.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                            )
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "معرض",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            if (newModPhotos.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    newModPhotos.forEach { path ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(68.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                        ) {
                                                            AsyncImage(
                                                                model = File(path),
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .clickable { photoToPreview = path }
                                                            )
                                                            IconButton(
                                                                onClick = { viewModel.removeNewModPhoto(path) },
                                                                modifier = Modifier
                                                                    .size(22.dp)
                                                                    .align(Alignment.TopEnd)
                                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                            ) {
                                                                Icon(Icons.Default.Close, contentDescription = "حذف", tint = Color.White, modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Save Button
                                    Button(
                                        onClick = { viewModel.saveTruckModification() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("save_modification_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SuccessGreen
                                        )
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "حفظ التعديل وتحديث تكعيب السيارة الحالي",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section: History of Past Modifications
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "سجل التعديلات والتحسينات السابقة (${modifications.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (modifications.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "لا توجد تعديلات مسجلة بعد لهذه الشاحنة",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "إذا قامت الشاحنة بتركيب وصلات أو تكبير أو تصغير الصندوق لاحقاً، اضغط على زر 'تسجيل تعديل جديد' أعلاه لتوثيق التعديل وحفظ تاريخ كيف كانت وكيف أصبحت.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(modifications, key = { it.id }) { mod ->
                            ModificationItemCard(
                                modification = mod,
                                onPreviewPhoto = { photoToPreview = it },
                                onDelete = { modPendingDelete = mod }
                            )
                        }
                    }
                }
            }
        }
    }

    // Photo Preview Dialog
    if (photoToPreview != null) {
        AlertDialog(
            onDismissRequest = { photoToPreview = null },
            confirmButton = {
                TextButton(onClick = { photoToPreview = null }) {
                    Text("إغلاق")
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = File(photoToPreview!!),
                        contentDescription = "معاينة صورة التعديل",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        )
    }

    // Delete Modification Confirmation Dialog
    if (modPendingDelete != null) {
        val mod = modPendingDelete!!
        AlertDialog(
            onDismissRequest = { modPendingDelete = null },
            title = { Text("حذف سجل التعديل؟") },
            text = {
                Text("هل أنت متأكد من حذف سجل التعديل (${mod.modificationType}) المسجل بتاريخ ${dateFormatter.format(Date(mod.modificationDate))}؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteModification(mod)
                        modPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("نعم، حذف السجل")
                }
            },
            dismissButton = {
                TextButton(onClick = { modPendingDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModificationItemCard(
    modification: TruckModification,
    onPreviewPhoto: (String) -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
    val dateStr = dateFormatter.format(Date(modification.modificationDate))
    val photos = modification.getPhotoList()
    val diff = modification.volumeDifference

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Type badge & Date & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = modification.modificationType,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "حذف سجل التعديل",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Volume Before & After Visual Comparison Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Before
                    Column {
                        Text(
                            text = "قبل التعديل",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f م³", modification.previousTotalVolume),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (modification.previousHasExtensions) "مع وصلات" else "بدون وصلات",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp
                        )
                    }

                    // Arrow Icon
                    Icon(
                        Icons.Default.TrendingFlat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    // After
                    Column {
                        Text(
                            text = "بعد التعديل",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f م³", modification.newTotalVolume),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (modification.newHasExtensions) {
                                if (modification.newExtensionDetails.isNotBlank()) "وصلات (${modification.newExtensionDetails})" else "مع وصلات"
                            } else "بدون وصلات",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (modification.newHasExtensions) SuccessGreen else MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp
                        )
                    }

                    // Net Difference Badge
                    val diffSign = if (diff >= 0) "+${String.format(Locale.US, "%.2f", diff)}" else String.format(Locale.US, "%.2f", diff)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (diff > 0) SuccessGreenContainer else if (diff < 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (diff > 0) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            } else if (diff < 0) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$diffSign م³",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (diff > 0) SuccessGreen else if (diff < 0) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Head and Tail breakdown if head & tail
            if (modification.previousTailVolume > 0 || modification.newTailVolume > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "الرأس: ${String.format(Locale.US, "%.2f", modification.previousHeadVolume)} ➔ ${String.format(Locale.US, "%.2f", modification.newHeadVolume)} م³",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "الذنب: ${String.format(Locale.US, "%.2f", modification.previousTailVolume)} ➔ ${String.format(Locale.US, "%.2f", modification.newTailVolume)} م³",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Notes if any
            if (modification.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ملاحظات: ${modification.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Photo thumbnails if any
            if (photos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    photos.forEach { path ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { onPreviewPhoto(path) }
                        ) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = "صورة التعديل",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates a unique file and content URI for camera capture
 */
private fun createCameraFileAndUri(context: Context): Pair<File?, Uri?> {
    return try {
        val photosDir = File(context.filesDir, "truck_photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val photoFile = File(photosDir, "MOD_${timestamp}_${System.currentTimeMillis() % 1000}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        Pair(photoFile, uri)
    } catch (e: Exception) {
        Pair(null, null)
    }
}
