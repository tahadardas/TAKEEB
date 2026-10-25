package com.example.util

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Manages the dedicated public storage folder requested by the user: "فشنثثلالاشيشلا"
 * located in user storage ("ملفاتي" / Documents), containing:
 * - ملفات_البرنامج (App files: backups, reports, exports)
 * - صور_السيارات (Truck documentation camera photos)
 */
object AppFolderManager {

    const val MAIN_FOLDER_NAME = "فشنثثلالاشيشلا"
    const val SUB_APP_FILES = "ملفات_البرنامج"
    const val SUB_PHOTOS = "صور_السيارات"

    /**
     * Resolves the main directory in shared storage ("ملفاتي")
     */
    fun getMainFolder(context: Context): File {
        // Try Documents directory in shared storage first
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val mainDocs = File(docsDir, MAIN_FOLDER_NAME)
        if (!mainDocs.exists()) {
            mainDocs.mkdirs()
        }

        // Also try root of external storage if writable
        try {
            val rootDir = Environment.getExternalStorageDirectory()
            val rootFolder = File(rootDir, MAIN_FOLDER_NAME)
            if (!rootFolder.exists()) {
                rootFolder.mkdirs()
            }
        } catch (e: Exception) {
            Log.w("AppFolderManager", "Could not create root folder: ${e.message}")
        }

        // Fallback to app external files dir if Documents cannot be written
        if (!mainDocs.exists() && !mainDocs.canWrite()) {
            val fallback = File(context.getExternalFilesDir(null), MAIN_FOLDER_NAME)
            fallback.mkdirs()
            return fallback
        }

        return mainDocs
    }

    /**
     * Subfolder for application files (backups, PDFs, CSVs)
     */
    fun getAppFilesFolder(context: Context): File {
        val folder = File(getMainFolder(context), SUB_APP_FILES)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    /**
     * Subfolder for truck photos
     */
    fun getPhotosFolder(context: Context): File {
        val folder = File(getMainFolder(context), SUB_PHOTOS)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    /**
     * Copies a photo file into the public photos folder so it is visible in "ملفاتي"
     */
    fun copyPhotoToPublic(context: Context, sourceFile: File): File? {
        if (!sourceFile.exists()) return null
        return try {
            val targetDir = getPhotosFolder(context)
            val destFile = File(targetDir, sourceFile.name)
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            scanFile(context, destFile)
            destFile
        } catch (e: Exception) {
            Log.e("AppFolderManager", "Failed to copy photo to public folder: ${e.message}")
            null
        }
    }

    /**
     * Saves a copy of a backup or export file to the public app files folder
     */
    fun copyFileToAppFiles(context: Context, sourceFile: File): File? {
        if (!sourceFile.exists()) return null
        return try {
            val targetDir = getAppFilesFolder(context)
            val destFile = File(targetDir, sourceFile.name)
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            scanFile(context, destFile)
            destFile
        } catch (e: Exception) {
            Log.e("AppFolderManager", "Failed to copy file to app files folder: ${e.message}")
            null
        }
    }

    /**
     * Notifies the system media scanner so the file shows up immediately in file managers
     */
    fun scanFile(context: Context, file: File) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null
            ) { path, uri ->
                Log.d("AppFolderManager", "Scanned $path -> $uri")
            }
        } catch (e: Exception) {
            Log.w("AppFolderManager", "Media scan failed: ${e.message}")
        }
    }

    /**
     * Ensures all folders are created on app startup
     */
    fun initializeFolders(context: Context) {
        try {
            getAppFilesFolder(context)
            getPhotosFolder(context)
        } catch (e: Exception) {
            Log.e("AppFolderManager", "Folder initialization error: ${e.message}")
        }
    }
}
