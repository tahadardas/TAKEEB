package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.entity.TruckEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RestoreResult(
    val success: Boolean,
    val addedCount: Int = 0,
    val updatedCount: Int = 0,
    val totalCount: Int = 0,
    val errorMessage: String? = null
)

object ExportManager {

    /**
     * Generates a CSV file of all truck entries with UTF-8 BOM for Excel Arabic compatibility
     */
    fun exportToCsv(context: Context, entries: List<TruckEntry>): Uri {
        val dateFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val fileName = "TruckCubic_Report_${fileDateFormat.format(Date())}.csv"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM to prevent Excel Arabic text garbling
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val header = listOf(
                "رقم السيارة",
                "اسم السائق",
                "نوع التكوين",
                "كمية الرأس (م³)",
                "كمية الذنب (م³)",
                "إجمالي التكعيب (م³)",
                "الوصلات",
                "تفاصيل الوصلات",
                "نوع المادة",
                "الموقع / المشروع",
                "ملاحظات",
                "تاريخ التسجيل"
            ).joinToString(",") { escapeCsv(it) } + "\n"

            fos.write(header.toByteArray(Charsets.UTF_8))

            for (entry in entries) {
                val typeStr = if (entry.hasHeadAndTail) "رأس وذنب" else "رأس فقط"
                val extStr = if (entry.hasExtensions) "نعم" else "لا"
                val dateStr = dateFormatter.format(Date(entry.createdAt))

                val row = listOf(
                    entry.truckNumber,
                    entry.driverName,
                    typeStr,
                    String.format(Locale.US, "%.2f", entry.headVolume),
                    if (entry.hasHeadAndTail) String.format(Locale.US, "%.2f", entry.tailVolume) else "0.00",
                    String.format(Locale.US, "%.2f", entry.totalVolume),
                    extStr,
                    entry.extensionDetails,
                    entry.materialType,
                    entry.siteLocation,
                    entry.notes,
                    dateStr
                ).joinToString(",") { escapeCsv(it) } + "\n"

                fos.write(row.toByteArray(Charsets.UTF_8))
            }
        }

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
     */
    fun exportToPdf(context: Context, entries: List<TruckEntry>): Uri {
        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        var pageNumber = 1

        val dateFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val fileName = "TruckCubic_Report_${fileDateFormat.format(Date())}.pdf"
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
        canvas.drawText("تقرير قياس وتكعيب الشاحنات وسيارات النقل", (pageWidth - 30).toFloat(), yPos, titlePaint)
        yPos += 16f
        canvas.drawText("تاريخ الإصدار: ${dateFormatter.format(Date())} | عدد السيارات: ${entries.size}", (pageWidth - 30).toFloat(), yPos, subtitlePaint)
        yPos += 20f

        // Summary KPI Box
        canvas.drawRoundRect(30f, yPos, (pageWidth - 30).toFloat(), yPos + 44f, 8f, 8f, headerBoxPaint)
        val kpiText = "إجمالي التكعيب الكلي: ${String.format(Locale.US, "%.2f", totalVolume)} م³  |  الرؤوس: ${String.format(Locale.US, "%.2f", entries.sumOf { it.headVolume })} م³  |  الأذناب: ${String.format(Locale.US, "%.2f", entries.sumOf { it.tailVolume })} م³"
        canvas.drawText(kpiText, (pageWidth - 45).toFloat(), yPos + 26f, headerTextPaint)
        yPos += 58f

        // Table Column Definitions (Total Width = 535)
        // Col: 1: # (25), 2: Truck No (70), 3: Driver (95), 4: Type (65), 5: Head (50), 6: Tail (50), 7: Total (55), 8: Ext (45), 9: Material (80)
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
     * Creates a full JSON Backup of all truck entries
     */
    fun createBackupJson(context: Context, entries: List<TruckEntry>): Uri {
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        val fileName = "TruckCubic_Backup_${fileDateFormat.format(Date())}.json"
        val file = File(context.cacheDir, fileName)

        val rootJson = JSONObject().apply {
            put("appName", "TruckCubic")
            put("backupVersion", 1)
            put("exportedAt", System.currentTimeMillis())
            put("totalTrucks", entries.size)

            val array = JSONArray()
            for (entry in entries) {
                val item = JSONObject().apply {
                    put("truckNumber", entry.truckNumber)
                    put("driverName", entry.driverName)
                    put("hasHeadAndTail", entry.hasHeadAndTail)
                    put("headVolume", entry.headVolume)
                    put("tailVolume", entry.tailVolume)
                    put("totalVolume", entry.totalVolume)
                    put("hasExtensions", entry.hasExtensions)
                    put("extensionDetails", entry.extensionDetails)
                    put("materialType", entry.materialType)
                    put("siteLocation", entry.siteLocation)
                    put("notes", entry.notes)
                    put("createdAt", entry.createdAt)
                }
                array.put(item)
            }
            put("trucks", array)
        }

        file.writeText(rootJson.toString(2), Charsets.UTF_8)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Parses JSON backup content into a list of TruckEntry objects for merge/upsert
     */
    fun parseBackupJson(jsonString: String): List<TruckEntry> {
        val root = JSONObject(jsonString)
        val array = root.optJSONArray("trucks") ?: JSONArray()
        val list = mutableListOf<TruckEntry>()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val truckNumber = item.optString("truckNumber", "").trim()
            if (truckNumber.isEmpty()) continue

            val entry = TruckEntry(
                truckNumber = truckNumber,
                driverName = item.optString("driverName", ""),
                hasHeadAndTail = item.optBoolean("hasHeadAndTail", true),
                headVolume = item.optDouble("headVolume", 0.0),
                tailVolume = item.optDouble("tailVolume", 0.0),
                totalVolume = item.optDouble("totalVolume", 0.0),
                hasExtensions = item.optBoolean("hasExtensions", false),
                extensionDetails = item.optString("extensionDetails", ""),
                materialType = item.optString("materialType", ""),
                siteLocation = item.optString("siteLocation", ""),
                notes = item.optString("notes", ""),
                createdAt = item.optLong("createdAt", System.currentTimeMillis())
            )
            list.add(entry)
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
