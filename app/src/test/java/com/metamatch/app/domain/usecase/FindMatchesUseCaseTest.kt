package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.ContractStatus
import com.metamatch.app.domain.model.FinancialTerms
import com.metamatch.app.domain.model.GeoPoint
import com.metamatch.app.domain.model.IdentityVerificationTier
import com.metamatch.app.domain.model.RideShareIntent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FindMatchesUseCaseTest
 * =========================
 * Exercises the geospatial/budget matching algorithm against a
 * [FakeRideShareRepository] seeded with hand-picked scenarios — this is
 * the payoff of keeping [FindMatchesUseCase] free of Android
 * dependencies: every one of these tests runs on the plain JVM in
 * milliseconds, no emulator required.
 */
class FindMatchesUseCaseTest {

    private val scheduledAt = Instant.parse("2030-10-10T20:00:00-06:00")

    private fun rideIntent(
        id: String,
        userId: String,
        email: String,
        destination: GeoPoint,
        amount: Double = 30.0,
        maxWalkingDistanceMeters: Double = 500.0,
        allowedEmailDomains: Set<String> = emptySet(),
        blockedUserIds: Set<String> = emptySet(),
        scheduledAtOverride: Instant = scheduledAt,
    ) = RideShareIntent(
        id = id,
        creatorUserId = userId,
        creatorEmail = email,
        createdAt = scheduledAt,
        scheduledAt = scheduledAtOverride,
        expiresAt = null,
        verificationTier = IdentityVerificationTier.EMAIL_ONLY,
        financialTerms = FinancialTerms(amount = amount),
        status = ContractStatus.ACTIVE,
        departure = GeoPoint(19.4363, -99.0721),
        destination = destination,
        maxWalkingDistanceMeters = maxWalkingDistanceMeters,
        allowedEmailDomains = allowedEmailDomains,
        blockedUserIds = blockedUserIds,
    )

    @Test
    fun `two nearby intents at the same time produce one match`() = runTest {
        val me = rideIntent("me", "user-me", "me@ucaribe.edu.mx", GeoPoint(19.4270, -99.1677))
        val other = rideIntent("other", "user-other", "other@ucaribe.edu.mx", GeoPoint(19.4272, -99.1675))
        val repository = FakeRideShareRepository(initialIntents = listOf(me, other))

        val matches = FindMatchesUseCase(repository)("user-me")

        assertEquals(1, matches.size)
        assertEquals(2, matches.first().participantIntentIds.size)
    }

    @Test
    fun `intents scheduled far apart never match`() = runTest {
        val me = rideIntent("me", "user-me", "me@ucaribe.edu.mx", GeoPoint(19.4270, -99.1677))
        val other = rideIntent(
            "other", "user-other", "other@ucaribe.edu.mx", GeoPoint(19.4272, -99.1675),
            scheduledAtOverride = scheduledAt.plus(kotlinx.datetime.DateTimePeriod(days = 30), kotlinx.datetime.TimeZone.UTC),
        )
        val repository = FakeRideShareRepository(initialIntents = listOf(me, other))

        val matches = FindMatchesUseCase(repository)("user-me")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `blacklisted user is never matched even if geospatially perfect`() = runTest {
        val me = rideIntent(
            "me", "user-me", "me@ucaribe.edu.mx", GeoPoint(19.4270, -99.1677),
            blockedUserIds = setOf("user-blocked"),
        )
        val blocked = rideIntent("blocked", "user-blocked", "blocked@ucaribe.edu.mx", GeoPoint(19.4271, -99.1676))
        val repository = FakeRideShareRepository(initialIntents = listOf(me, blocked))

        val matches = FindMatchesUseCase(repository)("user-me")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `email domain restriction excludes outsiders in both directions`() = runTest {
        val me = rideIntent(
            "me", "user-me", "me@ucaribe.edu.mx", GeoPoint(19.4270, -99.1677),
            allowedEmailDomains = setOf("ucaribe.edu.mx"),
        )
        val outsider = rideIntent("outsider", "user-outsider", "someone@gmail.com", GeoPoint(19.4271, -99.1676))
        val repository = FakeRideShareRepository(initialIntents = listOf(me, outsider))

        val matches = FindMatchesUseCase(repository)("user-me")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `match correctly flags an insufficient pooled budget`() = runTest {
        // Destinations are ~65 km from the shared departure point, so the
        // fare estimate will comfortably exceed two very small contributions.
        val me = rideIntent("me", "user-me", "me@ucaribe.edu.mx", GeoPoint(19.9, -99.9), amount = 1.0)
        val other = rideIntent("other", "user-other", "other@ucaribe.edu.mx", GeoPoint(19.901, -99.901), amount = 1.0)
        val repository = FakeRideShareRepository(initialIntents = listOf(me, other))

        val matches = FindMatchesUseCase(repository)("user-me")

        assertEquals(1, matches.size)
        assertTrue(!matches.first().meetsMinimumFare)
    }
}
