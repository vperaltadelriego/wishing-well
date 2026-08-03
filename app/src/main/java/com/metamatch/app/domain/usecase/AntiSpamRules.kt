package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.exception.MicroFeeRequiredException
import com.metamatch.app.domain.model.ContractIntent
import com.metamatch.app.domain.repository.ContractRepository

/**
 * AntiSpamRules
 * ===============
 *
 * WHAT: the actual "5 free listings, then a micro-fee" rule from Module
 * 2, as one generic function every vertical's own anti-spam use case
 * delegates to — see [CheckAntiSpamUseCase] (Ride) and
 * `CheckPizzaAntiSpamUseCase` (Pizza).
 *
 * WHY this is a plain top-level function instead of a Hilt-injected
 * generic class: Hilt cannot automatically tell
 * `CheckAntiSpamUseCase<RideShareIntent>` and
 * `CheckAntiSpamUseCase<PizzaShareIntent>` apart without extra qualifier
 * annotations at every injection site — more ceremony than this rule is
 * worth. Instead, each vertical keeps its own small, concrete, Hilt-
 * friendly wrapper class, and both wrappers call this one function so
 * the actual rule is written exactly once. This is the same rule that
 * justified extracting [com.metamatch.app.domain.repository
 * .ContractRepository]: duplication is fine to tolerate once (Ride
 * alone), but a second real vertical (Pizza) is when it's worth removing.
 *
 * HOW it connects to the architecture: reads the live limits via
 * [ContractRepository.getPlatformPolicy] and the user's current count via
 * [ContractRepository.getActiveIntentsForUser] — never hard-coded, see
 * [com.metamatch.app.domain.policy.PlatformPolicy]'s own docs.
 *
 * @param repository Whichever vertical's repository the caller is
 *   checking against — the `*` wildcards mean "some `ContractIntent`
 *   subtype and some match-result type, I don't need to know which."
 * @param userId The user attempting to publish a new intent.
 * @param acceptsMicroFee Whether the user has already agreed, in the UI,
 *   to pay the micro-fee if one is required.
 * @return an [AntiSpamCheckResult] describing whether a fee applies, or
 *   throws [MicroFeeRequiredException] if [acceptsMicroFee] is `false`
 *   and a fee *is* required — see that exception's docs for why this
 *   case is modeled as a thrown exception rather than a return value.
 */
suspend fun checkAntiSpam(
    repository: ContractRepository<out ContractIntent, *>,
    userId: String,
    acceptsMicroFee: Boolean,
): AntiSpamCheckResult {
    val policy = repository.getPlatformPolicy()
    val activeCount = repository.getActiveIntentsForUser(userId).size

    val isWithinFreeLimit = activeCount < policy.freeActiveIntentLimit
    if (isWithinFreeLimit) {
        return AntiSpamCheckResult(requiresFee = false, feeAmountCents = 0, feeCurrency = policy.feeCurrency)
    }

    if (!acceptsMicroFee) {
        throw MicroFeeRequiredException(
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
