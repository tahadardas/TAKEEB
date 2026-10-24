package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.ExportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("تكعيب الغضب", appName)
    }

    @Test
    fun `test parse backup json`() {
        val json = """
            {
              "appName": "TruckCubic",
              "backupVersion": 1,
              "trucks": [
                {
                  "truckNumber": "9999 أ ب",
                  "driverName": "سالم",
                  "hasHeadAndTail": true,
                  "headVolume": 15.0,
                  "tailVolume": 17.5,
                  "totalVolume": 32.5,
                  "hasExtensions": true,
                  "extensionDetails": "وصلة خشب"
                }
              ]
            }
        """.trimIndent()

        val parsed = ExportManager.parseBackupJson(json)
        assertEquals(1, parsed.size)
        val truck = parsed[0]
        assertEquals("9999 أ ب", truck.truckNumber)
        assertEquals("سالم", truck.driverName)
        assertEquals(32.5, truck.totalVolume, 0.001)
        assertTrue(truck.hasExtensions)
    }

    @Test
    fun `test phone number cleaning`() {
        val raw = "+963 (098) 708-0330."
        val cleaned = com.example.util.ContactPickerHelper.cleanPhoneNumber(raw)
        assertEquals("+9630987080330", cleaned)
    }

    @Test
    fun `test sms metrics calculation for arabic text`() {
        val shortArabic = "أسعار المواد اليوم"
        val (chars, segments) = com.example.util.SmsBroadcastHelper.calculateSmsMetrics(shortArabic)
        assertEquals(18, chars)
        assertEquals(1, segments)

        val longArabic = "أ".repeat(80)
        val (_, longSegments) = com.example.util.SmsBroadcastHelper.calculateSmsMetrics(longArabic)
        assertEquals(2, longSegments)
    }
}
