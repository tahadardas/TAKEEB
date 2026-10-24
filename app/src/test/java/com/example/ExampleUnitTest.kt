package com.example

import com.example.data.local.entity.TruckEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testHeadOnlyVolumeCalculation() {
        val headVolume = 18.5
        val entry = TruckEntry(
            truckNumber = "12345",
            driverName = "خالد",
            hasHeadAndTail = false,
            headVolume = headVolume,
            tailVolume = 0.0,
            totalVolume = headVolume,
            hasExtensions = false
        )
        assertEquals(18.5, entry.totalVolume, 0.001)
        assertEquals(0.0, entry.tailVolume, 0.001)
    }

    @Test
    fun testHeadAndTailVolumeCalculation() {
        val headVolume = 14.25
        val tailVolume = 19.75
        val total = headVolume + tailVolume
        val entry = TruckEntry(
            truckNumber = "78910",
            driverName = "محمد",
            hasHeadAndTail = true,
            headVolume = headVolume,
            tailVolume = tailVolume,
            totalVolume = total,
            hasExtensions = true,
            extensionDetails = "30 سم"
        )
        assertEquals(34.0, entry.totalVolume, 0.001)
        assertTrue(entry.hasExtensions)
    }

    @Test
    fun testDimensionsCubicCalculation() {
        val length = 5.0
        val width = 2.4
        val height = 1.5
        val cubicVolume = length * width * height
        assertEquals(18.0, cubicVolume, 0.001)
    }
}
