package com.metamatch.app.domain.repository

import com.metamatch.app.domain.model.RoommateIntent
import com.metamatch.app.domain.model.RoommateMatchResult

/**
 * RoommateRepository
 * ====================
 *
 * WHAT: the single contract the Meta-Match Roomie vertical uses to read
 * and write sublease/lease data — every operation [ContractRepository]
 * defines, specialized to [RoommateIntent] and [RoommateMatchResult],
 * plus one Roomie-specific addition: [updateIntent].
 *
 * WHY [updateIntent] exists here and nowhere else: Ride and Pizza intents
 * never need to change after publishing — a rider or a pizza-sharer
 * either accepts a match as-is or cancels and republishes. Roomie's own
 * product requirement is different: after seeing a real candidate, a
 * landlord should be able to adjust their asking price (or any other
 * term) *in place* — "I asked 8,500 but this candidate has 8,000, let me
 * lower my offer" — without losing the listing's identity or re-running
 * anti-spam accounting as if it were a brand-new intent. This is the
 * concrete mechanism behind the "terms must stay editable after a match"
 * requirement (see project memory `roomie_pizza_requirements.md`).
 *
 * Two implementations exist, chosen by `di/RepositoryModule.kt`:
 * [com.metamatch.app.data.mock.MockRoommateRepository] (active today) and
 * `SupabaseRoommateRepository` (skeleton, staged for later).
 */
interface RoommateRepository : ContractRepository<RoommateIntent, RoommateMatchResult> {

    /**
     * Persists an already-published [intent] with new field values (e.g.
     * an adjusted [com.metamatch.app.domain.model.FinancialTerms.amount])
     * — same identity ([com.metamatch.app.domain.model.ContractIntent.id]),
     * new terms. Unlike [publishIntent], this never re-runs anti-spam
     * accounting: the listing already existed and already counted against
     * the free-tier limit.
     */
    suspend fun updateIntent(intent: RoommateIntent): RoommateIntent
}
