package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CalcTarget
import com.example.ui.TruckViewModel
import com.example.ui.theme.AmberContainerLight
import com.example.ui.theme.CyanContainerLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnAmberContainerLight
import com.example.ui.theme.OnCyanContainerLight
import com.example.ui.theme.SuccessGreen
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewEntryScreen(
    viewModel: TruckViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val editingId by viewModel.editingEntryId.collectAsState()
    val truckNumber by viewModel.truckNumber.collectAsState()
    val driverName by viewModel.driverName.collectAsState()
    val hasHeadAndTail by viewModel.hasHeadAndTail.collectAsState()
    val headVolume by viewModel.headVolumeInput.collectAsState()
    val tailVolume by viewModel.tailVolumeInput.collectAsState()
    val hasExtensions by viewModel.hasExtensions.collectAsState()
    val extensionDetails by viewModel.extensionDetails.collectAsState()
    val materialType by viewModel.materialType.collectAsState()
    val siteLocation by viewModel.siteLocation.collectAsState()
    val notes by viewModel.notes.collectAsState()

    val calculatedTotal by viewModel.calculatedTotalVolume.collectAsState()

    val truckNumberError by viewModel.truckNumberError.collectAsState()
    val driverNameError by viewModel.driverNameError.collectAsState()
    val headVolumeError by viewModel.headVolumeError.collectAsState()
    val tailVolumeError by viewModel.tailVolumeError.collectAsState()

    // Smart Auto-complete / Learned suggestions
    val truckSuggestions by viewModel.truckNumberSuggestions.collectAsState()
    val driverSuggestions by viewModel.driverNameSuggestions.collectAsState()
    val knownMaterials by viewModel.distinctMaterials.collectAsState()
    val knownSites by viewModel.distinctSites.collectAsState()
    val duplicateEntry by viewModel.existingDuplicateEntry.collectAsState()
    val showDuplicateDialog by viewModel.showDuplicateConfirmDialog.collectAsState()

    val defaultMaterials = listOf("رمل", "بحص", "تراب/دفان", "حفريات", "أسفلت", "خرسانة", "كبس")
    val combinedMaterials = (defaultMaterials + knownMaterials).distinct()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Header (New or Editing)
            if (editingId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AmberContainerLight
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = OnAmberContainerLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تعديل وتحديث بيانات السيارة رقم ($truckNumber)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnAmberContainerLight
                            )
                        }
                        IconButton(
                            onClick = { viewModel.resetForm() },
                            modifier = Modifier.testTag("cancel_edit_mode_button")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إلغاء التعديل",
                                tint = OnAmberContainerLight
                            )
                        }
                    }
                }
            }

            // Section 1: Identification & Smart Auto-Fill
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. بيانات السيارة والسائق",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إكمال ذكي مانع للتكرار",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Truck Number Input
                    OutlinedTextField(
                        value = truckNumber,
                        onValueChange = { viewModel.onTruckNumberChange(it) },
                        label = { Text("رقم السيارة / اللوحة *") },
                        placeholder = { Text("مثال: 12345 د ب أو 8920") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        isError = truckNumberError != null,
                        supportingText = {
                            if (truckNumberError != null) {
                                Text(
                                    text = truckNumberError!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("truck_number_input")
                    )

                    // Learned Suggestions for Truck Numbers
                    if (truckSuggestions.isNotEmpty() && editingId == null) {
                        Column {
                            Text(
                                text = "سيارات مسجلة سابقة (اضغط للاسترجاع وتحديث القياس):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                truckSuggestions.forEach { suggestion ->
                                    AssistChip(
                                        onClick = {
                                            viewModel.onTruckNumberChange(suggestion)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.History,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        label = { Text(suggestion, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Duplicate Detection Alert Banner
                    AnimatedVisibility(
                        visible = duplicateEntry != null && editingId == null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        duplicateEntry?.let { existing ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = AmberContainerLight
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = OnAmberContainerLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "هذه السيارة مسجلة مسبقاً للسائق (${existing.driverName})!",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = OnAmberContainerLight
                                        )
                                    }

                                    Text(
                                        text = "لمنع تكرار رقم السيارة مرتين، يمكنك استرجاع بياناتها وتحديث القياس الحالي مباشرة:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnAmberContainerLight
                                    )

                                    Button(
                                        onClick = { viewModel.loadExistingTruckProfile(existing) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.testTag("load_existing_truck_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("استرجاع وتحديث بيانات السيارة", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Driver Name Input
                    OutlinedTextField(
                        value = driverName,
                        onValueChange = {
                            viewModel.driverName.value = it
                            if (it.isNotBlank()) viewModel.driverNameError.value = null
                        },
                        label = { Text("اسم السائق *") },
                        placeholder = { Text("مثال: أحمد محمد سالم") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        isError = driverNameError != null,
                        supportingText = {
                            if (driverNameError != null) {
                                Text(
                                    text = driverNameError!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("driver_name_input")
                    )

                    // Learned Driver Suggestions
                    if (driverSuggestions.isNotEmpty() && driverName.isNotBlank() && !driverSuggestions.contains(driverName.trim())) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "سائقون معروفون:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            driverSuggestions.take(3).forEach { dName ->
                                SuggestionChip(
                                    onClick = { viewModel.driverName.value = dName },
                                    label = { Text(dName, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Truck Configuration & Cubic Volume (رأس وذنب / رأس فقط)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "2. تكوين السيارة والتكعيب (م³)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "اختر هل السيارة تتألف من رأس وذنب أم رأس فقط:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Interactive Toggle Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Option 1: Head & Tail (رأس وذنب)
                        val isBoth = hasHeadAndTail
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isBoth) 2.dp else 1.dp,
                                    color = if (isBoth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.onHasHeadAndTailChange(true) }
                                .testTag("select_head_and_tail"),
                            color = if (isBoth) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = if (isBoth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "رأس وذنب",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isBoth) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "قاطرة ومقطورة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isBoth) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Option 2: Head Only (رأس فقط)
                        val isHeadOnly = !hasHeadAndTail
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isHeadOnly) 2.dp else 1.dp,
                                    color = if (isHeadOnly) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.onHasHeadAndTailChange(false) }
                                .testTag("select_head_only"),
                            color = if (isHeadOnly) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = if (isHeadOnly) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "رأس فقط",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isHeadOnly) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "شاحنة مفردة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isHeadOnly) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Head Volume Field
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "كمية الرأس (بالمتر المكعب م³) *",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            // Dimensions calculator shortcut button
                            OutlinedButton(
                                onClick = { viewModel.openCalcDialog(CalcTarget.HEAD) },
                                modifier = Modifier.testTag("calc_head_button")
                            ) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حاسبة الأبعاد", fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = headVolume,
                            onValueChange = {
                                viewModel.headVolumeInput.value = it
                                if (it.isNotBlank()) viewModel.headVolumeError.value = null
                            },
                            placeholder = { Text("مثال: 14.50") },
                            suffix = { Text("م³", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = headVolumeError != null,
                            supportingText = {
                                if (headVolumeError != null) {
                                    Text(
                                        text = headVolumeError!!,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("head_volume_input")
                        )
                    }

                    // Tail Volume Field (Visible only if hasHeadAndTail is true)
                    AnimatedVisibility(
                        visible = hasHeadAndTail,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "كمية الذنب (بالمتر المكعب م³) *",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                OutlinedButton(
                                    onClick = { viewModel.openCalcDialog(CalcTarget.TAIL) },
                                    modifier = Modifier.testTag("calc_tail_button")
                                ) {
                                    Icon(
                                        Icons.Default.Calculate,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حاسبة الأبعاد", fontSize = 12.sp)
                                }
                            }

                            OutlinedTextField(
                                value = tailVolume,
                                onValueChange = {
                                    viewModel.tailVolumeInput.value = it
                                    if (it.isNotBlank()) viewModel.tailVolumeError.value = null
                                },
                                placeholder = { Text("مثال: 18.00") },
                                suffix = { Text("م³", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = tailVolumeError != null,
                                supportingText = {
                                    if (tailVolumeError != null) {
                                        Text(
                                            text = tailVolumeError!!,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tail_volume_input")
                            )
                        }
                    }

                    // Live Total Volume Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "إجمالي التكعيب الكلي:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (hasHeadAndTail) "مجموع (الرأس + الذنب)" else "تكعيب الرأس المفرد",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%.2f م³", calculatedTotal),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.testTag("calculated_total_display")
                            )
                        }
                    }
                }
            }

            // Section 3: Extensions (الوصلات)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3. معلومات الوصلات (الزوائد والارتفاعات)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "هل تحتوي السيارة على وصلات؟",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasExtensions) "تحتوي على وصلات إضافية" else "بدون وصلات (صندوق قياسي)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = hasExtensions,
                            onCheckedChange = { viewModel.onHasExtensionsChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("has_extensions_switch")
                        )
                    }

                    // Optional extension detail input if checked
                    AnimatedVisibility(
                        visible = hasExtensions,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        OutlinedTextField(
                            value = extensionDetails,
                            onValueChange = { viewModel.extensionDetails.value = it },
                            label = { Text("تفاصيل أو ارتفاع الوصلات (اختياري)") },
                            placeholder = { Text("مثال: وصلة جانبية 40 سم أو وصلتين خشب") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = SuccessGreen
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("extension_details_input")
                        )
                    }
                }
            }

            // Section 4: Extra Details (نوع المادة والموقع والملاحظات)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "4. تفاصيل إضافية (اختياري)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Quick material chips
                    Text(
                        text = "نوع الحمولة / المادة:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        combinedMaterials.forEach { mat ->
                            val isSelected = materialType == mat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.materialType.value = if (isSelected) "" else mat
                                },
                                label = { Text(mat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = materialType,
                        onValueChange = { viewModel.materialType.value = it },
                        label = { Text("نوع المادة") },
                        placeholder = { Text("أو اكتب نوع المادة يدوياً") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("material_type_input")
                    )

                    // Site / Project with suggestions
                    OutlinedTextField(
                        value = siteLocation,
                        onValueChange = { viewModel.siteLocation.value = it },
                        label = { Text("الموقع / المشروع / الكسارة") },
                        placeholder = { Text("مثال: مشروع المطار أو كسارة الشمال") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("site_location_input")
                    )

                    if (knownSites.isNotEmpty() && siteLocation.isBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مواقع سابقة:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            knownSites.take(3).forEach { site ->
                                SuggestionChip(
                                    onClick = { viewModel.siteLocation.value = site },
                                    label = { Text(site, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.notes.value = it },
                        label = { Text("ملاحظات") },
                        placeholder = { Text("أية ملاحظات حول الحمولة أو القياس") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notes_input")
                    )
                }
            }

            // Submit & Reset Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.saveEntry() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_truck_entry_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (editingId != null) Icons.Default.CheckCircle else Icons.Default.Save,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (editingId != null) "تحديث بيانات التكعيب" else "حفظ بيانات التكعيب",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.resetForm() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reset_form_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إعادة تعيين الحقول")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Duplicate Prevention Confirmation Modal
    if (showDuplicateDialog && duplicateEntry != null) {
        val existing = duplicateEntry!!
        AlertDialog(
            onDismissRequest = { viewModel.showDuplicateConfirmDialog.value = false },
            icon = {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "رقم السيارة مسجل مسبقاً!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("السيارة رقم ($truckNumber) مسجلة مسبقاً باسم السائق (${existing.driverName}).")
                    Text("تطبيقاً لقاعدة عدم تكرار رقم السيارة مرتين، هل ترغب في تحديث بيانات القياس لهذه السيارة، أم تعديل رقم السيارة؟")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.loadExistingTruckProfile(existing)
                        viewModel.saveEntry(forceUpsert = true)
                    },
                    modifier = Modifier.testTag("confirm_update_existing_truck_button")
                ) {
                    Text("تحديث بيانات السيارة المسجلة")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.showDuplicateConfirmDialog.value = false }
                ) {
                    Text("تعديل رقم السيارة")
                }
            }
        )
    }
}
