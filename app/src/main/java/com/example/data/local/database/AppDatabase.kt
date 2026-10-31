package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.TruckEntryDao
import com.example.data.local.dao.TruckModificationDao
import com.example.data.local.entity.TruckEntry
import com.example.data.local.entity.TruckModification

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TruckEntry::class, TruckModification::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun truckEntryDao(): TruckEntryDao
    abstract fun truckModificationDao(): TruckModificationDao

    companion object {
        const val DATABASE_NAME = "truck_cubic_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `truck_entries` ADD COLUMN `driverPhone` TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `truck_modifications` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `truckNumber` TEXT NOT NULL,
                        `modificationDate` INTEGER NOT NULL,
                        `modificationType` TEXT NOT NULL,
                        `previousHeadVolume` REAL NOT NULL,
                        `previousTailVolume` REAL NOT NULL,
                        `previousTotalVolume` REAL NOT NULL,
                        `previousHasExtensions` INTEGER NOT NULL,
                        `previousExtensionDetails` TEXT NOT NULL,
                        `newHeadVolume` REAL NOT NULL,
                        `newTailVolume` REAL NOT NULL,
                        `newTotalVolume` REAL NOT NULL,
                        `newHasExtensions` INTEGER NOT NULL,
                        `newExtensionDetails` TEXT NOT NULL,
                        `volumeDifference` REAL NOT NULL,
                        `notes` TEXT NOT NULL,
                        `photoPaths` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_truck_modifications_truckNumber` ON `truck_modifications` (`truckNumber`)")
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
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
