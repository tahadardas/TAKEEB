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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.local.database.AppDatabase
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

    // File Picker Launcher for restoring SQLite database (.sqlite/.db), ZIP, or JSON backup
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
                    text = "قاعدة بيانات SQLite والنسخ الاحتياطي",
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
                modifier = modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Text("جارٍ معالجة وتصدير بيانات SQLite وحفظ الملفات...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    // SQLite Database Status Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "محرك البيانات: SQLite v3 (Room ORM)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = AppDatabase.getDatabaseSizeFormatted(context),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "الملف النشط: ${AppDatabase.DATABASE_NAME}.sqlite • حفظ محلي فوري وآمن بدون إنترنت",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Folder Location Notice
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "مجلد التطبيق الرئيسي في «ملفاتي»: تكعيب الغضب",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "مباشرة في الواجهة الرئيسية لملفاتي، وتُحفظ ملفات SQLite والنسخ في «ملفات_البرنامج».",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // VPS Cloud Server Sync Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.showExportBackupSheet.value = false
                                viewModel.showVpsSyncDialog.value = true
                            },
                        color = CyanPrimary.copy(alpha = 0.12f),
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyanPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مزامنة سيرفر الشركة VPS (الربط المباشر)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تبادل تلقائي للسجلات وتنزيل ورفع الصور مع سيرفر الشركة عبر الإنترنت",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "النسخ الاحتياطي لقاعدة البيانات:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Option: SQLite Database Direct Backup (.sqlite)
                    ExportActionTile(
                        icon = Icons.Default.Storage,
                        iconColor = Color(0xFF0284C7), // Sky Blue
                        title = "تصدير ملف قاعدة بيانات SQLite (.sqlite)",
                        subtitle = "نسخة خام احترافية كاملة ومقفلة بنظام WAL Checkpoint، جاهزة للنقل والاسترجاع الفوري بأي وقت",
                        testTag = "export_sqlite_button",
                        onClick = {
                            viewModel.exportSqliteDatabase(context)
                            viewModel.showExportBackupSheet.value = false
                        }
                    )

                    // Option: Compressed ZIP Backup with Photos
                    ExportActionTile(
                        icon = Icons.Default.Archive,
                        iconColor = AmberPrimary,
                        title = "نسخة احتياطية مضغوطة شاملة الصور (ZIP)",
                        subtitle = "ضغط كامل سجلات التكعيب وتعديلاتها مع صور الكاميرا لإرسالها بالواتساب",
                        testTag = "export_zip_backup_button",
                        onClick = {
                            viewModel.exportBackup(context, includePhotos = true)
                            viewModel.showExportBackupSheet.value = false
                        }
                    )

                    // Option: Quick JSON Backup without photos
                    ExportActionTile(
                        icon = Icons.Default.TextSnippet,
                        iconColor = Color(0xFF6366F1), // Indigo
                        title = "نسخة احتياطية خفيفة بدون صور (JSON)",
                        subtitle = "ملف نصي سريع للبيانات والجداول فقط",
                        testTag = "export_json_backup_button",
                        onClick = {
                            viewModel.exportBackup(context, includePhotos = false)
                            viewModel.showExportBackupSheet.value = false
                        }
                    )

                    // Option: Restore Backup (SQLite / ZIP / JSON)
                    ExportActionTile(
                        icon = Icons.Default.Sync,
                        iconColor = CyanPrimary,
                        title = "استعادة البيانات من ملف (SQLite / ZIP / JSON)",
                        subtitle = "استيراد قاعدة بيانات .sqlite أو ملف .db أو أرشيف ZIP وتحديث البيانات دون مسح أي سجلات قديمة",
                        testTag = "restore_backup_button",
                        onClick = {
                            restoreLauncher.launch(
                                arrayOf(
                                    "application/x-sqlite3",
                                    "application/vnd.sqlite3",
                                    "application/octet-stream",
                                    "application/zip",
                                    "application/x-zip-compressed",
                                    "application/json",
                                    "*/*"
                                )
                            )
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Text(
                        text = "التقارير والمشاركات الميدانية:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option: PDF Export
                    ExportActionTile(
                        icon = Icons.Default.PictureAsPdf,
                        iconColor = Color(0xFFDC2626), // Red
                        title = "تصدير تقرير PDF رسمي",
                        subtitle = "تقرير جاهز للطباعة والمشاركة مع جداول التكعيب وتفاصيل السيارات",
                        testTag = "export_pdf_button",
                        onClick = {
                            viewModel.exportToPdf(context)
                            viewModel.showExportBackupSheet.value = false
                        }
                    )

                    // Option: CSV Export
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
