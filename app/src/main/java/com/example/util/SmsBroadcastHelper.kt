package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

object SmsBroadcastHelper {

    /**
     * Opens system SMS app with multiple recipients pre-filled in 'To' field
     * and the pricing message body pre-filled.
     */
    fun openSmsAppForRecipients(
        context: Context,
        recipients: List<String>,
        messageBody: String
    ): Boolean {
        if (recipients.isEmpty()) return false

        val cleanRecipients = recipients.map { ContactPickerHelper.cleanPhoneNumber(it) }
            .filter { it.isNotBlank() }
            .distinct()

        if (cleanRecipients.isEmpty()) return false

        // In Android, multiple recipients in SMS URI are separated by ';' (standard) or ',' on some OEM clients (Samsung)
        val isSamsung = Build.MANUFACTURER.contains("samsung", ignoreCase = true)
        val separator = if (isSamsung) "," else ";"
        val joinedNumbers = cleanRecipients.joinToString(separator)

        return try {
            val uri = Uri.parse("smsto:$joinedNumbers")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", messageBody)
                putExtra(Intent.EXTRA_TEXT, messageBody)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w("SmsBroadcastHelper", "ACTION_SENDTO failed, trying ACTION_VIEW fallback", e)
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$joinedNumbers")
                    putExtra("sms_body", messageBody)
                    putExtra(Intent.EXTRA_TEXT, messageBody)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                true
            } catch (fallbackError: Exception) {
                Log.e("SmsBroadcastHelper", "ACTION_VIEW also failed", fallbackError)
                false
            }
        }
    }

    /**
     * Sends SMS directly using Android SmsManager in the background
     * Requires android.permission.SEND_SMS
     */
    fun sendDirectSms(
        context: Context,
        recipients: List<String>,
        messageBody: String
    ): Int {
        if (recipients.isEmpty() || messageBody.isBlank()) return 0

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        var dispatchedCount = 0
        val cleanRecipients = recipients.map { ContactPickerHelper.cleanPhoneNumber(it) }
            .filter { it.isNotBlank() }
            .distinct()

        for (phone in cleanRecipients) {
            try {
                val parts = smsManager.divideMessage(messageBody)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(phone, null, messageBody, null, null)
                }
                dispatchedCount++
            } catch (e: Exception) {
                Log.e("SmsBroadcastHelper", "Failed to dispatch SMS to $phone", e)
            }
        }

        return dispatchedCount
    }

    /**
     * Shares the message via standard Share sheet (WhatsApp, Telegram, etc.)
     */
    fun shareViaApps(context: Context, messageBody: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, messageBody)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(intent, "مشاركة نشرة الأسعار عبر:").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }

    /**
     * Calculates number of SMS segments based on Arabic/GSM character encoding
     */
    fun calculateSmsMetrics(text: String): Pair<Int, Int> {
        val length = text.length
        if (length == 0) return Pair(0, 0)

        // Arabic text uses UCS-2 (Unicode) encoding: 70 chars for single SMS, 67 per segment if concatenated
        val isUnicode = text.any { it.code > 127 }
        val segments = if (isUnicode) {
            if (length <= 70) 1 else ((length + 66) / 67)
        } else {
            if (length <= 160) 1 else ((length + 152) / 153)
        }
        return Pair(length, segments)
    }
}
