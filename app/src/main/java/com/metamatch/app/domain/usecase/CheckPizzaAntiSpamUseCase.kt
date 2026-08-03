package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.repository.PizzaShareRepository
import javax.inject.Inject

/**
 * CheckPizzaAntiSpamUseCase
 * ===========================
 *
 * WHAT: the Pizza vertical's entry point for the same free-tier/micro-fee
 * rule [CheckAntiSpamUseCase] enforces for Ride — see [checkAntiSpam] for
 * the shared rule itself, and [CheckAntiSpamUseCase]'s docs for why this
 * stays a small, separate, Hilt-injectable class per vertical instead of
 * one generic injected class.
 */
class CheckPizzaAntiSpamUseCase @Inject constructor(
    private val repository: PizzaShareRepository,
) {
    suspend operator fun invoke(userId: String, acceptsMicroFee: Boolean): AntiSpamCheckResult =
        checkAntiSpam(repository, userId, acceptsMicroFee)
}
