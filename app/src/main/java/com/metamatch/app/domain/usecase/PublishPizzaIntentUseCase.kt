package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.PizzaShareIntent
import com.metamatch.app.domain.repository.PizzaShareRepository
import javax.inject.Inject

/**
 * PublishPizzaIntentUseCase
 * ===========================
 *
 * WHAT: the Pizza vertical's version of
 * [PublishRideIntentUseCase] — the single entry point for "a user taps
 * Publish on the Pizza form." Same two-step orchestration: check the
 * anti-spam rule, then persist.
 *
 * WHY this mirrors [PublishRideIntentUseCase] almost line for line: the
 * orchestration (check limit, then publish) has nothing to do with which
 * vertical is involved — only the concrete intent/repository types
 * differ. See that class's own docs for the fuller rationale.
 */
class PublishPizzaIntentUseCase @Inject constructor(
    private val repository: PizzaShareRepository,
    private val checkPizzaAntiSpamUseCase: CheckPizzaAntiSpamUseCase,
) {
    suspend operator fun invoke(
        intent: PizzaShareIntent,
        acceptsMicroFee: Boolean = false,
    ): PizzaShareIntent {
        checkPizzaAntiSpamUseCase(userId = intent.creatorUserId, acceptsMicroFee = acceptsMicroFee)
        return repository.publishIntent(intent)
    }
}
