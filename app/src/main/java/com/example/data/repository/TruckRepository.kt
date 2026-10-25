package com.example.data.repository

import com.example.data.local.dao.TruckEntryDao
import com.example.data.local.dao.TruckModificationCount
import com.example.data.local.dao.TruckModificationDao
import com.example.data.local.entity.TruckEntry
import com.example.data.local.entity.TruckModification
import com.example.util.RestoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class TruckRepository(
    private val dao: TruckEntryDao,
    private val modificationDao: TruckModificationDao? = null
) {

    val allEntries: Flow<List<TruckEntry>> = dao.getAllEntries()

    val distinctTruckNumbers: Flow<List<String>> = dao.getDistinctTruckNumbers()
    val distinctDriverNames: Flow<List<String>> = dao.getDistinctDriverNames()
    val distinctDriverPhones: Flow<List<String>> = dao.getDistinctDriverPhones()
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

    suspend fun getEntryByPhone(phone: String): TruckEntry? =
        dao.getEntryByPhone(phone.trim())

    suspend fun insert(entry: TruckEntry): Long = dao.insertEntry(entry)

    suspend fun update(entry: TruckEntry) = dao.updateEntry(entry)

    suspend fun delete(entry: TruckEntry) = dao.deleteEntry(entry)

    suspend fun deleteById(id: Long) = dao.deleteEntryById(id)

    // ==========================================
    // Truck Modifications (History & Upgrades)
    // ==========================================

    fun getModificationsForTruck(truckNumber: String): Flow<List<TruckModification>> =
        modificationDao?.getModificationsForTruck(truckNumber.trim()) ?: emptyFlow()

    suspend fun getModificationsForTruckAscSnapshot(truckNumber: String): List<TruckModification> =
        modificationDao?.getModificationsForTruckAscSnapshot(truckNumber.trim()) ?: emptyList()

    fun getAllModifications(): Flow<List<TruckModification>> =
        modificationDao?.getAllModifications() ?: emptyFlow()

    suspend fun getAllModificationsSnapshot(): List<TruckModification> =
        modificationDao?.getAllModificationsSnapshot() ?: emptyList()

    fun getModificationCountForTruck(truckNumber: String): Flow<Int> =
        modificationDao?.getModificationCountForTruck(truckNumber.trim()) ?: emptyFlow()

    fun getModificationCounts(): Flow<List<TruckModificationCount>> =
        modificationDao?.getModificationCounts() ?: emptyFlow()

    suspend fun insertModification(modification: TruckModification): Long =
        modificationDao?.insertModification(modification) ?: -1L

    suspend fun deleteModification(modification: TruckModification) =
        modificationDao?.deleteModification(modification)

    suspend fun deleteModificationById(id: Long) =
        modificationDao?.deleteModificationById(id)

    suspend fun deleteModificationsForTruck(truckNumber: String) =
        modificationDao?.deleteModificationsForTruck(truckNumber.trim())

    /**
     * Records a new modification to a truck and immediately updates
     * the active TruckEntry in the database with the new volumes and extension state.
     */
    suspend fun recordTruckModification(
        updatedEntry: TruckEntry,
        modification: TruckModification
    ): Long {
        dao.updateEntry(updatedEntry)
        return modificationDao?.insertModification(modification) ?: -1L
    }

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

    /**
     * Upsert modification records during backup restore
     */
    suspend fun upsertBackupModifications(modifications: List<TruckModification>) {
        if (modificationDao == null || modifications.isEmpty()) return
        modificationDao.insertModifications(modifications.map { it.copy(id = 0) })
    }

    val totalTruckCount: Flow<Int> = dao.getTruckCount()

    val totalVolumeSum: Flow<Double> = dao.getTotalVolumeSum()
}
