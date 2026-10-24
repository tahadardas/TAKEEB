package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TruckEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TruckEntryDao {

    @Query("SELECT * FROM truck_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<TruckEntry>>

    @Query("SELECT * FROM truck_entries ORDER BY createdAt DESC")
    suspend fun getAllEntriesSnapshot(): List<TruckEntry>

    @Query("""
        SELECT * FROM truck_entries 
        WHERE truckNumber LIKE '%' || :query || '%' 
           OR driverName LIKE '%' || :query || '%' 
        ORDER BY createdAt DESC
    """)
    fun searchEntries(query: String): Flow<List<TruckEntry>>

    @Query("SELECT * FROM truck_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): TruckEntry?

    @Query("SELECT * FROM truck_entries WHERE LOWER(TRIM(truckNumber)) = LOWER(TRIM(:truckNumber)) LIMIT 1")
    suspend fun getEntryByTruckNumber(truckNumber: String): TruckEntry?

    @Query("SELECT DISTINCT truckNumber FROM truck_entries WHERE truckNumber != '' ORDER BY truckNumber ASC")
    fun getDistinctTruckNumbers(): Flow<List<String>>

    @Query("SELECT DISTINCT driverName FROM truck_entries WHERE driverName != '' ORDER BY driverName ASC")
    fun getDistinctDriverNames(): Flow<List<String>>

    @Query("SELECT DISTINCT materialType FROM truck_entries WHERE materialType != '' ORDER BY materialType ASC")
    fun getDistinctMaterials(): Flow<List<String>>

    @Query("SELECT DISTINCT siteLocation FROM truck_entries WHERE siteLocation != '' ORDER BY siteLocation ASC")
    fun getDistinctSites(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TruckEntry): Long

    @Update
    suspend fun updateEntry(entry: TruckEntry)

    @Delete
    suspend fun deleteEntry(entry: TruckEntry)

    @Query("DELETE FROM truck_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("SELECT COUNT(*) FROM truck_entries")
    fun getTruckCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalVolume), 0.0) FROM truck_entries")
    fun getTotalVolumeSum(): Flow<Double>
}
