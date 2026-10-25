package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.TruckEntryDao
import com.example.data.local.dao.TruckModificationDao
import com.example.data.local.entity.TruckEntry
import com.example.data.local.entity.TruckModification

@Database(entities = [TruckEntry::class, TruckModification::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun truckEntryDao(): TruckEntryDao
    abstract fun truckModificationDao(): TruckModificationDao

    companion object {
        const val DATABASE_NAME = "truck_cubic_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabaseFile(context: Context): java.io.File {
            return context.getDatabasePath(DATABASE_NAME)
        }

        fun getDatabaseSizeFormatted(context: Context): String {
            val file = getDatabaseFile(context)
            if (!file.exists()) return "0 KB"
            val bytes = file.length()
            val kb = bytes / 1024.0
            return if (kb > 1024) {
                String.format(java.util.Locale.US, "%.2f MB", kb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.1f KB", kb)
            }
        }
    }
}
