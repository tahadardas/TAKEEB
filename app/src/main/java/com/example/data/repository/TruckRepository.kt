package com.example.data.repository

import com.example.data.local.dao.TruckEntryDao
import com.example.data.local.entity.TruckEntry
import com.example.util.RestoreResult
import kotlinx.coroutines.flow.Flow

class TruckRepository(private val dao: TruckEntryDao) {

    val allEntries: Flow<List<TruckEntry>> = dao.getAllEntries()

    val distinctTruckNumbers: Flow<List<String>> = dao.getDistinctTruckNumbers()
    val distinctDriverNames: Flow<List<String>> = dao.getDistinctDriverNames()
    val distinctMaterials: Flow<List<String>> = dao.getDistinctMaterials()
    val distinctSites: Flow<List<String>> = dao.getDistinctSites()

    fun searchEntries(query: String): Flow<List<TruckEntry>> {
        return if (query.isBlank()) {
            dao.getAllEntries()
        } else {
            dao.searchEntries(query.trim())
        }
    }

    suspend fun getAllEntriesSnapshot(): List<TruckEntry> = dao.getAllEntriesSnapshot()

    suspend fun getEntryById(id: Long): TruckEntry? = dao.getEntryById(id)

    suspend fun getEntryByTruckNumber(truckNumber: String): TruckEntry? =
        dao.getEntryByTruckNumber(truckNumber.trim())

    suspend fun insert(entry: TruckEntry): Long = dao.insertEntry(entry)

    suspend fun update(entry: TruckEntry) = dao.updateEntry(entry)

    suspend fun delete(entry: TruckEntry) = dao.deleteEntry(entry)

    suspend fun deleteById(id: Long) = dao.deleteEntryById(id)

    /**
     * Smart Merge / Upsert for Backup Restoration:
     * Updates existing trucks by truckNumber, and inserts new trucks.
     * Never deletes or wipes existing data!
     */
    suspend fun upsertBackupEntries(entries: List<TruckEntry>): RestoreResult {
        var added = 0
        var updated = 0

        for (imported in entries) {
            val existing = dao.getEntryByTruckNumber(imported.truckNumber)
            if (existing != null) {
                // Update existing record with the imported data, retaining existing id
                val updatedEntry = imported.copy(id = existing.id)
                dao.updateEntry(updatedEntry)
                updated++
            } else {
                // Insert as new record
                dao.insertEntry(imported.copy(id = 0))
                added++
            }
        }

        return RestoreResult(
            success = true,
            addedCount = added,
            updatedCount = updated,
            totalCount = entries.size
        )
    }

    val totalTruckCount: Flow<Int> = dao.getTruckCount()

    val totalVolumeSum: Flow<Double> = dao.getTotalVolumeSum()
}
