package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a historical modification or upgrade made to a truck over time
 * (e.g., adding extensions, enlarging or shrinking the cargo box, updating measurements).
 *
 * Tracks how the truck was before, what changes were made, and the resulting volume.
 *
 * @param id Unique identifier
 * @param truckNumber License plate or truck number (رقم السيارة)
 * @param modificationDate Timestamp of when the modification took place
 * @param modificationType Type or title of modification (e.g. إضافة وصلة، تكبير الصندوق، تصغير الصندوق)
 * @param previousHeadVolume Head volume before modification (م³ سابقاً)
 * @param previousTailVolume Tail volume before modification (م³ سابقاً)
 * @param previousTotalVolume Total volume before modification (م³ سابقاً)
 * @param previousHasExtensions Whether it had extensions previously
 * @param previousExtensionDetails Previous extension details
 * @param newHeadVolume Head volume after modification (م³ حالياً)
 * @param newTailVolume Tail volume after modification (م³ حالياً)
 * @param newTotalVolume Total volume after modification (م³ حالياً)
 * @param newHasExtensions Whether it has extensions after modification
 * @param newExtensionDetails New extension details
 * @param volumeDifference Net change in cubic volume (newTotalVolume - previousTotalVolume)
 * @param notes Notes explaining the modification reason or workshop details
 * @param photoPaths Comma-separated photo paths documenting the modification
 */
@Entity(
    tableName = "truck_modifications",
    indices = [
        Index(value = ["truckNumber"])
    ]
)
data class TruckModification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val truckNumber: String,
    val modificationDate: Long = System.currentTimeMillis(),
    val modificationType: String,
    val previousHeadVolume: Double,
    val previousTailVolume: Double,
    val previousTotalVolume: Double,
    val previousHasExtensions: Boolean,
    val previousExtensionDetails: String = "",
    val newHeadVolume: Double,
    val newTailVolume: Double,
    val newTotalVolume: Double,
    val newHasExtensions: Boolean,
    val newExtensionDetails: String = "",
    val volumeDifference: Double = newTotalVolume - previousTotalVolume,
    val notes: String = "",
    val photoPaths: String = ""
) {
    fun getPhotoList(): List<String> {
        if (photoPaths.isBlank()) return emptyList()
        return photoPaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
