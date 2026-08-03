package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.repository.RideShareRepository
import javax.inject.Inject

/**
 * CheckAntiSpamUseCase
 * ======================
 *
 * WHAT: implements the "Anti-Spam & Rate Limiting System" from Module 2 —
 * a user may have up to [com.metamatch.app.domain.policy.PlatformPolicy
 * .freeActiveIntentLimit] simultaneous free active intents; anything past
 * that requires a small micro-fee.
 *
 * WHY it is its own use case instead of a check inlined into
 * [PublishRideIntentUseCase]:
 * this rule is a self-contained business policy that could plausibly be
 * reused elsewhere (e.g. a future "extend my intent" action should
 * probably also respect the free-tier count). Kotlin/Clean-Architecture
 * convention is "one use case = one user-meaningful action or business
 * rule" — keeping this separate also makes it trivially unit-testable in
 * isolation, with a fake repository and no Android dependencies at all.
 *
 * HOW it connects to the architecture
 * ------------------------------------
 * Called by [PublishRideIntentUseCase] before every publish. Reads the
 * user's current active-intent count via [RideShareRepository
 * .getActiveIntentsForUser] and the live limits via [RideShareRepository
 * .getPlatformPolicy] (never hard-coded — see that function's docs).
 *
 * Kotlin note: this class defines `operator fun invoke(...)`. That special
 * function name lets callers write `checkAntiSpamUseCase(userId)` as if
 * the use-case object itself were a function — a common Kotlin idiom for
 * single-responsibility use-case classes that keeps call sites terse
 * without hiding what is actually happening (this is still a real class
 * with real dependencies, just a nicer calling syntax).
 */
class CheckAntiSpamUseCase @Inject constructor(
    private val repository: RideShareRepository,
) {
    /**
     * @param userId The user attempting to publish a new intent.
     * @param acceptsMicroFee Whether the user has already agreed, in the
     *   UI, to pay the micro-fee if one is required. Defaults to `false`
     *   so the very first attempt always surfaces the fee requirement
     *   rather than silently charging money.
     * @return an [AntiSpamCheckResult] describing whether a fee applies
     *   and, if [acceptsMicroFee] is `false` and one *is* required, throws
     *   [com.metamatch.app.domain.exception.MicroFeeRequiredException]
     *   instead of returning — see that exception's docs for why this
     *   case is modeled as a thrown exception rather than a return value.
     */
    suspend operator fun invoke(userId: String, acceptsMicroFee: Boolean): AntiSpamCheckResult {
        val policy = repository.getPlatformPolicy()
        val activeCount = repository.getActiveIntentsForUser(userId).size

        val isWithinFreeLimit = activeCount < policy.freeActiveIntentLimit
        if (isWithinFreeLimit) {
            return AntiSpamCheckResult(requiresFee = false, feeAmountCents = 0, feeCurrency = policy.feeCurrency)
        }

        if (!acceptsMicroFee) {
            throw com.metamatch.app.domain.exception.MicroFeeRequiredException(
                currentActiveCount = activeCount,
                freeLimit = policy.freeActiveIntentLimit,
                feeAmountCents = policy.microFeeAmountCents,
                feeCurrency = policy.feeCurrency,
            )
        }

        return AntiSpamCheckResult(
            requiresFee = true,
            feeAmountCents = policy.microFeeAmountCents,
            feeCurrency = policy.feeCurrency,
        )
    }
}

/**
 * Outcome of an anti-spam check that did *not* need to throw — either the
 * user is comfortably under their free limit ([requiresFee] = false), or
 * they were over it and already agreed to pay ([requiresFee] = true, with
 * the amount that will be charged).
 */
data class AntiSpamCheckResult(
    val requiresFee: Boolean,
    val feeAmountCents: Int,
    val feeCurrency: String,
)
