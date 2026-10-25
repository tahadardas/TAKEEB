package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TruckModification
import kotlinx.coroutines.flow.Flow

data class TruckModificationCount(
    val truckNumber: String,
    val count: Int
)

@Dao
interface TruckModificationDao {

    @Query("SELECT * FROM truck_modifications WHERE LOWER(TRIM(truckNumber)) = LOWER(TRIM(:truckNumber)) ORDER BY modificationDate DESC")
    fun getModificationsForTruck(truckNumber: String): Flow<List<TruckModification>>

    @Query("SELECT * FROM truck_modifications WHERE LOWER(TRIM(truckNumber)) = LOWER(TRIM(:truckNumber)) ORDER BY modificationDate ASC")
    suspend fun getModificationsForTruckAscSnapshot(truckNumber: String): List<TruckModification>

    @Query("SELECT * FROM truck_modifications ORDER BY modificationDate DESC")
    fun getAllModifications(): Flow<List<TruckModification>>

    @Query("SELECT * FROM truck_modifications ORDER BY modificationDate DESC")
    suspend fun getAllModificationsSnapshot(): List<TruckModification>

    @Query("SELECT COUNT(*) FROM truck_modifications WHERE LOWER(TRIM(truckNumber)) = LOWER(TRIM(:truckNumber))")
    fun getModificationCountForTruck(truckNumber: String): Flow<Int>

    @Query("SELECT truckNumber, COUNT(*) as count FROM truck_modifications GROUP BY truckNumber")
    fun getModificationCounts(): Flow<List<TruckModificationCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModification(modification: TruckModification): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModifications(modifications: List<TruckModification>)

    @Update
    suspend fun updateModification(modification: TruckModification)

    @Delete
    suspend fun deleteModification(modification: TruckModification)

    @Query("DELETE FROM truck_modifications WHERE id = :id")
    suspend fun deleteModificationById(id: Long)

    @Query("DELETE FROM truck_modifications WHERE LOWER(TRIM(truckNumber)) = LOWER(TRIM(:truckNumber))")
    suspend fun deleteModificationsForTruck(truckNumber: String)
}
