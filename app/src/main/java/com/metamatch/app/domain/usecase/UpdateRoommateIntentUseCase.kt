package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.RoommateIntent
import com.metamatch.app.domain.repository.RoommateRepository
import javax.inject.Inject

/**
 * UpdateRoommateIntentUseCase
 * =============================
 *
 * WHAT: the entry point for "a user adjusts an already-published Roomie
 * listing's terms after seeing a match" — e.g. lowering an asking price
 * from 8,500 to 8,000 once a good candidate turns up. A thin wrapper
 * around [RoommateRepository.updateIntent]; kept as its own use case,
 * rather than called directly from a ViewModel, to follow the same "one
 * use case per business-meaningful action" convention every other
 * publish/match action in this codebase follows — see that interface
 * method's own docs for why this operation exists only for Roomie.
 */
class UpdateRoommateIntentUseCase @Inject constructor(
    private val repository: RoommateRepository,
) {
    suspend operator fun invoke(intent: RoommateIntent): RoommateIntent = repository.updateIntent(intent)
}
