package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.repository.RoommateRepository
import javax.inject.Inject

/**
 * CheckRoommateAntiSpamUseCase
 * ==============================
 *
 * WHAT: the Roomie vertical's entry point for the same free-tier/micro-fee
 * rule [CheckAntiSpamUseCase] (Ride) and [CheckPizzaAntiSpamUseCase]
 * (Pizza) enforce — see [checkAntiSpam] for the shared rule itself, and
 * [CheckAntiSpamUseCase]'s docs for why this stays a small, separate,
 * Hilt-injectable class per vertical.
 */
class CheckRoommateAntiSpamUseCase @Inject constructor(
    private val repository: RoommateRepository,
) {
    suspend operator fun invoke(userId: String, acceptsMicroFee: Boolean): AntiSpamCheckResult =
        checkAntiSpam(repository, userId, acceptsMicroFee)
}
