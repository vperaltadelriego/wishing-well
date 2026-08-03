package com.metamatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * MatchResult
 * ============
 *
 * WHAT: the output of [com.metamatch.app.domain.usecase.FindMatchesUseCase]
 * — two or more [RideShareIntent]s that the engine believes should share a
 * ride, plus the geometry/finance proof of *why* they were grouped
 * together.
 *
 * WHY it exists
 * -------------
 * The matching decision involves several pieces of derived data (a
 * centroid point, a total budget, whether that budget clears the
 * estimated fare) that are expensive to recompute every time the UI wants
 * to show them. Bundling them into one immutable snapshot means the
 * "Match Results" screen can just display `matchResult.meetsMinimumFare`
 * instead of re-running geometry math inside a Composable.
 *
 * HOW it connects to the architecture
 * ------------------------------------
 * - Produced by [com.metamatch.app.domain.usecase.FindMatchesUseCase]
 *   using the centroid/budget rules described there (a Kotlin mirror of
 *   the `ST_Centroid`/`ST_DWithin` PostGIS logic in `schema.sql`).
 * - Consumed by the "Match Results / Active Offers" Compose screen.
 * - Once a match is *accepted* by every participant, it becomes the seed
 *   for Module 3's visual meeting ticket (e.g. "Yellow Circle #37") and
 *   the in-app chat — not implemented in this MVP pass, but this is the
 *   object that hand-off will attach to.
 *
 * @property id Unique identifier for this proposed/confirmed match.
 * @property participantIntentIds The [RideShareIntent.id]s being grouped
 *   together. Kept as IDs (not embedded whole objects) so a `MatchResult`
 *   stays small and serializable even if the underlying intents change.
 * @property meetingPoint The fair meeting point — the geometric centroid
 *   of every participant's destination, per [GeoPoint.centroidOf].
 * @property totalContribution Sum of every participant's
 *   [FinancialTerms.amount], in the currency of [currency].
 * @property currency Currency code shared by all participants in this
 *   match (matching is only attempted between intents that already agree
 *   on currency — see [com.metamatch.app.domain.usecase.FindMatchesUseCase]).
 * @property estimatedFare The platform's best estimate of what a
 *   Taxi/Uber/Didi would cost for this trip. Compared against
 *   [totalContribution] to decide [meetsMinimumFare].
 * @property createdAt When this match was computed.
 */
data class MatchResult(
    val id: String,
    val participantIntentIds: List<String>,
    val meetingPoint: GeoPoint,
    val totalContribution: Double,
    val currency: String,
    val estimatedFare: Double,
    val createdAt: Instant,
) {
    init {
        require(participantIntentIds.size >= 2) {
            "A match requires at least 2 participants; got ${participantIntentIds.size}."
        }
    }

    /**
     * Whether the group's pooled budget is enough to actually pay for the
     * ride. This is the Kotlin-side equivalent of the SQL check described
     * in the brief: "verify that total aggregated contributions... meet or
     * exceed the minimum estimated fare."
     */
    val meetsMinimumFare: Boolean get() = totalContribution >= estimatedFare

    /** How much the group is over (positive) or short (negative) of the fare. */
    val budgetSurplus: Double get() = totalContribution - estimatedFare
}
