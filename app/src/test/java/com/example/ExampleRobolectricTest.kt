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
@Config(sdk = [36])
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

    @Test
    fun `test zip backup creation and restoration with photos`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val photoDir = java.io.File(context.filesDir, "truck_photos")
        photoDir.mkdirs()
        val dummyPhoto = java.io.File(photoDir, "IMG_CAMERA_test.jpg")
        dummyPhoto.writeText("fake image bytes")

        val entry = com.example.data.local.entity.TruckEntry(
            truckNumber = "12345 دمشق",
            driverName = "خالد",
            driverPhone = "0987080330",
            hasHeadAndTail = true,
            headVolume = 12.0,
            tailVolume = 14.0,
            totalVolume = 26.0,
            hasExtensions = false,
            photoPaths = dummyPhoto.absolutePath
        )

        val zipUri = ExportManager.createCompressedBackupZip(context, listOf(entry))
        assertTrue(zipUri.toString().isNotEmpty())

        val db = com.example.data.local.database.AppDatabase.getDatabase(context)
        val repo = com.example.data.repository.TruckRepository(db.truckEntryDao())

        val restoreResult = ExportManager.restoreBackup(context, zipUri, repo)
        assertTrue(restoreResult.success)
        assertEquals(1, restoreResult.totalCount)
        assertEquals(1, restoreResult.restoredPhotosCount)

        val restoredEntry = repo.getEntryByTruckNumber("12345 دمشق")
        org.junit.Assert.assertNotNull(restoredEntry)
        assertEquals("خالد", restoredEntry!!.driverName)
        assertTrue(restoredEntry.photoPaths.contains("IMG_CAMERA_test.jpg"))
    }

    @Test
    fun `test truck modification recording and volume diff calculation`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.local.database.AppDatabase.getDatabase(context)
        val repo = com.example.data.repository.TruckRepository(
            dao = db.truckEntryDao(),
            modificationDao = db.truckModificationDao()
        )

        val originalTruck = com.example.data.local.entity.TruckEntry(
            truckNumber = "778899 حلب",
            driverName = "محمود",
            driverPhone = "0987080330",
            hasHeadAndTail = true,
            headVolume = 10.0,
            tailVolume = 12.0,
            totalVolume = 22.0,
            hasExtensions = false
        )
        val insertedId = repo.insert(originalTruck)
        val saved = repo.getEntryById(insertedId)!!
        assertEquals(22.0, saved.totalVolume, 0.001)

        // Record a modification: adding extensions of 30cm, increasing head by 1.5 and tail by 1.5 -> total 25.0 (+3.0)
        val newHead = 11.5
        val newTail = 13.5
        val newTotal = 25.0
        val diff = newTotal - saved.totalVolume // +3.0

        val mod = com.example.data.local.entity.TruckModification(
            truckNumber = saved.truckNumber,
            modificationType = "إضافة وصلة",
            previousHeadVolume = saved.headVolume,
            previousTailVolume = saved.tailVolume,
            previousTotalVolume = saved.totalVolume,
            previousHasExtensions = saved.hasExtensions,
            newHeadVolume = newHead,
            newTailVolume = newTail,
            newTotalVolume = newTotal,
            newHasExtensions = true,
            newExtensionDetails = "وصلة صاج 30 سم",
            volumeDifference = diff,
            notes = "تم تركيب وصلة بالورشة"
        )

        val updatedTruck = saved.copy(
            headVolume = newHead,
            tailVolume = newTail,
            totalVolume = newTotal,
            hasExtensions = true,
            extensionDetails = "وصلة صاج 30 سم"
        )

        repo.recordTruckModification(updatedTruck, mod)

        // Verify active truck has updated volume
        val active = repo.getEntryByTruckNumber("778899 حلب")!!
        assertEquals(25.0, active.totalVolume, 0.001)
        assertTrue(active.hasExtensions)

        // Verify modification history recorded correctly
        val history = repo.getModificationsForTruckAscSnapshot("778899 حلب")
        assertEquals(1, history.size)
        val recordedMod = history[0]
        assertEquals("إضافة وصلة", recordedMod.modificationType)
        assertEquals(22.0, recordedMod.previousTotalVolume, 0.001)
        assertEquals(25.0, recordedMod.newTotalVolume, 0.001)
        assertEquals(3.0, recordedMod.volumeDifference, 0.001)
        assertEquals("وصلة صاج 30 سم", recordedMod.newExtensionDetails)
    }
}
