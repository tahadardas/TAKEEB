package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TruckViewModel
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SuccessGreen

@Composable
fun VpsSyncDialog(
    viewModel: TruckViewModel,
    modifier: Modifier = Modifier
) {
    val showDialog by viewModel.showVpsSyncDialog.collectAsState()
    if (!showDialog) return

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val serverUrlState by viewModel.vpsServerUrl.collectAsState()
    val apiKeyState by viewModel.vpsApiKey.collectAsState()
    val autoSyncState by viewModel.vpsAutoSyncEnabled.collectAsState()
    val isSyncing by viewModel.isVpsSyncing.collectAsState()
    val syncStatusMessage by viewModel.vpsSyncStatusMessage.collectAsState()
    val lastSyncFormatted by viewModel.vpsLastSyncTimeFormatted.collectAsState()
    val isConnected by viewModel.vpsIsConnected.collectAsState()

    var inputUrl by remember(serverUrlState) { mutableStateOf(serverUrlState) }
    var inputApiKey by remember(apiKeyState) { mutableStateOf(apiKeyState) }
    var inputAutoSync by remember(autoSyncState) { mutableStateOf(autoSyncState) }
    var showApiKey by remember { mutableStateOf(false) }
    var showQuickGuide by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isSyncing) viewModel.showVpsSyncDialog.value = false
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مزامنة سيرفر الشركة VPS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تبادل السجلات والصور بين جميع الهواتف",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = { viewModel.showVpsSyncDialog.value = false },
                    enabled = !isSyncing
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
                // Connection Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (isConnected) {
                            true -> SuccessGreen.copy(alpha = 0.12f)
                            false -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = when (isConnected) {
                                    true -> Icons.Default.CloudDone
                                    false -> Icons.Default.CloudOff
                                    null -> Icons.Default.CloudQueue
                                },
                                contentDescription = null,
                                tint = when (isConnected) {
                                    true -> SuccessGreen
                                    false -> MaterialTheme.colorScheme.error
                                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = when (isConnected) {
                                        true -> "السيرفر متصل وجاهز للمزامنة"
                                        false -> "تعذر الاتصال بالسيرفر"
                                        null -> "حالة الاتصال: غير مفحوص بعد"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when (isConnected) {
                                        true -> SuccessGreen
                                        false -> MaterialTheme.colorScheme.error
                                        null -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = "آخر مزامنة: $lastSyncFormatted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }

                // Detailed message if any
                if (!syncStatusMessage.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = syncStatusMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                HorizontalDivider()

                // Server Configuration Inputs
                Text(
                    text = "بيانات الربط مع السيرفر (Plesk / Node.js)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("رابط السيرفر (URL)") },
                    placeholder = { Text("https://sync.yourdomain.com:3000") },
                    leadingIcon = {
                        Icon(Icons.Default.Dns, contentDescription = null)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vps_server_url_input")
                )

                OutlinedTextField(
                    value = inputApiKey,
                    onValueChange = { inputApiKey = it },
                    label = { Text("مفتاح الأمان السري للشركة (API Key)") },
                    placeholder = { Text("تكعيب-الغضب-2026-سري") },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "إخفاء" else "إظهار"
                            )
                        }
                    },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vps_api_key_input")
                )

                // Auto-sync Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مزامنة تلقائية عند فتح التطبيق",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "يتم تنزيل السيارات الجديدة والصور ورفع السجلات في الخلفية",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = inputAutoSync,
                        onCheckedChange = { inputAutoSync = it },
                        modifier = Modifier.testTag("vps_auto_sync_switch")
                    )
                }

                // Save and Test Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.saveVpsSettings(context, inputUrl, inputApiKey, inputAutoSync)
                            viewModel.testVpsConnection(context)
                        },
                        enabled = !isSyncing && inputUrl.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vps_test_connection_button")
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فحص الاتصال")
                    }

                    Button(
                        onClick = {
                            viewModel.saveVpsSettings(context, inputUrl, inputApiKey, inputAutoSync)
                        },
                        enabled = !isSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vps_save_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ الإعدادات")
                    }
                }

                HorizontalDivider()

                // Sync Now Primary Action
                Button(
                    onClick = {
                        viewModel.saveVpsSettings(context, inputUrl, inputApiKey, inputAutoSync)
                        viewModel.performVpsSync(context)
                    },
                    enabled = !isSyncing && inputUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("vps_sync_now_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("جارٍ المزامنة ونقل البيانات والصور...")
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مزامنة فورية الآن (رفع وتنزيل السجلات والصور)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Help / Guide Toggle
                TextButton(
                    onClick = { showQuickGuide = !showQuickGuide },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showQuickGuide) "إخفاء دليل تهيئة السيرفر في Plesk" else "كيف أقوم بتشغيل السيرفر في لوحة تحكم Plesk؟",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                AnimatedVisibility(visible = showQuickGuide) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "خطوات تشغيل السيرفر في Plesk (بسيطة جداً):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "1. داخل مجلد المشروع ستجد مجلد باسم vps_server يحتوي على ملفات Node.js الجاهزة بالكامل.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "2. في لوحة تحكم Plesk: اذهب إلى مجلد الموقع في File Manager وارفع محتويات vps_server (الملفات: server.js, package.json, .env.example).",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "3. انقر على أيقونة Node.js في Plesk، وحدد Application Startup File = server.js ثم اضغط NPM Install ثم Restart Application.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "4. انسخ رابط موقعك وضعه في خانة 'رابط السيرفر' أعلاه ومبروك المزامنة الحية لجميع هواتف الشركة!",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SuccessGreen
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.showVpsSyncDialog.value = false },
                enabled = !isSyncing
            ) {
                Text("إغلاق")
            }
        }
    )
}
