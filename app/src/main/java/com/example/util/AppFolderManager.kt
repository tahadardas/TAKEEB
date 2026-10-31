package com.example.util

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Manages the dedicated public storage folder requested by the user: "تكعيب الغضب"
 * located directly at the root of user storage ("ملفاتي" / Internal Storage), containing:
 * - ملفات_البرنامج (App files: backups, reports, exports)
 * - صور_السيارات (Truck documentation photos)
 */
object AppFolderManager {

    const val MAIN_FOLDER_NAME = "تكعيب الغضب"
    const val OLD_FOLDER_NAME = "فشنثثلالاشيشلا"
    const val SUB_APP_FILES = "ملفات_البرنامج"
    const val SUB_PHOTOS = "صور_السيارات"

    /**
     * Resolves the main directory in shared storage ("ملفاتي")
     * Prioritizes the root level of internal storage so the user sees it immediately upon opening "ملفاتي"
     */
    fun getMainFolder(context: Context): File {
        // 1. Primary: Direct root of user storage ("ملفاتي" / Internal Storage Root)
        try {
            val rootDir = Environment.getExternalStorageDirectory()
            val rootFolder = File(rootDir, MAIN_FOLDER_NAME)
            if (!rootFolder.exists()) {
                rootFolder.mkdirs()
            }
            if (rootFolder.exists() && rootFolder.canWrite()) {
                scanFile(context, rootFolder)
                return rootFolder
            }
        } catch (e: Exception) {
            Log.w("AppFolderManager", "Root folder creation notice: ${e.message}")
        }

        // 2. Secondary fallback: Documents directory in shared storage
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val mainDocs = File(docsDir, MAIN_FOLDER_NAME)
        if (!mainDocs.exists()) {
            mainDocs.mkdirs()
        }
        if (mainDocs.exists() && mainDocs.canWrite()) {
            scanFile(context, mainDocs)
            return mainDocs
        }

        // 3. Fallback to app external files dir if root and Documents cannot be written
        val fallback = File(context.getExternalFilesDir(null), MAIN_FOLDER_NAME)
        if (!fallback.exists()) fallback.mkdirs()
        return fallback
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
     * Automatically migrates data from previous old folder names (e.g. فشنثثلالاشيشلا)
     * into the new root folder "تكعيب الغضب" so no user files are lost
     */
    fun migrateOldFolderIfExists(context: Context) {
        try {
            val targetMain = getMainFolder(context)
            val potentialOldLocations = listOf(
                File(Environment.getExternalStorageDirectory(), OLD_FOLDER_NAME),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), OLD_FOLDER_NAME),
                File(context.getExternalFilesDir(null), OLD_FOLDER_NAME)
            )

            for (oldDir in potentialOldLocations) {
                if (oldDir.exists() && oldDir.isDirectory) {
                    copyDirectoryRecursively(context, oldDir, targetMain)
                    try {
                        oldDir.deleteRecursively()
                    } catch (e: Exception) {
                        Log.w("AppFolderManager", "Could not delete old folder: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppFolderManager", "Migration from old folder error: ${e.message}")
        }
    }

    private fun copyDirectoryRecursively(context: Context, source: File, destination: File) {
        if (!destination.exists()) destination.mkdirs()
        source.listFiles()?.forEach { file ->
            val destChild = File(destination, file.name)
            if (file.isDirectory) {
                copyDirectoryRecursively(context, file, destChild)
            } else {
                if (!destChild.exists() || destChild.length() == 0L) {
                    file.copyTo(destChild, overwrite = true)
                    scanFile(context, destChild)
                }
            }
        }
    }

    /**
     * Ensures all folders are created and old data migrated on app startup
     */
    fun initializeFolders(context: Context) {
        try {
            getAppFilesFolder(context)
            getPhotosFolder(context)
            migrateOldFolderIfExists(context)
        } catch (e: Exception) {
            Log.e("AppFolderManager", "Folder initialization error: ${e.message}")
        }
    }
}
