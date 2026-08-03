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
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days

/**
 * UpdateRoommateIntentUseCaseTest
 * =================================
 * Verifies the price-renegotiation flow this vertical is built around:
 * editing an already-published listing's terms in place, and having a
 * subsequent match recompute [com.metamatch.app.domain.model
 * .RoommateMatchResult.priceGapPercent] against the new value — see
 * [com.metamatch.app.domain.repository.RoommateRepository.updateIntent]'s
 * own docs for why this exists only for Roomie.
 */
class UpdateRoommateIntentUseCaseTest {

    private val windowStart = Instant.parse("2026-08-15T00:00:00-05:00")
    private val windowEnd = windowStart.plus(60.days)

    private fun roommateIntent(id: String, userId: String, email: String, role: RoommateRole, amount: Double) = RoommateIntent(
        id = id,
        creatorUserId = userId,
        creatorEmail = email,
        createdAt = windowStart,
        scheduledAt = windowStart,
        expiresAt = windowEnd,
        verificationTier = IdentityVerificationTier.EMAIL_ONLY,
        financialTerms = FinancialTerms(amount = amount),
        status = ContractStatus.ACTIVE,
        role = role,
        zone = "Cancún - Región 15",
        propertyDescription = "A place",
        moveInWindowStart = windowStart,
        moveInWindowEnd = windowEnd,
        leaseDurationMonths = 12,
        depositAmount = amount,
        guarantorArrangement = "None yet",
        preferenceNotes = "",
        isThirdPartyArrangement = false,
        occupantDescription = "",
    )

    @Test
    fun `updating the offerer's price is reflected in a subsequent match`() = runTest {
        val seeker = roommateIntent("seeker", "user-seeker", "seeker@ucaribe.edu.mx", RoommateRole.SEEKING, amount = 8000.0)
        val offerer = roommateIntent("offerer", "user-offerer", "offerer@ucaribe.edu.mx", RoommateRole.OFFERING, amount = 8500.0)
        val repository = FakeRoommateRepository(initialIntents = listOf(seeker, offerer))

        val beforeMatch = FindRoommateMatchesUseCase(repository)("user-seeker").first()
        assertTrue(beforeMatch.priceGapPercent > 0.0)

        val loweredOffer = offerer.copy(financialTerms = offerer.financialTerms.copy(amount = 8000.0))
        UpdateRoommateIntentUseCase(repository)(loweredOffer)

        val afterMatch = FindRoommateMatchesUseCase(repository)("user-seeker").first()
        assertEquals(8000.0, afterMatch.askingPrice, 0.0)
        assertTrue(afterMatch.isPriceAligned)
    }
}
