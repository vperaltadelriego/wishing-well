package com.metamatch.app.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * GeoPoint
 * ========
 *
 * WHAT: a plain latitude/longitude pair, used everywhere the domain layer
 * needs to talk about "a place on the map" (departure point, destination,
 * a computed meeting centroid).
 *
 * WHY it exists as its own type instead of two loose `Double` parameters:
 * passing `lat: Double, lng: Double` around every function signature is
 * easy to get backwards (did I pass longitude first by mistake?). Wrapping
 * both in one named type removes that entire class of bug and gives us one
 * place to put geometry helpers like [distanceMetersTo].
 *
 * HOW this maps to the real backend (Supabase + PostGIS):
 * In `schema.sql`, this same concept is stored as a native
 * `geography(Point, 4326)` column. PostGIS can then run spatial SQL
 * functions such as `ST_DWithin` and `ST_Centroid` directly on the
 * database, instead of pulling every row into the app to do geometry in
 * Kotlin. [distanceMetersTo] below is a *client-side approximation* of
 * that same idea (the Haversine formula) — good enough for showing a
 * "~350 m away" label in the UI or for the `MockRideShareRepository` to
 * simulate matching without a real database, but the source of truth for
 * production matching is always the PostGIS query described in
 * `schema.sql`.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90." }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180." }
    }

    /**
     * Straight-line ("as the crow flies") distance to [other], in meters,
     * using the Haversine formula. This ignores streets/sidewalks, so it
     * systematically *underestimates* real walking distance — that is
     * intentional: [com.metamatch.app.domain.usecase.FindMatchesUseCase]
     * treats it as a cheap first-pass filter, not the final word.
     */
    fun distanceMetersTo(other: GeoPoint): Double {
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLng = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMeters * c
    }

    companion object {
        /**
         * The geometric mean position of a list of points — a lightweight
         * stand-in for PostGIS's `ST_Centroid()`. Used by
         * [com.metamatch.app.domain.usecase.FindMatchesUseCase] to find the
         * "fair meeting point" between two or more passengers' destinations.
         */
        fun centroidOf(points: List<GeoPoint>): GeoPoint {
            require(points.isNotEmpty()) { "Cannot compute a centroid of zero points." }
            val avgLat = points.sumOf { it.latitude } / points.size
            val avgLng = points.sumOf { it.longitude } / points.size
            return GeoPoint(avgLat, avgLng)
        }
    }
}
