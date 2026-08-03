package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.ContractStatus
import com.metamatch.app.domain.model.FinancialTerms
import com.metamatch.app.domain.model.GeoPoint
import com.metamatch.app.domain.model.IdentityVerificationTier
import com.metamatch.app.domain.model.PizzaShareIntent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FindPizzaMatchesUseCaseTest
 * =============================
 * Exercises the shared-purchase matching algorithm against a
 * [FakePizzaShareRepository] seeded with hand-picked scenarios — the
 * Pizza vertical's twin of `FindMatchesUseCaseTest`, run on the plain JVM,
 * no emulator required.
 */
class FindPizzaMatchesUseCaseTest {

    private val scheduledAt = Instant.parse("2026-08-15T20:00:00-05:00")

    private fun pizzaIntent(
        id: String,
        userId: String,
        email: String,
        homeLocation: GeoPoint,
        desiredUnits: Int = 4,
        totalUnits: Int = 8,
        establishment: String = "Domino's Pizza",
        productDescription: String = "Pizza grande especial",
        amount: Double = 100.0,
        maxDistanceMeters: Double = 400.0,
        allowedEmailDomains: Set<String> = emptySet(),
        blockedUserIds: Set<String> = emptySet(),
        scheduledAtOverride: Instant = scheduledAt,
    ) = PizzaShareIntent(
        id = id,
        creatorUserId = userId,
        creatorEmail = email,
        createdAt = scheduledAt,
        scheduledAt = scheduledAtOverride,
        expiresAt = null,
        verificationTier = IdentityVerificationTier.EMAIL_ONLY,
        financialTerms = FinancialTerms(amount = amount),
        status = ContractStatus.ACTIVE,
        homeLocation = homeLocation,
        establishment = establishment,
        productDescription = productDescription,
        totalUnits = totalUnits,
        totalPriceForWholeOrder = 200.0,
        desiredUnits = desiredUnits,
        maxDistanceMeters = maxDistanceMeters,
        allowedEmailDomains = allowedEmailDomains,
        blockedUserIds = blockedUserIds,
    )

    @Test
    fun `two nearby intents wanting complementary units produce one fully-claimed match`() = runTest {
        val me = pizzaIntent("me", "user-me", "me@ucaribe.edu.mx", GeoPoint(21.0894, -86.8459), desiredUnits = 4)
        val other = pizzaIntent("other", "user-other", "other@ucaribe.edu.mx", GeoPoint(21.0896, -86.8461), desiredUnits = 4)
        val repository = FakePizzaShareRepository(initialIntents = listOf(me, other))

        val matches = FindPizzaMatchesUseCase(repository)("user-me")

        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals(2, match.participantIntentIds.size)
        assertEquals(8, match.totalUnitsClaimed)
        assertTrue(match.isFullyClaimed)
    }

    @Test
    fun `different establishment never matches`() = runTest {
        val me = pizzaIntent("me", "user-me", "me@ucaribe.edu.mx", GeoPoint(21.0894, -86.8459), establishment = "Domino's Pizza")
        val other = pizzaIntent("other", "user-other", "other@ucaribe.edu.mx", GeoPoint(21.0896, -86.8461), establishment = "Pizza Hut")
        val repository = FakePizzaShareRepository(initialIntents = listOf(me, other))

        val matches = FindPizzaMatchesUseCase(repository)("user-me")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `units that would overshoot the whole order are excluded`() = runTest {
        // "me" wants 4 of 8; a candidate wanting 6 would push the total to
        // 10, over the order's 8 units, so they must NOT be matched.
        val me = pizzaIntent("me", "user-me", "me@ucaribe.edu.mx", GeoPoint(21.0894, -86.8459), desiredUnits = 4, totalUnits = 8)
        val tooMuch = pizzaIntent("too-much", "user-toomuch", "toomuch@ucaribe.edu.mx", GeoPoint(21.0896, -86.8461), desiredUnits = 6, totalUnits = 8)
        val repository = FakePizzaShareRepository(initialIntents = listOf(me, tooMuch))

        val matches = FindPizzaMatchesUseCase(repository)("user-me")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `intents scheduled far apart never match`() = runTest {
        val me = pizzaIntent("me", "user-me", "me@ucaribe.edu.mx", GeoPoint(21.0894, -86.8459))
        val other = pizzaIntent(
            "other", "user-other", "other@ucaribe.edu.mx", GeoPoint(21.0896, -86.8461),
            scheduledAtOverride = scheduledAt.plus(kotlinx.datetime.DateTimePeriod(hours = 3), kotlinx.datetime.TimeZone.UTC),
        )
        val repository = FakePizzaShareRepository(initialIntents = listOf(me, other))

        val matches = FindPizzaMatchesUseCase(repository)("user-me")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `blacklisted user is never matched even if otherwise perfect`() = runTest {
        val me = pizzaIntent(
            "me", "user-me", "me@ucaribe.edu.mx", GeoPoint(21.0894, -86.8459),
            blockedUserIds = setOf("user-blocked"),
        )
        val blocked = pizzaIntent("blocked", "user-blocked", "blocked@ucaribe.edu.mx", GeoPoint(21.0895, -86.8460))
        val repository = FakePizzaShareRepository(initialIntents = listOf(me, blocked))

        val matches = FindPizzaMatchesUseCase(repository)("user-me")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `email domain restriction excludes outsiders in both directions`() = runTest {
        val me = pizzaIntent(
            "me", "user-me", "me@ucaribe.edu.mx", GeoPoint(21.0894, -86.8459),
            allowedEmailDomains = setOf("ucaribe.edu.mx"),
        )
        val outsider = pizzaIntent("outsider", "user-outsider", "someone@gmail.com", GeoPoint(21.0895, -86.8460))
        val repository = FakePizzaShareRepository(initialIntents = listOf(me, outsider))

        val matches = FindPizzaMatchesUseCase(repository)("user-me")

        assertTrue(matches.isEmpty())
    }
}
