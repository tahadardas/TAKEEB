package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TruckViewModel
import com.example.ui.theme.SuccessGreen
import com.example.util.ContactPickerHelper
import com.example.util.SmsBroadcastHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmsBroadcastScreen(
    viewModel: TruckViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val recipients by viewModel.smsRecipients.collectAsState()
    val selectedPhones by viewModel.selectedRecipients.collectAsState()
    val smsText by viewModel.smsMessageText.collectAsState()
    val isSending by viewModel.isSendingSms.collectAsState()

    // Price builder state
    var showPriceBuilder by remember { mutableStateOf(false) }
    val priceSand by viewModel.priceSand.collectAsState()
    val priceG1 by viewModel.priceGravel1.collectAsState()
    val priceG2 by viewModel.priceGravel2.collectAsState()
    val priceDust by viewModel.priceDust.collectAsState()
    val priceBase by viewModel.priceBase.collectAsState()
    val priceNotes by viewModel.priceNotes.collectAsState()

    // Automatically select all recipients initially if nothing is selected yet
    LaunchedEffect(recipients) {
        if (selectedPhones.isEmpty() && recipients.isNotEmpty()) {
            viewModel.selectAllRecipients()
        }
    }

    // Contact Picker Launcher for adding extra recipients from phonebook
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                val details = ContactPickerHelper.extractContactDetails(context, contactUri)
                if (details != null) {
                    val (name, phone) = details
                    viewModel.addCustomContact(name, phone)
                }
            }
        }
    }

    // Direct SMS Permission Launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.sendDirectSms(context)
        } else {
            // Permission denied, open default SMS app as safe fallback
            viewModel.openSmsApp(context)
        }
    }

    // Character and SMS count calculation
    val (charCount, smsParts) = remember(smsText) {
        SmsBroadcastHelper.calculateSmsMetrics(smsText)
    }

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
            // Header / Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "بث رسائل SMS والأسعار للسائقين",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "إرسال نشرة أسعار المواد والتنسيق الميداني لجميع السائقين بكبسة زر واحدة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "السائقون المسجلون: ${recipients.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val activeCount = if (selectedPhones.isNotEmpty()) selectedPhones.size else recipients.size
                            Text(
                                text = "المحددون للإرسال: $activeCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Section 1: Recipients Management & Contacts Picker
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
                        Column {
                            Text(
                                text = "1. قائمة أرقام السائقين المستلمين",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "حدد السائقين المراد إرسال الرسالة إليهم",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Pick Contact Button
                        Button(
                            onClick = {
                                val pickIntent = Intent(
                                    Intent.ACTION_PICK,
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                )
                                contactPickerLauncher.launch(pickIntent)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.testTag("pick_contact_to_sms_button")
                        ) {
                            Icon(
                                Icons.Default.ContactPhone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إرفاق من جهات اتصالي", fontSize = 12.sp)
                        }
                    }

                    // Selection Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تم تحديد (${if (selectedPhones.isNotEmpty()) selectedPhones.size else recipients.size}) من أصل (${recipients.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { viewModel.selectAllRecipients() },
                                modifier = Modifier.testTag("select_all_recipients_button")
                            ) {
                                Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تحديد الكل", fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = { viewModel.deselectAllRecipients() },
                                modifier = Modifier.testTag("deselect_all_recipients_button")
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إلغاء التحديد", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    if (recipients.isEmpty()) {
                        // Empty State
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد أرقام هواتف مسجلة بعد",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "أضف رقم هاتف السائق عند تسجيل السيارات أو أرفق أرقاماً الآن من جهات اتصالك.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val pickIntent = Intent(
                                        Intent.ACTION_PICK,
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                    )
                                    contactPickerLauncher.launch(pickIntent)
                                }
                            ) {
                                Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اختيار رقم من جهات الاتصال الآن")
                            }
                        }
                    } else {
                        // Driver Recipients List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            recipients.forEach { recipient ->
                                val isChecked = selectedPhones.contains(recipient.phone) || (selectedPhones.isEmpty() && recipients.size == 1)

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.toggleRecipient(recipient.phone) },
                                    color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { viewModel.toggleRecipient(recipient.phone) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = recipient.driverName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    if (recipient.isCustomContact) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "جهة اتصال",
                                                                fontSize = 10.sp,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                                            )
                                                        }
                                                    } else if (recipient.truckNumber.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "(${recipient.truckNumber})",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = recipient.phone,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        if (recipient.isCustomContact) {
                                            IconButton(
                                                onClick = { viewModel.removeCustomContact(recipient.phone) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "حذف من القائمة",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Pricing Templates & Material Price Generator
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
                            text = "2. قوالب تسعيرة المواد الجاهزة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextButton(
                            onClick = { showPriceBuilder = !showPriceBuilder },
                            modifier = Modifier.testTag("toggle_price_builder_button")
                        ) {
                            Icon(
                                Icons.Default.PriceChange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showPriceBuilder) "إخفاء حاسبة الأسعار" else "تخصيص أسعار المواد",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Quick Template Buttons
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = { viewModel.applyQuickTemplate(1) },
                            leadingIcon = {
                                Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("📋 نشرة أسعار المواد اليوم") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("template_price_button")
                        )

                        AssistChip(
                            onClick = { viewModel.applyQuickTemplate(2) },
                            leadingIcon = {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("🚚 إشعار عمل وتحميل موقع") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("template_work_button")
                        )

                        AssistChip(
                            onClick = { viewModel.applyQuickTemplate(3) },
                            leadingIcon = {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            label = { Text("مسح النص") }
                        )
                    }

                    // Expandable Material Prices Builder
                    AnimatedVisibility(
                        visible = showPriceBuilder,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Column(
                                modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "أدخل أسعار المواد الحالية ليتم إدراجها بالرسالة تلقائياً:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = priceSand,
                                        onValueChange = { viewModel.priceSand.value = it },
                                        label = { Text("سعر الرمل الناعم (م³)") },
                                        placeholder = { Text("مثال: 45000 ل.س") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = priceG1,
                                        onValueChange = { viewModel.priceGravel1.value = it },
                                        label = { Text("سعر بحص نمرة 1") },
                                        placeholder = { Text("مثال: 40000 ل.س") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = priceG2,
                                        onValueChange = { viewModel.priceGravel2.value = it },
                                        label = { Text("سعر بحص نمرة 2") },
                                        placeholder = { Text("مثال: 38000 ل.س") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = priceDust,
                                        onValueChange = { viewModel.priceDust.value = it },
                                        label = { Text("سعر بودرة الحجر") },
                                        placeholder = { Text("مثال: 35000 ل.س") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = priceBase,
                                    onValueChange = { viewModel.priceBase.value = it },
                                    label = { Text("سعر التراب / الدفان للردميات") },
                                    placeholder = { Text("مثال: 25000 ل.س") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = priceNotes,
                                    onValueChange = { viewModel.priceNotes.value = it },
                                    label = { Text("ملاحظات وشروط التوصيل (اختياري)") },
                                    placeholder = { Text("مثال: التسليم أرض الموقع، الدفع نقداً عند الوصول") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        viewModel.generateMaterialPriceBulletin()
                                        showPriceBuilder = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("generate_price_bulletin_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("توليد وإدراج التسعيرة في نص الرسالة", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: SMS Message Composer
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. نص رسالة الـ SMS الموجهة للسائقين",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Live character and segment metrics
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "الأحرف: $charCount ($smsParts رسالة)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = smsText,
                        onValueChange = { viewModel.smsMessageText.value = it },
                        placeholder = {
                            Text("اكتب نص الرسالة هنا مع الأسعار أو استخدم أحد القوالب الجاهزة بالأعلى...")
                        },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_content_input")
                    )

                    Text(
                        text = "💡 نصيحة: ستُرسل هذه الرسالة لجميع السائقين المحددين في القائمة مع الحفاظ على صياغة موحدة وواضحة.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 4: Dispatch Actions (One-click SMS, Direct SMS, and Multi-Share)
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
                    val activeCount = if (selectedPhones.isNotEmpty()) selectedPhones.size else recipients.size

                    // Primary Button 1: Open SMS App with all recipients
                    Button(
                        onClick = { viewModel.openSmsApp(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("send_sms_to_all_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "فتح تطبيق الرسائل (SMS) لـ ($activeCount) سائقاً",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Button 2: Direct Background SMS dispatch
                    Button(
                        onClick = {
                            smsPermissionLauncher.launch(android.Manifest.permission.SEND_SMS)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_direct_sms_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSending,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ الإرسال المباشر...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إرسال SMS فوري ومباشر (عبر الشريحة)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Button 3: Share via WhatsApp & Other Apps
                    OutlinedButton(
                        onClick = { viewModel.shareSmsPricing(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("share_pricing_whatsapp_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مشاركة نشرة الأسعار عبر واتساب والتطبيقات الأخرى",
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
