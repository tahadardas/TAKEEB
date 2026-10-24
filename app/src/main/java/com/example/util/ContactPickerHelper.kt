package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

object ContactPickerHelper {

    /**
     * Extracts phone number and contact display name from a contact picker Uri.
     * Returns Pair(displayName, phoneNumber) or null if extraction fails.
     */
    fun extractContactDetails(context: Context, uri: Uri): Pair<String, String>? {
        return try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

                    val rawNumber = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""
                    val rawName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""

                    val cleanNumber = cleanPhoneNumber(rawNumber)
                    if (cleanNumber.isNotBlank()) {
                        Pair(rawName.trim(), cleanNumber)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ContactPickerHelper", "Failed to resolve contact from uri: $uri", e)
            null
        }
    }

    /**
     * Cleans phone number by removing unwanted spacing, dashes, and parentheses while preserving valid digits and '+'
     */
    fun cleanPhoneNumber(phone: String): String {
        return phone.replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace(".", "")
            .trim()
    }
}
