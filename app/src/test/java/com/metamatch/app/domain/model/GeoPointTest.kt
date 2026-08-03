package com.metamatch.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GeoPointTest
 * ==============
 * A small, fast, pure-JVM unit test (no Android framework, no emulator
 * needed) for the geometry helpers [GeoPoint] provides. This is exactly
 * the kind of test the domain layer's "no Android imports" rule exists to
 * enable — it runs in milliseconds on any machine.
 */
class GeoPointTest {

    @Test
    fun `distance to self is zero`() {
        val point = GeoPoint(21.0894, -86.8459)
        assertEquals(0.0, point.distanceMetersTo(point), 0.001)
    }

    @Test
    fun `distance between two known Cancun points is roughly correct`() {
        // Universidad del Caribe campus vs. Cancun International Airport.
        val ucaribe = GeoPoint(21.0894, -86.8459)
        val airport = GeoPoint(21.0367, -86.8770)

        val distanceMeters = ucaribe.distanceMetersTo(airport)

        // Straight-line distance here is roughly 6-7 km; a wide tolerance
        // keeps this test robust to the exact coordinates chosen while
        // still catching a badly broken Haversine implementation.
        assertTrue("Expected a few kilometers, got $distanceMeters meters", distanceMeters in 4_000.0..10_000.0)
    }

    @Test
    fun `centroid of two identical points is that same point`() {
        val point = GeoPoint(19.4326, -99.1332)
        val centroid = GeoPoint.centroidOf(listOf(point, point))
        assertEquals(point, centroid)
    }

    @Test
    fun `centroid of two distinct points lies exactly between them`() {
        val a = GeoPoint(0.0, 0.0)
        val b = GeoPoint(2.0, 2.0)
        val centroid = GeoPoint.centroidOf(listOf(a, b))
        assertEquals(1.0, centroid.latitude, 0.0001)
        assertEquals(1.0, centroid.longitude, 0.0001)
    }
}
