package com.metamatch.app.domain.model

/**
 * UserIntegrityScore
 * ====================
 *
 * WHAT: MetaMatch's reputation system — Module 1 of the brief. A single
 * number from 0.0 to 5.0 that answers "how likely is this person to
 * actually show up?", derived from how many of their past contracts were
 * [com.metamatch.app.domain.model.ContractStatus.FULFILLED] versus
 * [com.metamatch.app.domain.model.ContractStatus.CANCELLED].
 *
 * WHY it exists
 * -------------
 * A meta-matching platform lives or dies on trust: nobody wants to
 * coordinate a ride to the airport with a stranger who cancels half the
 * time. Rather than bolting a "rating" field onto every contract type
 * individually, integrity is modeled once, per user, in Module 1 — so it
 * automatically applies to ride shares today and to any future contract
 * type (study groups, item loans) without new code.
 *
 * HOW it connects to the architecture
 * ------------------------------------
 * - Updated whenever a [com.metamatch.app.domain.model.ContractIntent]
 *   transitions to `FULFILLED` or `CANCELLED` (see [recordFulfillment]
 *   and [recordCancellation]).
 * - Read by the "Integrity Rating Screen" (Module 3, later iteration) and
 *   by [com.metamatch.app.domain.usecase.FindMatchesUseCase], which can
 *   choose to deprioritize very low-integrity users as match candidates.
 * - Stored in Supabase as one row per user in the `user_integrity_scores`
 *   table (see `schema.sql`), updated by a `SECURITY DEFINER` SQL function
 *   so a malicious client can never write its own inflated score directly.
 *
 * @property userId Owner of this score.
 * @property completedMatches Number of contracts this user saw through to
 *   `FULFILLED`.
 * @property canceledMatches Number of contracts this user backed out of
 *   after being matched (`CANCELLED` *after* `MATCHED`, not before —
 *   canceling an intent that never found a match is not penalized).
 */
data class UserIntegrityScore(
    val userId: String,
    val completedMatches: Int = 0,
    val canceledMatches: Int = 0,
) {
    init {
        require(completedMatches >= 0) { "completedMatches cannot be negative." }
        require(canceledMatches >= 0) { "canceledMatches cannot be negative." }
    }

    /**
     * The 0.0–5.0 score itself, computed on demand rather than stored
     * redundantly. Kotlin note: this is a *computed property* (`get() =`
     * instead of a stored field) — it can never drift out of sync with
     * [completedMatches]/[canceledMatches] because it is recalculated
     * every time it is read.
     *
     * Brand-new users with zero history default to a neutral 4.0 rather
     * than 0.0 — a fresh account should not look untrustworthy just for
     * being new; it should look untrustworthy only after actually
     * canceling on people.
     */
    val score: Double
        get() {
            val totalResolved = completedMatches + canceledMatches
            if (totalResolved == 0) return 4.0
            val ratio = completedMatches.toDouble() / totalResolved
            return (ratio * 5.0).coerceIn(0.0, 5.0)
        }

    /** Convenience label used by the UI (e.g. to color-code a badge). */
    val trustLevel: TrustLevel
        get() = when {
            score >= 4.5 -> TrustLevel.EXCELLENT
            score >= 3.5 -> TrustLevel.GOOD
            score >= 2.0 -> TrustLevel.CAUTION
            else -> TrustLevel.LOW
        }

    /** Returns a *new* score reflecting one more fulfilled contract. */
    fun recordFulfillment(): UserIntegrityScore = copy(completedMatches = completedMatches + 1)

    /** Returns a *new* score reflecting one more post-match cancellation. */
    fun recordCancellation(): UserIntegrityScore = copy(canceledMatches = canceledMatches + 1)
}

enum class TrustLevel { EXCELLENT, GOOD, CAUTION, LOW }
