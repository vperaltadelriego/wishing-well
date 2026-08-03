package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.RoommateIntent
import com.metamatch.app.domain.repository.RoommateRepository
import javax.inject.Inject

/**
 * PublishRoommateIntentUseCase
 * ==============================
 *
 * WHAT: the Roomie vertical's version of [PublishRideIntentUseCase]/
 * [PublishPizzaIntentUseCase] — the single entry point for "a user taps
 * Publish on the Roomie form." Same two-step orchestration: check the
 * anti-spam rule, then persist.
 */
class PublishRoommateIntentUseCase @Inject constructor(
    private val repository: RoommateRepository,
    private val checkRoommateAntiSpamUseCase: CheckRoommateAntiSpamUseCase,
) {
    suspend operator fun invoke(
        intent: RoommateIntent,
        acceptsMicroFee: Boolean = false,
    ): RoommateIntent {
        checkRoommateAntiSpamUseCase(userId = intent.creatorUserId, acceptsMicroFee = acceptsMicroFee)
        return repository.publishIntent(intent)
    }
}
