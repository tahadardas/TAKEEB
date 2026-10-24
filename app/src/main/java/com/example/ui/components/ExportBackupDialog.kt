package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TruckViewModel
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SuccessGreen

@Composable
fun ExportBackupDialog(
    viewModel: TruckViewModel,
    modifier: Modifier = Modifier
) {
    val showDialog by viewModel.showExportBackupSheet.collectAsState()
    if (!showDialog) return

    val context = LocalContext.current
    val isExporting by viewModel.isExporting.collectAsState()

    // File Picker Launcher for restoring JSON backup
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreBackupFromUri(context, uri)
            viewModel.showExportBackupSheet.value = false
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isExporting) viewModel.showExportBackupSheet.value = false
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التصدير والنسخ الاحتياطي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { viewModel.showExportBackupSheet.value = false },
                    enabled = !isExporting
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isExporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("جارٍ المعالجة وتجهيز الملف...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Text(
                        text = "اختر التنسيق المطلوب لتصدير ومشاركة بيانات التكعيب:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: PDF Export
                    ExportActionTile(
                        icon = Icons.Default.PictureAsPdf,
                        iconColor = Color(0xFFDC2626), // Red
                        title = "تصدير تقرير PDF",
                        subtitle = "تقرير رسمي جاهز للطباعة والمشاركة مع جداول القياسات",
                        testTag = "export_pdf_button",
                        onClick = {
                            viewModel.exportToPdf(context)
                            viewModel.showExportBackupSheet.value = false
                        }
                    )

                    // Option 2: CSV Export
                    ExportActionTile(
                        icon = Icons.Default.TableChart,
                        iconColor = SuccessGreen,
                        title = "تصدير ملف CSV (Excel)",
                        subtitle = "متوافق مع برنامج إكسل ويدعم اللغة العربية بدقة",
                        testTag = "export_csv_button",
                        onClick = {
                            viewModel.exportToCsv(context)
                            viewModel.showExportBackupSheet.value = false
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Text(
                        text = "النسخ الاحتياطي والنقل لهاتف آخر:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Option 3: Backup JSON (with WhatsApp share)
                    ExportActionTile(
                        icon = Icons.Default.CloudUpload,
                        iconColor = AmberPrimary,
                        title = "مشاركة نسخة احتياطية (واتساب/سحابياً)",
                        subtitle = "حفظ ومشاركة ملف البيانات لاستعادته على أي هاتف آخر",
                        testTag = "export_backup_button",
                        onClick = {
                            viewModel.exportBackup(context)
                            viewModel.showExportBackupSheet.value = false
                        }
                    )

                    // Option 4: Restore Backup (Merge / Upsert mode)
                    ExportActionTile(
                        icon = Icons.Default.Sync,
                        iconColor = CyanPrimary,
                        title = "استعادة وتحديث البيانات",
                        subtitle = "تحديث البيانات المسجلة وإضافة السيارات الجديدة دون مسح أي بيانات سابقة",
                        testTag = "restore_backup_button",
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/json", "*/*"))
                        }
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ExportActionTile(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
