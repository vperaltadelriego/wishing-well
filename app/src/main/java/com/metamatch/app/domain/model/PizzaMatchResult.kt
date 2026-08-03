package com.metamatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * PizzaMatchResult
 * ==================
 *
 * WHAT: the output of
 * [com.metamatch.app.domain.usecase.FindPizzaMatchesUseCase] — two or
 * more [PizzaShareIntent]s the engine believes should split one order
 * together, plus the proof of *why* (pickup point, units claimed).
 *
 * WHY this is its own type instead of reusing [MatchResult]: a pizza
 * match has no "estimated fare" to check a budget against — its
 * equivalent question is "how much of the order has this group actually
 * claimed?" ([isFullyClaimed]), which is a different shape of fact than
 * [MatchResult.meetsMinimumFare]. Forcing both verticals through one
 * shared result type would mean one of them carries fields that make no
 * sense for it; two small, honest types are clearer than one overloaded
 * one — the same reasoning that gave [RideShareIntent] and
 * [PizzaShareIntent] separate model classes in the first place.
 *
 * @property participantIntentIds See [MatchResult.participantIntentIds] — same role.
 * @property pickupPoint The fair meeting point to collect the shared
 *   order — the geometric centroid of every participant's
 *   [PizzaShareIntent.homeLocation], via [GeoPoint.centroidOf]. The
 *   direct analogue of [MatchResult.meetingPoint].
 * @property establishment Copied from the matched intents for display,
 *   so the UI never has to re-look-up the original intent just to show
 *   "Domino's" on the match card.
 * @property productDescription Same reasoning as [establishment].
 * @property totalUnitsClaimed Sum of every participant's
 *   [PizzaShareIntent.desiredUnits] in this match.
 * @property totalUnitsAvailable The whole order's size
 *   ([PizzaShareIntent.totalUnits]), copied from whichever intent seeded
 *   this cluster.
 * @property totalContribution Sum of every participant's
 *   [FinancialTerms.amount] — the pooled amount they'll collectively pay.
 * @property currency Currency shared by every participant in this match.
 * @property createdAt When this match was computed.
 */
data class PizzaMatchResult(
    val id: String,
    val participantIntentIds: List<String>,
    val pickupPoint: GeoPoint,
    val establishment: String,
    val productDescription: String,
    val totalUnitsClaimed: Int,
    val totalUnitsAvailable: Int,
    val totalContribution: Double,
    val currency: String,
    val createdAt: Instant,
) {
    init {
        require(participantIntentIds.size >= 2) {
            "A match requires at least 2 participants; got ${participantIntentIds.size}."
        }
    }

    /** Whether this group has claimed the entire order — informational
     * only, the same way [MatchResult.meetsMinimumFare] never blocks a
     * Ride match from being shown or accepted. */
    val isFullyClaimed: Boolean get() = totalUnitsClaimed >= totalUnitsAvailable

    /** How many units of the order remain unclaimed (0 if fully claimed). */
    val unitsRemaining: Int get() = (totalUnitsAvailable - totalUnitsClaimed).coerceAtLeast(0)
}
