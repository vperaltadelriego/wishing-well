package com.metamatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * RoommateMatchResult
 * =====================
 *
 * WHAT: the output of
 * [com.metamatch.app.domain.usecase.FindRoommateMatchesUseCase] — one
 * [RoommateIntent] in [RoommateRole.SEEKING] paired with one in
 * [RoommateRole.OFFERING], compatible on zone and move-in window.
 *
 * WHY this is a flat pair, not a cluster like [MatchResult]/
 * [PizzaMatchResult]: Roomie's matching is inherently two-sided (see
 * [RoommateIntent]'s class doc) — there is no "group" to grow, only one
 * seeker and one offerer per result. A seeker with several compatible
 * listings simply gets several `RoommateMatchResult`s, the same way a
 * person browsing rentals sees a list of options rather than one merged
 * group.
 *
 * WHY price is never a filter, only a computed gap here: see
 * [FindRoommateMatchesUseCase]'s own docs — the explicit product
 * requirement is that a near-perfect match with a price gap (e.g. asking
 * 8,500, candidate has 8,000) must still surface as a match, with the gap
 * shown honestly, exactly like [MatchResult.meetsMinimumFare] and
 * [PizzaMatchResult.isFullyClaimed] are informational rather than
 * exclusionary.
 *
 * @property participantIntentIds Exactly two IDs: the seeker's intent
 *   and the offerer's intent, in that order.
 * @property zone Copied from the matched intents for display.
 * @property askingPrice The offering side's [FinancialTerms.amount].
 * @property seekerBudget The seeking side's [FinancialTerms.amount].
 * @property currency Currency shared by both sides.
 * @property createdAt When this match was computed.
 */
data class RoommateMatchResult(
    val id: String,
    val participantIntentIds: List<String>,
    val zone: String,
    val askingPrice: Double,
    val seekerBudget: Double,
    val currency: String,
    val createdAt: Instant,
) {
    init {
        require(participantIntentIds.size == 2) {
            "A Roomie match is exactly one seeker and one offerer; got ${participantIntentIds.size} participants."
        }
        require(askingPrice >= 0.0) { "askingPrice cannot be negative; got $askingPrice." }
    }

    /** How far apart the two sides are, as a percentage of the asking
     * price. Positive means the seeker's budget falls short; negative
     * means it exceeds the ask. Purely informational — never gates
     * whether this match is shown. `0.0` for a free ([askingPrice] = 0)
     * listing, where a percentage gap is meaningless. */
    val priceGapPercent: Double get() =
        if (askingPrice == 0.0) 0.0 else ((askingPrice - seekerBudget) / askingPrice) * 100.0

    /** Whether the seeker's budget already meets or exceeds the ask. */
    val isPriceAligned: Boolean get() = seekerBudget >= askingPrice
}
