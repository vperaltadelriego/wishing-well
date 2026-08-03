package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.ContractStatus
import com.metamatch.app.domain.model.FinancialTerms
import com.metamatch.app.domain.model.IdentityVerificationTier
import com.metamatch.app.domain.model.RoommateIntent
import com.metamatch.app.domain.model.RoommateRole
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days

/**
 * FindRoommateMatchesUseCaseTest
 * ================================
 * Exercises the asymmetric SEEKING/OFFERING matching algorithm against a
 * [FakeRoommateRepository] seeded with hand-picked scenarios — the Roomie
 * vertical's twin of `FindMatchesUseCaseTest`/`FindPizzaMatchesUseCaseTest`.
 */
class FindRoommateMatchesUseCaseTest {

    private val windowStart = Instant.parse("2026-08-15T00:00:00-05:00")
    private val windowEnd = windowStart.plus(60.days)

    private fun roommateIntent(
        id: String,
        userId: String,
        email: String,
        role: RoommateRole,
        amount: Double,
        zone: String = "Cancún - Región 15",
        moveInStart: Instant = windowStart,
        moveInEnd: Instant = windowEnd,
        allowedEmailDomains: Set<String> = emptySet(),
        blockedUserIds: Set<String> = emptySet(),
    ) = RoommateIntent(
        id = id,
        creatorUserId = userId,
        creatorEmail = email,
        createdAt = windowStart,
        scheduledAt = moveInStart,
        expiresAt = moveInEnd,
        verificationTier = IdentityVerificationTier.EMAIL_ONLY,
        financialTerms = FinancialTerms(amount = amount),
        status = ContractStatus.ACTIVE,
        role = role,
        zone = zone,
        propertyDescription = "A place",
        moveInWindowStart = moveInStart,
        moveInWindowEnd = moveInEnd,
        leaseDurationMonths = 12,
        depositAmount = amount,
        guarantorArrangement = "None yet",
        preferenceNotes = "",
        isThirdPartyArrangement = false,
        occupantDescription = "",
        allowedEmailDomains = allowedEmailDomains,
        blockedUserIds = blockedUserIds,
    )

    @Test
    fun `opposite roles in the same zone with overlapping windows produce a match`() = runTest {
        val seeker = roommateIntent("seeker", "user-seeker", "seeker@ucaribe.edu.mx", RoommateRole.SEEKING, amount = 8000.0)
        val offerer = roommateIntent("offerer", "user-offerer", "offerer@ucaribe.edu.mx", RoommateRole.OFFERING, amount = 8500.0)
        val repository = FakeRoommateRepository(initialIntents = listOf(seeker, offerer))

        val matches = FindRoommateMatchesUseCase(repository)("user-seeker")

        assertEquals(1, matches.size)
        assertEquals(listOf("seeker", "offerer"), matches.first().participantIntentIds)
    }

    @Test
    fun `same role never matches`() = runTest {
        val me = roommateIntent("me", "user-me", "me@ucaribe.edu.mx", RoommateRole.SEEKING, amount = 8000.0)
        val otherSeeker = roommateIntent("other", "user-other", "other@ucaribe.edu.mx", RoommateRole.SEEKING, amount = 8000.0)
        val repository = FakeRoommateRepository(initialIntents = listOf(me, otherSeeker))

        val matches = FindRoommateMatchesUseCase(repository)("user-me")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `different zone excludes an otherwise-compatible pair`() = runTest {
        val seeker = roommateIntent(
            "seeker", "user-seeker", "seeker@ucaribe.edu.mx", RoommateRole.SEEKING, amount = 8000.0,
            zone = "Cancún - Región 15",
        )
        val offerer = roommateIntent(
            "offerer", "user-offerer", "offerer@ucaribe.edu.mx", RoommateRole.OFFERING, amount = 8500.0,
            zone = "Playa del Carmen",
        )
        val repository = FakeRoommateRepository(initialIntents = listOf(seeker, offerer))

        val matches = FindRoommateMatchesUseCase(repository)("user-seeker")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `non-overlapping move-in windows exclude a match`() = runTest {
        val seeker = roommateIntent(
            "seeker", "user-seeker", "seeker@ucaribe.edu.mx", RoommateRole.SEEKING, amount = 8000.0,
            moveInStart = windowStart, moveInEnd = windowStart.plus(10.days),
        )
        val offerer = roommateIntent(
            "offerer", "user-offerer", "offerer@ucaribe.edu.mx", RoommateRole.OFFERING, amount = 8500.0,
            moveInStart = windowStart.plus(100.days), moveInEnd = windowStart.plus(150.days),
        )
        val repository = FakeRoommateRepository(initialIntents = listOf(seeker, offerer))

        val matches = FindRoommateMatchesUseCase(repository)("user-seeker")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `a misaligned price still produces a match, flagged not filtered`() = runTest {
        // The brief's own example: asking 8,500 against an 8,000 budget
        // must still surface as a match — price is never a hard filter.
        val seeker = roommateIntent("seeker", "user-seeker", "seeker@ucaribe.edu.mx", RoommateRole.SEEKING, amount = 8000.0)
        val offerer = roommateIntent("offerer", "user-offerer", "offerer@ucaribe.edu.mx", RoommateRole.OFFERING, amount = 8500.0)
        val repository = FakeRoommateRepository(initialIntents = listOf(seeker, offerer))

        val matches = FindRoommateMatchesUseCase(repository)("user-seeker")

        assertEquals(1, matches.size)
        val match = matches.first()
        assertFalse(match.isPriceAligned)
        assertTrue(match.priceGapPercent > 0.0)
    }

    @Test
    fun `blacklisted user is never matched even if otherwise compatible`() = runTest {
        val seeker = roommateIntent(
            "seeker", "user-seeker", "seeker@ucaribe.edu.mx", RoommateRole.SEEKING, amount = 8000.0,
            blockedUserIds = setOf("user-offerer"),
        )
        val offerer = roommateIntent("offerer", "user-offerer", "offerer@ucaribe.edu.mx", RoommateRole.OFFERING, amount = 8500.0)
        val repository = FakeRoommateRepository(initialIntents = listOf(seeker, offerer))

        val matches = FindRoommateMatchesUseCase(repository)("user-seeker")

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `email domain restriction excludes outsiders in both directions`() = runTest {
        val seeker = roommateIntent(
            "seeker", "user-seeker", "seeker@ucaribe.edu.mx", RoommateRole.SEEKING, amount = 8000.0,
            allowedEmailDomains = setOf("ucaribe.edu.mx"),
        )
        val outsider = roommateIntent("offerer", "user-outsider", "someone@gmail.com", RoommateRole.OFFERING, amount = 8500.0)
        val repository = FakeRoommateRepository(initialIntents = listOf(seeker, outsider))

        val matches = FindRoommateMatchesUseCase(repository)("user-seeker")

        assertTrue(matches.isEmpty())
    }
}
