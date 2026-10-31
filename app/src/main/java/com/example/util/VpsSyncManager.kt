package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.local.entity.TruckEntry
import com.example.data.local.entity.TruckModification
import com.example.data.repository.TruckRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class SyncSummary(
    val success: Boolean,
    val pushedTrucks: Int = 0,
    val pushedModifications: Int = 0,
    val pulledTrucks: Int = 0,
    val pulledModifications: Int = 0,
    val uploadedPhotos: Int = 0,
    val downloadedPhotos: Int = 0,
    val errorMessage: String? = null,
    val serverTime: Long = System.currentTimeMillis()
)

object VpsSyncManager {

    private const val TAG = "VpsSyncManager"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun cleanUrl(rawUrl: String): String {
        var url = rawUrl.trim().removeSuffix("/")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        return url
    }

    /**
     * Tests connection to the VPS server and validates company API key
     */
    fun testConnection(serverUrl: String, apiKey: String): Pair<Boolean, String> {
        val clean = cleanUrl(serverUrl)
        if (clean.isBlank()) return Pair(false, "يرجى إدخال رابط السيرفر")

        return try {
            val request = Request.Builder()
                .url("$clean/api/status")
                .addHeader("x-api-key", apiKey.trim())
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val stats = json.optJSONObject("statistics")
                    val trucks = stats?.optInt("totalTrucks", 0) ?: 0
                    val volume = stats?.optDouble("totalCubicVolume", 0.0) ?: 0.0
                    val photos = stats?.optInt("totalPhotosOnServer", 0) ?: 0
                    Pair(true, "متصل بالسيرفر بنجاح! السجلات في السيرفر: $trucks سيارة (${String.format(java.util.Locale.US, "%.1f", volume)} م³)، الصور: $photos")
                } else if (response.code == 401) {
                    Pair(false, "رمز أمان الشركة (Secret API Key) غير صحيح")
                } else {
                    Pair(false, "رد السيرفر برمز خطأ: ${response.code} (${response.message})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed: ${e.message}", e)
            Pair(false, "تعذر الاتصال بالسيرفر: ${e.localizedMessage}")
        }
    }

    /**
     * Full bidirectional synchronization with the VPS server:
     * 1. PUSH: Uploads local trucks & modifications to server.
     * 2. UPLOAD PHOTOS: Uploads local photos that are missing on the server.
     * 3. PULL: Retrieves updated trucks & modifications from server since last sync.
     * 4. DOWNLOAD PHOTOS: Downloads newly added photos from other devices into local storage.
     * 5. MERGE: Non-destructively upserts records and saves master local backup.
     */
    suspend fun syncWithServer(
        context: Context,
        repository: TruckRepository,
        serverUrl: String,
        apiKey: String,
        lastSyncTime: Long
    ): SyncSummary {
        val clean = cleanUrl(serverUrl)
        if (clean.isBlank()) {
            return SyncSummary(false, errorMessage = "لم يتم تحديد رابط السيرفر")
        }

        return try {
            val localTrucks = repository.getAllEntriesSnapshot()
            val localMods = repository.getAllModificationsSnapshot()

            // 1. PUSH local data to VPS
            val pushPayload = JSONObject().apply {
                val trucksArr = JSONArray()
                for (t in localTrucks) {
                    trucksArr.put(JSONObject().apply {
                        put("truckNumber", t.truckNumber)
                        put("driverName", t.driverName)
                        put("driverPhone", t.driverPhone)
                        put("hasHeadAndTail", t.hasHeadAndTail)
                        put("headVolume", t.headVolume)
                        put("tailVolume", t.tailVolume)
                        put("totalVolume", t.totalVolume)
                        put("hasExtensions", t.hasExtensions)
                        put("extensionDetails", t.extensionDetails)
                        put("materialType", t.materialType)
                        put("siteLocation", t.siteLocation)
                        put("notes", t.notes)
                        put("photoPaths", extractCleanFileNames(t.photoPaths))
                        put("createdAt", t.createdAt)
                    })
                }
                put("trucks", trucksArr)

                val modsArr = JSONArray()
                for (m in localMods) {
                    modsArr.put(JSONObject().apply {
                        put("truckNumber", m.truckNumber)
                        put("modificationDate", m.modificationDate)
                        put("modificationType", m.modificationType)
                        put("previousHeadVolume", m.previousHeadVolume)
                        put("previousTailVolume", m.previousTailVolume)
                        put("previousTotalVolume", m.previousTotalVolume)
                        put("previousHasExtensions", m.previousHasExtensions)
                        put("previousExtensionDetails", m.previousExtensionDetails)
                        put("newHeadVolume", m.newHeadVolume)
                        put("newTailVolume", m.newTailVolume)
                        put("newTotalVolume", m.newTotalVolume)
                        put("newHasExtensions", m.newHasExtensions)
                        put("newExtensionDetails", m.newExtensionDetails)
                        put("volumeDifference", m.volumeDifference)
                        put("notes", m.notes)
                        put("photoPaths", extractCleanFileNames(m.photoPaths))
                    })
                }
                put("modifications", modsArr)
            }

            val pushRequest = Request.Builder()
                .url("$clean/api/sync/push")
                .addHeader("x-api-key", apiKey.trim())
                .post(pushPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val pushResponse = httpClient.newCall(pushRequest).execute()
            if (!pushResponse.isSuccessful) {
                val errBody = pushResponse.body?.string() ?: ""
                pushResponse.close()
                return SyncSummary(
                    success = false,
                    errorMessage = "فشل إرسال البيانات للسيرفر: رمز ${pushResponse.code} $errBody"
                )
            }
            pushResponse.close()

            // 2. Upload unique local photos to VPS
            var uploadedPhotosCount = 0
            val allLocalPhotoPaths = (localTrucks.flatMap { it.getPhotoList() } + localMods.flatMap { it.getPhotoList() }).distinct()
            for (path in allLocalPhotoPaths) {
                val file = File(path)
                if (file.exists() && file.length() > 0) {
                    try {
                        val uploaded = uploadPhotoFile(clean, apiKey, file)
                        if (uploaded) uploadedPhotosCount++
                    } catch (e: Exception) {
                        Log.w(TAG, "Photo upload skipped for ${file.name}: ${e.message}")
                    }
                }
            }

            // 3. PULL changes from VPS
            val pullUrl = "$clean/api/sync/pull?since=$lastSyncTime"
            val pullRequest = Request.Builder()
                .url(pullUrl)
                .addHeader("x-api-key", apiKey.trim())
                .get()
                .build()

            val pullResponse = httpClient.newCall(pullRequest).execute()
            if (!pullResponse.isSuccessful) {
                val err = pullResponse.body?.string() ?: ""
                pullResponse.close()
                return SyncSummary(
                    success = false,
                    errorMessage = "فشل جلب التحديثات من السيرفر: رمز ${pullResponse.code} $err"
                )
            }

            val pullBody = pullResponse.body?.string() ?: "{}"
            pullResponse.close()

            val pullJson = JSONObject(pullBody)
            val serverTimestamp = pullJson.optLong("serverTimestamp", System.currentTimeMillis())
            val serverTrucksArray = pullJson.optJSONArray("trucks") ?: JSONArray()
            val serverModsArray = pullJson.optJSONArray("modifications") ?: JSONArray()

            val internalPhotosDir = File(context.filesDir, "truck_photos").apply { if (!exists()) mkdirs() }
            val publicPhotosDir = AppFolderManager.getPhotosFolder(context)

            // 4. Download any missing photos referenced by server records
            var downloadedPhotosCount = 0
            val allIncomingPhotoNames = mutableSetOf<String>()

            for (i in 0 until serverTrucksArray.length()) {
                val tObj = serverTrucksArray.getJSONObject(i)
                val pStr = tObj.optString("photoPaths", "")
                if (pStr.isNotBlank()) {
                    pStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { allIncomingPhotoNames.add(it) }
                }
            }
            for (i in 0 until serverModsArray.length()) {
                val mObj = serverModsArray.getJSONObject(i)
                val pStr = mObj.optString("photoPaths", "")
                if (pStr.isNotBlank()) {
                    pStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { allIncomingPhotoNames.add(it) }
                }
            }

            for (rawName in allIncomingPhotoNames) {
                val fileName = File(rawName).name
                val localInternal = File(internalPhotosDir, fileName)
                val localPublic = File(publicPhotosDir, fileName)

                if (!localInternal.exists() && !localPublic.exists()) {
                    try {
                        val downloaded = downloadPhotoFile(clean, fileName, localInternal, localPublic, context)
                        if (downloaded) downloadedPhotosCount++
                    } catch (e: Exception) {
                        Log.w(TAG, "Photo download failed for $fileName: ${e.message}")
                    }
                }
            }

            // 5. MERGE incoming trucks into local database
            val incomingTrucks = mutableListOf<TruckEntry>()
            for (i in 0 until serverTrucksArray.length()) {
                val item = serverTrucksArray.getJSONObject(i)
                val tNum = item.optString("truckNumber", "").trim()
                if (tNum.isEmpty()) continue

                val rawPhotos = item.optString("photoPaths", "")
                val localMappedPaths = mapPhotoFileNamesToLocalPaths(rawPhotos, internalPhotosDir)

                incomingTrucks.add(
                    TruckEntry(
                        truckNumber = tNum,
                        driverName = item.optString("driverName", ""),
                        driverPhone = item.optString("driverPhone", ""),
                        hasHeadAndTail = item.optBoolean("hasHeadAndTail", true),
                        headVolume = item.optDouble("headVolume", 0.0),
                        tailVolume = item.optDouble("tailVolume", 0.0),
                        totalVolume = item.optDouble("totalVolume", 0.0),
                        hasExtensions = item.optBoolean("hasExtensions", false),
                        extensionDetails = item.optString("extensionDetails", ""),
                        materialType = item.optString("materialType", ""),
                        siteLocation = item.optString("siteLocation", ""),
                        notes = item.optString("notes", ""),
                        photoPaths = localMappedPaths,
                        createdAt = item.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val incomingMods = mutableListOf<TruckModification>()
            for (i in 0 until serverModsArray.length()) {
                val item = serverModsArray.getJSONObject(i)
                val tNum = item.optString("truckNumber", "").trim()
                if (tNum.isEmpty()) continue

                val rawPhotos = item.optString("photoPaths", "")
                val localMappedPaths = mapPhotoFileNamesToLocalPaths(rawPhotos, internalPhotosDir)

                incomingMods.add(
                    TruckModification(
                        truckNumber = tNum,
                        modificationDate = item.optLong("modificationDate", System.currentTimeMillis()),
                        modificationType = item.optString("modificationType", "تعديل"),
                        previousHeadVolume = item.optDouble("previousHeadVolume", 0.0),
                        previousTailVolume = item.optDouble("previousTailVolume", 0.0),
                        previousTotalVolume = item.optDouble("previousTotalVolume", 0.0),
                        previousHasExtensions = item.optBoolean("previousHasExtensions", false),
                        previousExtensionDetails = item.optString("previousExtensionDetails", ""),
                        newHeadVolume = item.optDouble("newHeadVolume", 0.0),
                        newTailVolume = item.optDouble("newTailVolume", 0.0),
                        newTotalVolume = item.optDouble("newTotalVolume", 0.0),
                        newHasExtensions = item.optBoolean("newHasExtensions", false),
                        newExtensionDetails = item.optString("newExtensionDetails", ""),
                        volumeDifference = item.optDouble("volumeDifference", 0.0),
                        notes = item.optString("notes", ""),
                        photoPaths = localMappedPaths
                    )
                )
            }

            if (incomingTrucks.isNotEmpty()) {
                repository.upsertBackupEntries(incomingTrucks)
            }
            if (incomingMods.isNotEmpty()) {
                repository.upsertBackupModifications(incomingMods)
            }

            // Save local auto-backup snapshot
            ExportManager.saveMasterAutoBackup(context)

            SyncSummary(
                success = true,
                pushedTrucks = localTrucks.size,
                pushedModifications = localMods.size,
                pulledTrucks = incomingTrucks.size,
                pulledModifications = incomingMods.size,
                uploadedPhotos = uploadedPhotosCount,
                downloadedPhotos = downloadedPhotosCount,
                serverTime = serverTimestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            SyncSummary(
                success = false,
                errorMessage = "حدث خطأ أثناء المزامنة: ${e.localizedMessage}"
            )
        }
    }

    private fun extractCleanFileNames(photoPaths: String): String {
        if (photoPaths.isBlank()) return ""
        return photoPaths.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { File(it).name }
            .distinct()
            .joinToString(",")
    }

    private fun mapPhotoFileNamesToLocalPaths(rawPhotos: String, internalDir: File): String {
        if (rawPhotos.isBlank()) return ""
        return rawPhotos.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { name ->
                val fName = File(name).name
                val target = File(internalDir, fName)
                target.absolutePath
            }
            .distinct()
            .joinToString(",")
    }

    private fun uploadPhotoFile(cleanUrl: String, apiKey: String, file: File): Boolean {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "photo",
                file.name,
                file.asRequestBody("image/*".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("$cleanUrl/api/photos/upload")
            .addHeader("x-api-key", apiKey.trim())
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            return response.isSuccessful
        }
    }

    private fun downloadPhotoFile(
        cleanUrl: String,
        fileName: String,
        localInternal: File,
        localPublic: File,
        context: Context
    ): Boolean {
        val request = Request.Builder()
            .url("$cleanUrl/api/photos/$fileName")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return false
            val bytes = response.body?.bytes() ?: return false

            FileOutputStream(localInternal).use { it.write(bytes) }
            try {
                FileOutputStream(localPublic).use { it.write(bytes) }
                AppFolderManager.scanFile(context, localPublic)
            } catch (e: Exception) {
                Log.w(TAG, "Public photo copy warning: ${e.message}")
            }
            return true
        }
    }
}
