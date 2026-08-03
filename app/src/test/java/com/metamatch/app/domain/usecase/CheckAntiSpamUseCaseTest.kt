package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.exception.MicroFeeRequiredException
import com.metamatch.app.domain.model.ContractStatus
import com.metamatch.app.domain.model.FinancialTerms
import com.metamatch.app.domain.model.GeoPoint
import com.metamatch.app.domain.model.IdentityVerificationTier
import com.metamatch.app.domain.model.RideShareIntent
import com.metamatch.app.domain.policy.PlatformPolicy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * CheckAntiSpamUseCaseTest
 * ===========================
 * Verifies Module 2's "5 free listings, then a micro-fee" rule directly
 * against a small, explicit [PlatformPolicy] — proof that the free-tier
 * limit is genuinely data-driven rather than hard-coded inside the use
 * case (see [PlatformPolicy]'s own documentation on why that matters for
 * "agility after testing the app").
 */
class CheckAntiSpamUseCaseTest {

    private fun activeIntent(id: String, userId: String) = RideShareIntent(
        id = id,
        creatorUserId = userId,
        creatorEmail = "$userId@ucaribe.edu.mx",
        createdAt = Instant.parse("2026-08-01T10:00:00-06:00"),
        scheduledAt = Instant.parse("2026-08-02T10:00:00-06:00"),
        expiresAt = null,
        verificationTier = IdentityVerificationTier.EMAIL_ONLY,
        financialTerms = FinancialTerms(amount = 0.0),
        status = ContractStatus.ACTIVE,
        departure = GeoPoint(19.4363, -99.0721),
        destination = GeoPoint(19.4270, -99.1677),
        maxWalkingDistanceMeters = 300.0,
    )

    @Test
    fun `under the free limit does not require a fee`() = runTest {
        val repository = FakeRideShareRepository(
            initialIntents = listOf(activeIntent("1", "user-a")),
            policy = PlatformPolicy(freeActiveIntentLimit = 5),
        )
        val result = CheckAntiSpamUseCase(repository)(userId = "user-a", acceptsMicroFee = false)
        assertFalse(result.requiresFee)
    }

    @Test
    fun `at the free limit throws unless the fee is accepted`() = runTest {
        val fiveActiveIntents = (1..5).map { activeIntent(it.toString(), "user-a") }
        val repository = FakeRideShareRepository(
            initialIntents = fiveActiveIntents,
            policy = PlatformPolicy(freeActiveIntentLimit = 5, microFeeAmountCents = 10),
        )
        val useCase = CheckAntiSpamUseCase(repository)

        try {
            useCase(userId = "user-a", acceptsMicroFee = false)
            fail("Expected MicroFeeRequiredException when at the free limit.")
        } catch (e: MicroFeeRequiredException) {
            assertTrue(e.feeAmountCents == 10)
            assertTrue(e.currentActiveCount == 5)
        }

        val result = useCase(userId = "user-a", acceptsMicroFee = true)
        assertTrue(result.requiresFee)
        assertTrue(result.feeAmountCents == 10)
    }
}
