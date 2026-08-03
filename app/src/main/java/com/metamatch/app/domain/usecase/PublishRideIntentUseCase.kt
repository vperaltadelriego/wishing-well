package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.RideShareIntent
import com.metamatch.app.domain.repository.RideShareRepository
import javax.inject.Inject

/**
 * PublishRideIntentUseCase
 * ==========================
 *
 * WHAT: the single entry point for "a user taps Publish on the Ride Share
 * form." Orchestrates the two things that have to happen, in order,
 * before an intent becomes visible to anyone else:
 *   1. Check the anti-spam / free-tier rule ([CheckAntiSpamUseCase]).
 *   2. Persist the intent ([RideShareRepository.publishIntent]).
 *
 * WHY orchestration belongs in a use case, not the ViewModel:
 * the ViewModel's job is to hold UI state and react to user actions — it
 * should not know the *business rule* that publishing can require a fee.
 * By moving that sequencing into a use case, the same logic is reusable
 * (e.g. from a future "quick re-publish" button) and independently
 * testable without instantiating any Compose/ViewModel machinery at all.
 * This is the classic Clean Architecture boundary: ViewModel → Use Case →
 * Repository, with dependencies only ever pointing inward.
 *
 * HOW it connects to the architecture
 * ------------------------------------
 * Constructed by Hilt (the `@Inject constructor` below) and called from
 * `PublishIntentViewModel`.
 */
class PublishRideIntentUseCase @Inject constructor(
    private val repository: RideShareRepository,
    private val checkAntiSpamUseCase: CheckAntiSpamUseCase,
) {
    /**
     * @param intent The fully-formed intent the user built in the Publish
     *   screen (already validated for required fields by the UI layer).
     * @param acceptsMicroFee Forwarded to [CheckAntiSpamUseCase] — pass
     *   `true` only after the user has explicitly confirmed the fee dialog.
     * @return the persisted [RideShareIntent] on success.
     * @throws com.metamatch.app.domain.exception.MicroFeeRequiredException
     *   if the user is over their free limit and [acceptsMicroFee] is
     *   `false`. The caller (ViewModel) is expected to catch this, show a
     *   confirmation dialog, and retry with `acceptsMicroFee = true`.
     */
    suspend operator fun invoke(
        intent: RideShareIntent,
        acceptsMicroFee: Boolean = false,
    ): RideShareIntent {
        // Step 1: enforce the free-tier / micro-fee rule. This line either
        // returns normally (limit respected, or fee accepted) or throws —
        // in both outcomes, execution only reaches Step 2 if publishing is
        // actually allowed to proceed.
        checkAntiSpamUseCase(userId = intent.creatorUserId, acceptsMicroFee = acceptsMicroFee)

        // Step 2: only now do we touch storage.
        return repository.publishIntent(intent)
    }
}
