package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a truck measurement entry in the cubic volume log.
 *
 * @param id Unique identifier
 * @param truckNumber License plate or truck number (رقم السيارة)
 * @param driverName Name of the driver (اسم السائق)
 * @param driverPhone Driver phone number (رقم هاتف السائق - اختياري)
 * @param hasHeadAndTail Whether the truck has head & tail (رأس وذنب) or head only (رأس فقط)
 * @param headVolume Head volume in cubic meters (كمية الرأس م³)
 * @param tailVolume Tail volume in cubic meters (كمية الذنب م³) - 0.0 if head only
 * @param totalVolume Total calculated volume (إجمالي التكعيب م³)
 * @param hasExtensions Whether the truck has extensions (وصلات)
 * @param extensionDetails Optional details about the extensions (ارتفاع/عدد الوصلات)
 * @param materialType Material or cargo type (نوع الحمولة: رمل، بحص، تراب...)
 * @param siteLocation Project or job site location (الموقع / المشروع)
 * @param notes Extra notes
 * @param photoPaths Comma-separated file paths of photos captured exclusively via camera (صور الكاميرا الحصرية)
 * @param createdAt Creation timestamp in milliseconds
 */
@Entity(tableName = "truck_entries")
data class TruckEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val truckNumber: String,
    val driverName: String,
    val driverPhone: String = "",
    val hasHeadAndTail: Boolean,
    val headVolume: Double,
    val tailVolume: Double,
    val totalVolume: Double,
    val hasExtensions: Boolean,
    val extensionDetails: String = "",
    val materialType: String = "",
    val siteLocation: String = "",
    val notes: String = "",
    val photoPaths: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Returns a list of photo file paths
     */
    fun getPhotoList(): List<String> {
        if (photoPaths.isBlank()) return emptyList()
        return photoPaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
