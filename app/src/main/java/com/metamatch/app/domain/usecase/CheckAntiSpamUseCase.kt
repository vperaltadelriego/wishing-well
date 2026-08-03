package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.repository.RideShareRepository
import javax.inject.Inject

/**
 * CheckAntiSpamUseCase
 * ======================
 *
 * WHAT: the Ride vertical's entry point for Module 2's "Anti-Spam & Rate
 * Limiting System" — a thin, Hilt-injectable wrapper around the actual
 * rule in [checkAntiSpam] (see that function's own docs for why the rule
 * itself lives there and not here).
 *
 * WHY it is still its own tiny class instead of every caller invoking
 * [checkAntiSpam] directly: keeping one small class per vertical
 * (`CheckAntiSpamUseCase` here, `CheckPizzaAntiSpamUseCase` for Pizza)
 * means Hilt can inject each one by its own concrete
 * [RideShareRepository]/`PizzaShareRepository` type with zero extra
 * configuration, and call sites keep the familiar
 * `checkAntiSpamUseCase(userId, acceptsMicroFee)` calling convention.
 *
 * Kotlin note: `operator fun invoke(...)` lets callers write
 * `checkAntiSpamUseCase(userId, ...)` as if the use-case object itself
 * were a function.
 */
class CheckAntiSpamUseCase @Inject constructor(
    private val repository: RideShareRepository,
) {
    suspend operator fun invoke(userId: String, acceptsMicroFee: Boolean): AntiSpamCheckResult =
        checkAntiSpam(repository, userId, acceptsMicroFee)
}
