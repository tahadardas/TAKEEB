package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.TruckEntry
import com.example.ui.theme.AmberContainerLight
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CyanContainerLight
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnAmberContainerLight
import com.example.ui.theme.OnCyanContainerLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TruckCard(
    entry: TruckEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    modificationCount: Int = 0,
    onOpenModifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
    val formattedDate = dateFormatter.format(Date(entry.createdAt))
    val photos = entry.getPhotoList()

    var previewPhotoPath by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("truck_card_${entry.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Truck Number Badge and Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // License Plate Style Truck Number
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = entry.truckNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Total Volume Hero Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "الإجمالي",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f م³", entry.totalVolume),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Driver Name & Phone Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "السائق: ${entry.driverName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Phone quick call / chat
                if (entry.driverPhone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { dialPhone(context, entry.driverPhone) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "اتصال",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = entry.driverPhone,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { openWhatsApp(context, entry.driverPhone) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = "واتساب",
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chips: Type, Extensions, Material, Location
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Truck Type Chip
                if (entry.hasHeadAndTail) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("رأس وذنب", fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = null
                    )
                } else {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("رأس فقط", fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        border = null
                    )
                }

                // Extensions Chip
                if (entry.hasExtensions) {
                    SuggestionChip(
                        onClick = {},
                        icon = {
                            Icon(
                                Icons.Default.Extension,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            val txt = if (entry.extensionDetails.isNotBlank()) {
                                "مع وصلات (${entry.extensionDetails})"
                            } else {
                                "مع وصلات"
                            }
                            Text(txt, fontWeight = FontWeight.Bold)
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = SuccessGreenContainer,
                            labelColor = SuccessGreen
                        ),
                        border = null
                    )
                } else {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("بدون وصلات") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                }

                // Material Type Chip (if present)
                if (entry.materialType.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(entry.materialType) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                // Site Location (if present)
                if (entry.siteLocation.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = { Text(entry.siteLocation) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                // Camera photos badge chip
                if (photos.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = { Text("${photos.size} صور كاميرا حصرية", fontWeight = FontWeight.Bold, color = SuccessGreen) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SuccessGreenContainer
                        )
                    )
                }

                // Modifications history badge chip
                if (modificationCount > 0) {
                    AssistChip(
                        onClick = onOpenModifications,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            Text(
                                "معدّلة ($modificationCount مرات)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Camera Photos Thumbnails Row
            if (photos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    photos.forEach { path ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { previewPhotoPath = path }
                        ) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = "صورة السيارة",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Volume Breakdown Grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Head Volume
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "كمية الرأس",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.2f م³", entry.headVolume),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (entry.hasHeadAndTail) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        )

                        // Tail Volume
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "كمية الذنب",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.2f م³", entry.tailVolume),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    // Total
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "المجموع",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.2f م³", entry.totalVolume),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Notes if any
            if (entry.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ملاحظات: ${entry.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Section (No vertical squishing, robust RTL layout)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Workflow Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit Button
                    FilledTonalButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_truck_${entry.id}"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تعديل",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Duplicate / Second load
                    OutlinedButton(
                        onClick = onDuplicate,
                        modifier = Modifier
                            .weight(1.15f)
                            .testTag("duplicate_truck_${entry.id}"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "حمولة ثانية",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Modifications History / Box Upgrade button
                    val hasMods = modificationCount > 0
                    Button(
                        onClick = onOpenModifications,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("modifications_truck_${entry.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = if (hasMods) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        border = if (hasMods) null else ButtonDefaults.outlinedButtonBorder,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val btnText = if (hasMods) "التعديلات ($modificationCount)" else "تعديل الصندوق"
                        Text(
                            text = btnText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Secondary Actions Row: Share Receipt & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { shareTruckReceipt(context, entry, formattedDate) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_truck_${entry.id}"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مشاركة إشعار التكعيب",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .testTag("delete_truck_${entry.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "حذف",
                            fontSize = 12.sp,
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    // Photo preview dialog
    if (previewPhotoPath != null) {
        AlertDialog(
            onDismissRequest = { previewPhotoPath = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "صورة الكاميرا الميدانية",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { previewPhotoPath = null }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = File(previewPhotoPath!!),
                        contentDescription = "معاينة صورة السيارة",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { previewPhotoPath = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

private fun dialPhone(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun openWhatsApp(context: Context, phone: String) {
    try {
        val clean = phone.replace("+", "").replace(" ", "").trim()
        val url = "https://api.whatsapp.com/send?phone=$clean"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun shareTruckReceipt(context: Context, entry: TruckEntry, formattedDate: String) {
    val typeText = if (entry.hasHeadAndTail) "رأس وذنب (قاطرة ومقطورة)" else "رأس فقط (شاحنة مفردة)"
    val extText = if (entry.hasExtensions) {
        if (entry.extensionDetails.isNotBlank()) "نعم - ${entry.extensionDetails}" else "نعم (تحتوي على وصلات)"
    } else {
        "لا (بدون وصلات)"
    }

    val photos = entry.getPhotoList()

    val reportText = buildString {
        appendLine("📋 إشعار قياس وتكعيب - تطبيق تكعيب الغضب 📋")
        appendLine("══════════════════════════")
        appendLine("🚛 رقم السيارة: ${entry.truckNumber}")
        appendLine("👤 اسم السائق: ${entry.driverName}")
        if (entry.driverPhone.isNotBlank()) {
            appendLine("📞 هاتف السائق: ${entry.driverPhone}")
        }
        appendLine("⚙️ نوع التكوين: $typeText")
        appendLine("📏 كمية الرأس: ${String.format(Locale.US, "%.2f", entry.headVolume)} م³")
        if (entry.hasHeadAndTail) {
            appendLine("📐 كمية الذنب: ${String.format(Locale.US, "%.2f", entry.tailVolume)} م³")
        }
        appendLine("📦 إجمالي التكعيب: ${String.format(Locale.US, "%.2f", entry.totalVolume)} م³")
        appendLine("🔗 وجود وصلات: $extText")
        if (photos.isNotEmpty()) {
            appendLine("📷 صور التوثيق: مرفق ${photos.size} صور ملتقطة من الكاميرا حصراً")
        }
        if (entry.materialType.isNotBlank()) {
            appendLine("🧱 نوع المادة: ${entry.materialType}")
        }
        if (entry.siteLocation.isNotBlank()) {
            appendLine("📍 الموقع/المشروع: ${entry.siteLocation}")
        }
        if (entry.notes.isNotBlank()) {
            appendLine("📝 ملاحظات: ${entry.notes}")
        }
        appendLine("🕒 التاريخ والوقت: $formattedDate")
        appendLine("══════════════════════════")
        appendLine("نظام تكعيب الغضب - هاتف المتابعة: 0987080330")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, reportText)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "مشاركة تفاصيل التكعيب")
    context.startActivity(shareIntent)
}
