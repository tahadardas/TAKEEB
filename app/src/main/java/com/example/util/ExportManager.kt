package com.example.util

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.TruckEntry
import com.example.data.local.entity.TruckModification
import com.example.data.repository.TruckRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class RestoreResult(
    val success: Boolean,
    val addedCount: Int = 0,
    val updatedCount: Int = 0,
    val totalCount: Int = 0,
    val restoredPhotosCount: Int = 0,
    val errorMessage: String? = null
)

object ExportManager {

    private const val TAG = "ExportManager"

    /**
     * Generates a CSV file of all truck entries with UTF-8 BOM for Excel Arabic compatibility
     * Also saves a copy in the dedicated folder: تكعيب الغضب/ملفات_البرنامج
     */
    fun exportToCsv(context: Context, entries: List<TruckEntry>): Uri {
        val dateFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val fileName = "TakeebGhadab_Report_${fileDateFormat.format(Date())}.csv"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM to prevent Excel Arabic text garbling
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val header = listOf(
                "رقم السيارة",
                "اسم السائق",
                "رقم هاتف السائق",
                "نوع التكوين",
                "كمية الرأس (م³)",
                "كمية الذنب (م³)",
                "إجمالي التكعيب (م³)",
                "الوصلات",
                "تفاصيل الوصلات",
                "نوع المادة",
                "الموقع / المشروع",
                "ملاحظات",
                "عدد الصور المرفقة",
                "تاريخ التسجيل"
            ).joinToString(",") { escapeCsv(it) } + "\n"

            fos.write(header.toByteArray(Charsets.UTF_8))

            for (entry in entries) {
                val typeStr = if (entry.hasHeadAndTail) "رأس وذنب" else "رأس فقط"
                val extStr = if (entry.hasExtensions) "نعم" else "لا"
                val dateStr = dateFormatter.format(Date(entry.createdAt))
                val photoCount = entry.getPhotoList().size

                val row = listOf(
                    entry.truckNumber,
                    entry.driverName,
                    entry.driverPhone,
                    typeStr,
                    String.format(Locale.US, "%.2f", entry.headVolume),
                    if (entry.hasHeadAndTail) String.format(Locale.US, "%.2f", entry.tailVolume) else "0.00",
                    String.format(Locale.US, "%.2f", entry.totalVolume),
                    extStr,
                    entry.extensionDetails,
                    entry.materialType,
                    entry.siteLocation,
                    entry.notes,
                    "$photoCount",
                    dateStr
                ).joinToString(",") { escapeCsv(it) } + "\n"

                fos.write(row.toByteArray(Charsets.UTF_8))
            }
        }

        // Copy to public folder
        AppFolderManager.copyFileToAppFiles(context, file)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }

    /**
     * Generates a high-quality PDF Report using Android PdfDocument
     * Also saves a copy in the dedicated folder: تكعيب الغضب/ملفات_البرنامج
     */
    fun exportToPdf(context: Context, entries: List<TruckEntry>): Uri {
        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        var pageNumber = 1

        val dateFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val fileName = "TakeebGhadab_Report_${fileDateFormat.format(Date())}.pdf"
        val file = File(context.cacheDir, fileName)

        val totalVolume = entries.sumOf { it.totalVolume }

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }

        val headerBoxPaint = Paint().apply {
            color = Color.rgb(254, 243, 199) // Amber light container
            style = Paint.Style.FILL
        }

        val headerTextPaint = Paint().apply {
            color = Color.rgb(120, 53, 15) // Amber dark
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(30, 41, 59) // Slate 800
            style = Paint.Style.FILL
        }

        val tableHeaderTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val cellTextPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }

        val rowEvenPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val rowOddPaint = Paint().apply {
            color = Color.rgb(248, 250, 252) // Slate 50
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }

        // Draw Cover / Page 1 Header
        var yPos = 40f
        canvas.drawText("تقرير قياس وتكعيب الشاحنات وسيارات النقل - تكعيب الغضب", (pageWidth - 30).toFloat(), yPos, titlePaint)
        yPos += 16f
        canvas.drawText("تاريخ الإصدار: ${dateFormatter.format(Date())} | عدد السيارات: ${entries.size} | هاتف: 0987080330", (pageWidth - 30).toFloat(), yPos, subtitlePaint)
        yPos += 20f

        // Summary KPI Box
        canvas.drawRoundRect(30f, yPos, (pageWidth - 30).toFloat(), yPos + 44f, 8f, 8f, headerBoxPaint)
        val kpiText = "إجمالي التكعيب الكلي: ${String.format(Locale.US, "%.2f", totalVolume)} م³  |  الرؤوس: ${String.format(Locale.US, "%.2f", entries.sumOf { it.headVolume })} م³  |  الأذناب: ${String.format(Locale.US, "%.2f", entries.sumOf { it.tailVolume })} م³"
        canvas.drawText(kpiText, (pageWidth - 45).toFloat(), yPos + 26f, headerTextPaint)
        yPos += 58f

        // Table Column Definitions (Total Width = 535)
        val colWidths = floatArrayOf(25f, 75f, 95f, 65f, 45f, 45f, 55f, 50f, 80f)
        val colHeaders = arrayOf("#", "رقم السيارة", "اسم السائق", "التكوين", "رأس", "ذنب", "الإجمالي", "وصلات", "المادة")
        val startX = 30f
        val tableWidth = (pageWidth - 60).toFloat()

        fun drawTableHeader(c: Canvas, curY: Float) {
            c.drawRect(startX, curY, startX + tableWidth, curY + 22f, tableHeaderPaint)
            var currentX = startX
            for (i in colHeaders.indices) {
                val cx = currentX + colWidths[i] / 2f
                c.drawText(colHeaders[i], cx, curY + 15f, tableHeaderTextPaint)
                currentX += colWidths[i]
            }
        }

        drawTableHeader(canvas, yPos)
        yPos += 22f

        for (index in entries.indices) {
            val entry = entries[index]

            // Check if page break is needed
            if (yPos > pageHeight - 50) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = 40f
                drawTableHeader(canvas, yPos)
                yPos += 22f
            }

            val rowBg = if (index % 2 == 0) rowEvenPaint else rowOddPaint
            canvas.drawRect(startX, yPos, startX + tableWidth, yPos + 20f, rowBg)
            canvas.drawRect(startX, yPos, startX + tableWidth, yPos + 20f, borderPaint)

            var currentX = startX
            val rowValues = arrayOf(
                "${index + 1}",
                entry.truckNumber,
                truncate(entry.driverName, 14),
                if (entry.hasHeadAndTail) "رأس وذنب" else "رأس فقط",
                String.format(Locale.US, "%.1f", entry.headVolume),
                if (entry.hasHeadAndTail) String.format(Locale.US, "%.1f", entry.tailVolume) else "-",
                String.format(Locale.US, "%.2f", entry.totalVolume),
                if (entry.hasExtensions) "نعم" else "لا",
                truncate(entry.materialType.ifBlank { "-" }, 12)
            )

            for (i in rowValues.indices) {
                val cx = currentX + colWidths[i] / 2f
                canvas.drawText(rowValues[i], cx, yPos + 14f, cellTextPaint)
                currentX += colWidths[i]
            }

            yPos += 20f
        }

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        // Copy to public folder
        AppFolderManager.copyFileToAppFiles(context, file)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun truncate(text: String, maxLen: Int): String {
        return if (text.length > maxLen) text.substring(0, maxLen - 1) + ".." else text
    }

    /**
     * Builds the JSON representation of truck entries and modifications
     */
    fun buildBackupJsonObject(
        entries: List<TruckEntry>,
        modifications: List<TruckModification> = emptyList()
    ): JSONObject {
        return JSONObject().apply {
            put("appName", "تكعيب الغضب")
            put("phone", "0987080330")
            put("backupVersion", 3)
            put("exportedAt", System.currentTimeMillis())
            put("totalTrucks", entries.size)
            put("totalModifications", modifications.size)

            val array = JSONArray()
            for (entry in entries) {
                val item = JSONObject().apply {
                    put("truckNumber", entry.truckNumber)
                    put("driverName", entry.driverName)
                    put("driverPhone", entry.driverPhone)
                    put("hasHeadAndTail", entry.hasHeadAndTail)
                    put("headVolume", entry.headVolume)
                    put("tailVolume", entry.tailVolume)
                    put("totalVolume", entry.totalVolume)
                    put("hasExtensions", entry.hasExtensions)
                    put("extensionDetails", entry.extensionDetails)
                    put("materialType", entry.materialType)
                    put("siteLocation", entry.siteLocation)
                    put("notes", entry.notes)
                    put("photoPaths", entry.photoPaths)
                    put("createdAt", entry.createdAt)

                    // Also store relative photo file names for reliable cross-device reconstruction
                    val photoNames = JSONArray()
                    entry.getPhotoList().forEach { path ->
                        val f = File(path)
                        photoNames.put(f.name)
                    }
                    put("photoFileNames", photoNames)
                }
                array.put(item)
            }
            put("trucks", array)

            // Include modification history records
            val modArray = JSONArray()
            for (mod in modifications) {
                val modItem = JSONObject().apply {
                    put("truckNumber", mod.truckNumber)
                    put("modificationDate", mod.modificationDate)
                    put("modificationType", mod.modificationType)
                    put("previousHeadVolume", mod.previousHeadVolume)
                    put("previousTailVolume", mod.previousTailVolume)
                    put("previousTotalVolume", mod.previousTotalVolume)
                    put("previousHasExtensions", mod.previousHasExtensions)
                    put("previousExtensionDetails", mod.previousExtensionDetails)
                    put("newHeadVolume", mod.newHeadVolume)
                    put("newTailVolume", mod.newTailVolume)
                    put("newTotalVolume", mod.newTotalVolume)
                    put("newHasExtensions", mod.newHasExtensions)
                    put("newExtensionDetails", mod.newExtensionDetails)
                    put("volumeDifference", mod.volumeDifference)
                    put("notes", mod.notes)
                    put("photoPaths", mod.photoPaths)

                    val modPhotoNames = JSONArray()
                    mod.getPhotoList().forEach { path ->
                        val f = File(path)
                        modPhotoNames.put(f.name)
                    }
                    put("photoFileNames", modPhotoNames)
                }
                modArray.put(modItem)
            }
            put("modifications", modArray)
        }
    }

    /**
     * Creates a COMPRESSED ZIP BACKUP containing:
     * - backup.json (Truck database records, modification history, and metadata)
     * - photos/ (All referenced camera photo files for trucks and modifications)
     * Also saves a copy in the dedicated folder: تكعيب الغضب/ملفات_البرنامج
     */
    fun createCompressedBackupZip(
        context: Context,
        entries: List<TruckEntry>,
        modifications: List<TruckModification> = emptyList()
    ): Uri {
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val fileName = "TakeebGhadab_Backup_${fileDateFormat.format(Date())}.zip"
        val zipFile = File(context.cacheDir, fileName)

        val jsonObject = buildBackupJsonObject(entries, modifications)
        val jsonBytes = jsonObject.toString(2).toByteArray(Charsets.UTF_8)

        // Find all unique existing photo files referenced in entries and modifications
        val uniquePhotoFiles = mutableMapOf<String, File>()
        val allPhotoPaths = entries.flatMap { it.getPhotoList() } + modifications.flatMap { it.getPhotoList() }

        for (path in allPhotoPaths) {
            val f = File(path)
            if (f.exists() && f.isFile) {
                uniquePhotoFiles[f.name] = f
            } else {
                // Check if file exists in public photos folder
                val publicFolder = AppFolderManager.getPhotosFolder(context)
                val publicFile = File(publicFolder, f.name)
                if (publicFile.exists() && publicFile.isFile) {
                    uniquePhotoFiles[publicFile.name] = publicFile
                }
            }
        }

        FileOutputStream(zipFile).use { fos ->
            BufferedOutputStream(fos).use { bos ->
                ZipOutputStream(bos).use { zos ->
                    // Set compression method
                    zos.setLevel(java.util.zip.Deflater.BEST_COMPRESSION)

                    // 1. Add backup.json entry
                    val jsonEntry = ZipEntry("backup.json")
                    zos.putNextEntry(jsonEntry)
                    zos.write(jsonBytes)
                    zos.closeEntry()

                    // 2. Add each photo file under photos/
                    val buffer = ByteArray(8192)
                    for ((photoName, file) in uniquePhotoFiles) {
                        try {
                            val photoEntry = ZipEntry("photos/$photoName")
                            zos.putNextEntry(photoEntry)
                            FileInputStream(file).use { fis ->
                                var bytesRead: Int
                                while (fis.read(buffer).also { bytesRead = it } != -1) {
                                    zos.write(buffer, 0, bytesRead)
                                }
                            }
                            zos.closeEntry()
                        } catch (e: Exception) {
                            Log.w(TAG, "Error adding photo $photoName to zip: ${e.message}")
                        }
                    }
                }
            }
        }

        // Copy backup to the user's dedicated folder in My Files ("ملفاتي")
        AppFolderManager.copyFileToAppFiles(context, zipFile)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )
    }

    /**
     * Legacy uncompressed JSON backup creation (backward compatibility)
     */
    fun createBackupJson(
        context: Context,
        entries: List<TruckEntry>,
        modifications: List<TruckModification> = emptyList()
    ): Uri {
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        val fileName = "TakeebGhadab_Backup_${fileDateFormat.format(Date())}.json"
        val file = File(context.cacheDir, fileName)

        val rootJson = buildBackupJsonObject(entries, modifications)
        file.writeText(rootJson.toString(2), Charsets.UTF_8)

        AppFolderManager.copyFileToAppFiles(context, file)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Exports the native SQLite database file directly (truck_cubic_database.sqlite):
     * Runs WAL checkpoint to ensure all uncommitted data in WAL is flushed to the main file,
     * copies it into cacheDir with timestamp, saves a master copy in the dedicated folder:
     * تكعيب الغضب/ملفات_البرنامج/TakeebGhadab_Database_[timestamp].sqlite
     * and returns the FileProvider Uri for instant sharing via WhatsApp, Drive, etc.
     */
    fun exportSqliteDatabase(context: Context): Uri {
        try {
            val db = AppDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { c ->
                c.moveToFirst()
            }
        } catch (e: Exception) {
            Log.w(TAG, "WAL checkpoint warning: ${e.message}")
        }

        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val fileName = "TakeebGhadab_Database_${fileDateFormat.format(Date())}.sqlite"
        val exportFile = File(context.cacheDir, fileName)

        FileInputStream(dbFile).use { input ->
            FileOutputStream(exportFile).use { output ->
                input.copyTo(output)
            }
        }

        // Copy to dedicated public folder: تكعيب الغضب/ملفات_البرنامج
        AppFolderManager.copyFileToAppFiles(context, exportFile)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )
    }

    const val MASTER_SQLITE_FILENAME = "قاعدة_بيانات_السيارات_المعتمدة.sqlite"

    /**
     * Automatically saves a synchronized master copy of the SQLite database to:
     * تكعيب الغضب/ملفات_البرنامج/قاعدة_بيانات_السيارات_المعتمدة.sqlite
     * This protects user data from being wiped during updates or reinstalls.
     */
    fun saveMasterAutoBackup(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            try {
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { c ->
                    c.moveToFirst()
                }
            } catch (e: Exception) {
                Log.w(TAG, "WAL checkpoint warning: ${e.message}")
            }

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            if (!dbFile.exists() || dbFile.length() == 0L) return

            val targetDir = AppFolderManager.getAppFilesFolder(context)
            val masterFile = File(targetDir, MASTER_SQLITE_FILENAME)

            FileInputStream(dbFile).use { input ->
                FileOutputStream(masterFile).use { output ->
                    input.copyTo(output)
                }
            }
            AppFolderManager.scanFile(context, masterFile)
            Log.d(TAG, "Saved master auto backup to ${masterFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save master auto backup: ${e.message}")
        }
    }

    /**
     * Finds the master SQLite backup or latest valid backup (.sqlite or .zip)
     * in the public folder تكعيب الغضب/ملفات_البرنامج
     */
    fun findMasterOrLatestBackup(context: Context): File? {
        return try {
            val appFilesDir = AppFolderManager.getAppFilesFolder(context)
            if (!appFilesDir.exists()) return null

            val masterFile = File(appFilesDir, MASTER_SQLITE_FILENAME)
            if (masterFile.exists() && masterFile.length() > 0L) {
                return masterFile
            }

            val files = appFilesDir.listFiles() ?: return null
            files
                .filter { it.isFile && (it.name.endsWith(".sqlite") || it.name.endsWith(".zip") || it.name.endsWith(".db")) }
                .maxByOrNull { it.lastModified() }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding backup file: ${e.message}")
            null
        }
    }

    /**
     * Unified restore function:
     * Automatically detects whether the selected file is:
     * 1. A native SQLite database (.sqlite / .db)
     * 2. A compressed ZIP archive (.zip) with photos
     * 3. A JSON backup (.json)
     * extracts photos (if ZIP), and updates/merges database records without deleting anything.
     */
    suspend fun restoreBackup(
        context: Context,
        fileUri: Uri,
        repository: TruckRepository
    ): RestoreResult {
        return when {
            isZipFile(context, fileUri) -> restoreFromZip(context, fileUri, repository)
            isSqliteFile(context, fileUri) -> restoreFromSqlite(context, fileUri, repository)
            else -> restoreFromJson(context, fileUri, repository)
        }
    }

    private fun isZipFile(context: Context, uri: Uri): Boolean {
        val path = uri.path?.lowercase(Locale.ROOT) ?: ""
        if (path.endsWith(".zip")) return true

        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(4)
                val read = stream.read(header)
                if (read >= 4) {
                    header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                            (header[2] == 0x03.toByte() || header[2] == 0x05.toByte() || header[2] == 0x07.toByte())
                } else false
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun isSqliteFile(context: Context, uri: Uri): Boolean {
        val path = uri.path?.lowercase(Locale.ROOT) ?: ""
        if (path.endsWith(".sqlite") || path.endsWith(".db") || path.endsWith(".sqlite3")) {
            return true
        }

        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(16)
                val read = stream.read(header)
                if (read == 16) {
                    val magic = String(header, Charsets.US_ASCII)
                    magic.startsWith("SQLite format 3")
                } else false
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Restores records directly from a native SQLite database file:
     * Reads 'truck_entries' and 'truck_modifications' tables,
     * merges and updates existing records, adds new records.
     */
    private suspend fun restoreFromSqlite(
        context: Context,
        fileUri: Uri,
        repository: TruckRepository
    ): RestoreResult {
        val tempDbFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.sqlite")
        return try {
            context.contentResolver.openInputStream(fileUri)?.use { input ->
                FileOutputStream(tempDbFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return RestoreResult(success = false, errorMessage = "تعذر قراءة ملف قاعدة بيانات SQLite المحدد")

            var addedCount = 0
            var updatedCount = 0

            val sqlite = SQLiteDatabase.openDatabase(
                tempDbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            sqlite.use { db ->
                // 1. Process truck_entries table
                val hasEntriesTable = db.rawQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='truck_entries'",
                    null
                ).use { c ->
                    c.moveToFirst() && c.getInt(0) > 0
                }

                if (hasEntriesTable) {
                    val cursor = db.rawQuery("SELECT * FROM truck_entries", null)
                    cursor.use { c ->
                        val colTruckNumber = c.getColumnIndex("truckNumber")
                        val colDriverName = c.getColumnIndex("driverName")
                        val colDriverPhone = c.getColumnIndex("driverPhone")
                        val colHasHeadAndTail = c.getColumnIndex("hasHeadAndTail")
                        val colHeadVolume = c.getColumnIndex("headVolume")
                        val colTailVolume = c.getColumnIndex("tailVolume")
                        val colTotalVolume = c.getColumnIndex("totalVolume")
                        val colHasExtensions = c.getColumnIndex("hasExtensions")
                        val colExtensionDetails = c.getColumnIndex("extensionDetails")
                        val colMaterialType = c.getColumnIndex("materialType")
                        val colSiteLocation = c.getColumnIndex("siteLocation")
                        val colNotes = c.getColumnIndex("notes")
                        val colPhotoPaths = c.getColumnIndex("photoPaths")
                        val colCreatedAt = c.getColumnIndex("createdAt")

                        while (c.moveToNext()) {
                            val tNumber = if (colTruckNumber != -1) c.getString(colTruckNumber) ?: "" else ""
                            if (tNumber.isBlank()) continue

                            val dName = if (colDriverName != -1) c.getString(colDriverName) ?: "" else ""
                            val dPhone = if (colDriverPhone != -1) c.getString(colDriverPhone) ?: "" else ""
                            val hasBoth = if (colHasHeadAndTail != -1) c.getInt(colHasHeadAndTail) == 1 else true
                            val hVol = if (colHeadVolume != -1) c.getDouble(colHeadVolume) else 0.0
                            val tVol = if (colTailVolume != -1) c.getDouble(colTailVolume) else 0.0
                            val totVol = if (colTotalVolume != -1) c.getDouble(colTotalVolume) else (hVol + tVol)
                            val hasExt = if (colHasExtensions != -1) c.getInt(colHasExtensions) == 1 else false
                            val extDet = if (colExtensionDetails != -1) c.getString(colExtensionDetails) ?: "" else ""
                            val matType = if (colMaterialType != -1) c.getString(colMaterialType) ?: "" else ""
                            val siteLoc = if (colSiteLocation != -1) c.getString(colSiteLocation) ?: "" else ""
                            val notesVal = if (colNotes != -1) c.getString(colNotes) ?: "" else ""
                            val photosVal = if (colPhotoPaths != -1) c.getString(colPhotoPaths) ?: "" else ""
                            val created = if (colCreatedAt != -1) c.getLong(colCreatedAt) else System.currentTimeMillis()

                            val existing = repository.getEntryByTruckNumber(tNumber)
                            if (existing != null) {
                                val existingPhotos = existing.getPhotoList()
                                val importedPhotos = if (photosVal.isNotBlank()) photosVal.split(",") else emptyList()
                                val mergedPhotos = (existingPhotos + importedPhotos).distinct()

                                val updated = existing.copy(
                                    driverName = if (dName.isNotBlank()) dName else existing.driverName,
                                    driverPhone = if (dPhone.isNotBlank()) dPhone else existing.driverPhone,
                                    hasHeadAndTail = hasBoth,
                                    headVolume = hVol,
                                    tailVolume = tVol,
                                    totalVolume = totVol,
                                    hasExtensions = hasExt,
                                    extensionDetails = if (extDet.isNotBlank()) extDet else existing.extensionDetails,
                                    materialType = if (matType.isNotBlank()) matType else existing.materialType,
                                    siteLocation = if (siteLoc.isNotBlank()) siteLoc else existing.siteLocation,
                                    notes = if (notesVal.isNotBlank()) notesVal else existing.notes,
                                    photoPaths = mergedPhotos.joinToString(",")
                                )
                                repository.update(updated)
                                updatedCount++
                            } else {
                                val newEntry = TruckEntry(
                                    truckNumber = tNumber,
                                    driverName = dName,
                                    driverPhone = dPhone,
                                    hasHeadAndTail = hasBoth,
                                    headVolume = hVol,
                                    tailVolume = tVol,
                                    totalVolume = totVol,
                                    hasExtensions = hasExt,
                                    extensionDetails = extDet,
                                    materialType = matType,
                                    siteLocation = siteLoc,
                                    notes = notesVal,
                                    photoPaths = photosVal,
                                    createdAt = created
                                )
                                repository.insert(newEntry)
                                addedCount++
                            }
                        }
                    }
                }

                // 2. Process truck_modifications table
                val hasModsTable = db.rawQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='truck_modifications'",
                    null
                ).use { c ->
                    c.moveToFirst() && c.getInt(0) > 0
                }

                if (hasModsTable) {
                    val modCursor = db.rawQuery("SELECT * FROM truck_modifications", null)
                    modCursor.use { c ->
                        val colTruckNumber = c.getColumnIndex("truckNumber")
                        val colModType = c.getColumnIndex("modificationType")
                        val colModDate = c.getColumnIndex("modificationDate")
                        val colPrevHead = c.getColumnIndex("previousHeadVolume")
                        val colPrevTail = c.getColumnIndex("previousTailVolume")
                        val colPrevTotal = c.getColumnIndex("previousTotalVolume")
                        val colPrevHasExt = c.getColumnIndex("previousHasExtensions")
                        val colPrevExtDet = c.getColumnIndex("previousExtensionDetails")
                        val colNewHead = c.getColumnIndex("newHeadVolume")
                        val colNewTail = c.getColumnIndex("newTailVolume")
                        val colNewTotal = c.getColumnIndex("newTotalVolume")
                        val colNewHasExt = c.getColumnIndex("newHasExtensions")
                        val colNewExtDet = c.getColumnIndex("newExtensionDetails")
                        val colVolDiff = c.getColumnIndex("volumeDifference")
                        val colNotes = c.getColumnIndex("notes")
                        val colPhotos = c.getColumnIndex("photoPaths")

                        while (c.moveToNext()) {
                            val tNumber = if (colTruckNumber != -1) c.getString(colTruckNumber) ?: "" else ""
                            if (tNumber.isBlank()) continue

                            val modType = if (colModType != -1) c.getString(colModType) ?: "" else "تعديل"
                            val modDate = if (colModDate != -1) c.getLong(colModDate) else System.currentTimeMillis()

                            val newMod = TruckModification(
                                truckNumber = tNumber,
                                modificationType = modType,
                                modificationDate = modDate,
                                previousHeadVolume = if (colPrevHead != -1) c.getDouble(colPrevHead) else 0.0,
                                previousTailVolume = if (colPrevTail != -1) c.getDouble(colPrevTail) else 0.0,
                                previousTotalVolume = if (colPrevTotal != -1) c.getDouble(colPrevTotal) else 0.0,
                                previousHasExtensions = if (colPrevHasExt != -1) c.getInt(colPrevHasExt) == 1 else false,
                                previousExtensionDetails = if (colPrevExtDet != -1) c.getString(colPrevExtDet) ?: "" else "",
                                newHeadVolume = if (colNewHead != -1) c.getDouble(colNewHead) else 0.0,
                                newTailVolume = if (colNewTail != -1) c.getDouble(colNewTail) else 0.0,
                                newTotalVolume = if (colNewTotal != -1) c.getDouble(colNewTotal) else 0.0,
                                newHasExtensions = if (colNewHasExt != -1) c.getInt(colNewHasExt) == 1 else false,
                                newExtensionDetails = if (colNewExtDet != -1) c.getString(colNewExtDet) ?: "" else "",
                                volumeDifference = if (colVolDiff != -1) c.getDouble(colVolDiff) else 0.0,
                                notes = if (colNotes != -1) c.getString(colNotes) ?: "" else "",
                                photoPaths = if (colPhotos != -1) c.getString(colPhotos) ?: "" else ""
                            )
                            repository.insertModification(newMod)
                        }
                    }
                }
            }

            RestoreResult(
                success = true,
                addedCount = addedCount,
                updatedCount = updatedCount,
                totalCount = addedCount + updatedCount,
                restoredPhotosCount = 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring SQLite database: ${e.message}", e)
            RestoreResult(
                success = false,
                errorMessage = "فشلت استعادة قاعدة بيانات SQLite: ${e.localizedMessage}"
            )
        } finally {
            tempDbFile.delete()
        }
    }

    /**
     * Restores from a compressed ZIP backup:
     * - Unpacks photos into internal truck_photos AND public folder تكعيب الغضب/صور_السيارات
     * - Parses backup.json
     * - Remaps photo paths to current device's storage
     * - Merges / updates database records
     */
    private suspend fun restoreFromZip(
        context: Context,
        zipUri: Uri,
        repository: TruckRepository
    ): RestoreResult {
        val localPhotoDir = File(context.filesDir, "truck_photos")
        if (!localPhotoDir.exists()) localPhotoDir.mkdirs()

        val publicPhotoDir = AppFolderManager.getPhotosFolder(context)

        val restoredPhotoMap = mutableMapOf<String, String>()
        var backupJsonString: String? = null
        var photosCount = 0

        context.contentResolver.openInputStream(zipUri)?.use { rawStream ->
            BufferedInputStream(rawStream).use { bis ->
                ZipInputStream(bis).use { zis ->
                    val buffer = ByteArray(8192)
                    var entry = zis.nextEntry

                    while (entry != null) {
                        val entryName = entry.name
                        val fileName = File(entryName).name

                        if (!entry.isDirectory && fileName.isNotBlank()) {
                            if (fileName.equals("backup.json", ignoreCase = true) || fileName.endsWith(".json", ignoreCase = true)) {
                                val baos = ByteArrayOutputStream()
                                var len: Int
                                while (zis.read(buffer).also { len = it } != -1) {
                                    baos.write(buffer, 0, len)
                                }
                                backupJsonString = baos.toString(Charsets.UTF_8.name())
                            } else if (isImageFile(fileName)) {
                                // Extract photo into local internal directory
                                val destLocalFile = File(localPhotoDir, fileName)
                                FileOutputStream(destLocalFile).use { fos ->
                                    var len: Int
                                    while (zis.read(buffer).also { len = it } != -1) {
                                        fos.write(buffer, 0, len)
                                    }
                                }

                                // Also copy photo to public folder
                                try {
                                    val destPublicFile = File(publicPhotoDir, fileName)
                                    destLocalFile.copyTo(destPublicFile, overwrite = true)
                                    AppFolderManager.scanFile(context, destPublicFile)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Could not copy photo to public dir: ${e.message}")
                                }

                                restoredPhotoMap[fileName] = destLocalFile.absolutePath
                                photosCount++
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } ?: return RestoreResult(false, errorMessage = "تعذر فتح ملف النسخة الاحتياطية")

        if (backupJsonString.isNullOrBlank()) {
            return RestoreResult(false, errorMessage = "الملف المضغوط لا يحتوي على بيانات backup.json صالحة")
        }

        val entries = parseBackupJson(backupJsonString!!, restoredPhotoMap, localPhotoDir)
        if (entries.isEmpty()) {
            return RestoreResult(false, errorMessage = "لم يتم العثور على سجلات سيارات صالحة في النسخة الاحتياطية")
        }

        val repoResult = repository.upsertBackupEntries(entries)

        // Also restore modification history if present in backup
        val modifications = parseBackupModificationsJson(backupJsonString!!, restoredPhotoMap, localPhotoDir)
        if (modifications.isNotEmpty()) {
            repository.upsertBackupModifications(modifications)
        }

        return RestoreResult(
            success = true,
            addedCount = repoResult.addedCount,
            updatedCount = repoResult.updatedCount,
            totalCount = repoResult.totalCount,
            restoredPhotosCount = photosCount
        )
    }

    /**
     * Restores from legacy JSON backup
     */
    private suspend fun restoreFromJson(
        context: Context,
        fileUri: Uri,
        repository: TruckRepository
    ): RestoreResult {
        val jsonContent = context.contentResolver.openInputStream(fileUri)?.bufferedReader(Charsets.UTF_8)?.use {
            it.readText()
        } ?: return RestoreResult(false, errorMessage = "تعذر قراءة ملف النسخة الاحتياطية")

        val entries = parseBackupJson(jsonContent)
        if (entries.isEmpty()) {
            return RestoreResult(false, errorMessage = "الملف لا يحتوي على بيانات شاحنات صالحة")
        }

        val repoResult = repository.upsertBackupEntries(entries)

        // Also restore modification history if present in backup
        val modifications = parseBackupModificationsJson(jsonContent)
        if (modifications.isNotEmpty()) {
            repository.upsertBackupModifications(modifications)
        }

        return RestoreResult(
            success = true,
            addedCount = repoResult.addedCount,
            updatedCount = repoResult.updatedCount,
            totalCount = repoResult.totalCount,
            restoredPhotosCount = 0
        )
    }

    private fun isImageFile(fileName: String): Boolean {
        val lower = fileName.lowercase(Locale.ROOT)
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }

    /**
     * Parses JSON backup content into a list of TruckEntry objects for merge/upsert.
     * Optionally remaps photo paths using restoredPhotoMap.
     */
    fun parseBackupJson(
        jsonString: String,
        restoredPhotoMap: Map<String, String> = emptyMap(),
        fallbackPhotoDir: File? = null
    ): List<TruckEntry> {
        val root = JSONObject(jsonString)
        val array = root.optJSONArray("trucks") ?: JSONArray()
        val list = mutableListOf<TruckEntry>()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val truckNumber = item.optString("truckNumber", "").trim()
            if (truckNumber.isEmpty()) continue

            // Reconstruct photo paths for this truck
            val mappedPhotos = mutableListOf<String>()

            // 1. Check photoFileNames array
            val photoNamesArr = item.optJSONArray("photoFileNames")
            if (photoNamesArr != null && photoNamesArr.length() > 0) {
                for (j in 0 until photoNamesArr.length()) {
                    val name = photoNamesArr.optString(j, "").trim()
                    if (name.isNotEmpty()) {
                        val path = restoredPhotoMap[name]
                            ?: (if (fallbackPhotoDir != null) File(fallbackPhotoDir, name).takeIf { it.exists() }?.absolutePath else null)
                            ?: (fallbackPhotoDir?.let { File(it, name).absolutePath })
                        if (path != null) {
                            mappedPhotos.add(path)
                        }
                    }
                }
            }

            // 2. Check legacy photoPaths if no photoFileNames found
            if (mappedPhotos.isEmpty()) {
                val legacyPaths = item.optString("photoPaths", "").trim()
                if (legacyPaths.isNotEmpty()) {
                    val split = legacyPaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    for (oldPath in split) {
                        val fName = File(oldPath).name
                        val path = restoredPhotoMap[fName]
                            ?: if (File(oldPath).exists()) oldPath
                            else fallbackPhotoDir?.let { File(it, fName).takeIf { f -> f.exists() }?.absolutePath }
                            ?: oldPath
                        mappedPhotos.add(path)
                    }
                }
            }

            val finalPhotoPaths = mappedPhotos.distinct().joinToString(",")

            val entry = TruckEntry(
                truckNumber = truckNumber,
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
                photoPaths = finalPhotoPaths,
                createdAt = item.optLong("createdAt", System.currentTimeMillis())
            )
            list.add(entry)
        }

        return list
    }

    /**
     * Parses JSON backup content into a list of TruckModification records
     */
    fun parseBackupModificationsJson(
        jsonString: String,
        restoredPhotoMap: Map<String, String> = emptyMap(),
        fallbackPhotoDir: File? = null
    ): List<TruckModification> {
        val root = try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            return emptyList()
        }
        val array = root.optJSONArray("modifications") ?: return emptyList()
        val list = mutableListOf<TruckModification>()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val truckNumber = item.optString("truckNumber", "").trim()
            if (truckNumber.isEmpty()) continue

            // Reconstruct photo paths for this modification
            val mappedPhotos = mutableListOf<String>()

            val photoNamesArr = item.optJSONArray("photoFileNames")
            if (photoNamesArr != null && photoNamesArr.length() > 0) {
                for (j in 0 until photoNamesArr.length()) {
                    val name = photoNamesArr.optString(j, "").trim()
                    if (name.isNotEmpty()) {
                        val path = restoredPhotoMap[name]
                            ?: (if (fallbackPhotoDir != null) File(fallbackPhotoDir, name).takeIf { it.exists() }?.absolutePath else null)
                            ?: (fallbackPhotoDir?.let { File(it, name).absolutePath })
                        if (path != null) {
                            mappedPhotos.add(path)
                        }
                    }
                }
            }

            if (mappedPhotos.isEmpty()) {
                val legacyPaths = item.optString("photoPaths", "").trim()
                if (legacyPaths.isNotEmpty()) {
                    val split = legacyPaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    for (oldPath in split) {
                        val fName = File(oldPath).name
                        val path = restoredPhotoMap[fName]
                            ?: if (File(oldPath).exists()) oldPath
                            else fallbackPhotoDir?.let { File(it, fName).takeIf { f -> f.exists() }?.absolutePath }
                            ?: oldPath
                        mappedPhotos.add(path)
                    }
                }
            }

            val finalPhotoPaths = mappedPhotos.distinct().joinToString(",")

            val mod = TruckModification(
                truckNumber = truckNumber,
                modificationDate = item.optLong("modificationDate", System.currentTimeMillis()),
                modificationType = item.optString("modificationType", "تعديل القياسات"),
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
                photoPaths = finalPhotoPaths
            )
            list.add(mod)
        }
        return list
    }

    /**
     * Helper to share any file URI with WhatsApp or Android chooser
     */
    fun shareFile(context: Context, fileUri: Uri, mimeType: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
