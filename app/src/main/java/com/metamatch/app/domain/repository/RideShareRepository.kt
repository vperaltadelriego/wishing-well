package com.metamatch.app.domain.repository

import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.MatchResult
import com.metamatch.app.domain.model.RideShareIntent
import com.metamatch.app.domain.model.UserIntegrityScore
import com.metamatch.app.domain.model.UserRule
import com.metamatch.app.domain.policy.PlatformPolicy
import kotlinx.coroutines.flow.Flow

/**
 * RideShareRepository
 * =====================
 *
 * WHAT: the single contract the rest of the app uses to read and write
 * ride-share data. It says *what* operations exist, never *how* they are
 * carried out.
 *
 * WHY it exists (the Repository pattern + the "RepositoryStrategy toggle")
 * -----------------------------------------------------------------------
 * This is the seam the whole "run standalone in Android Studio, deploy
 * against real Supabase later" requirement hangs on. ViewModels and use
 * cases are written once, against this interface, and never import a
 * concrete class directly. Two implementations exist:
 *
 *   - [com.metamatch.app.data.mock.MockRideShareRepository] — pure
 *     in-memory Kotlin, zero network calls, seeded with realistic sample
 *     data. This is what makes the app buildable and demoable the moment
 *     it is cloned, with no Supabase project, no API keys, nothing.
 *   - `SupabaseRideShareRepository` (stub in this pass, fully wired in a
 *     later iteration) — talks to Postgres/PostGIS/Realtime over the
 *     Supabase Kotlin SDK, using the schema in `schema.sql`.
 *
 * Hilt's `di/RepositoryModule.kt` decides, in exactly one place, which of
 * the two gets constructed — everything else in the app is unaffected by
 * that choice. This is the Dependency Inversion Principle (the "D" in
 * SOLID) in practice: high-level code (use cases) depends on an
 * abstraction (this interface), and low-level code (the two repository
 * classes) depends on that same abstraction too, instead of high-level
 * code depending directly on low-level details.
 *
 * Kotlin note: every function here is `suspend`. That keyword marks a
 * function as *pausable* — it can do slow work (a network call, a
 * database query) without blocking the thread it was called from. Calling
 * a `suspend fun` from a ViewModel inside `viewModelScope.launch { }` is
 * how Compose UIs stay responsive while data loads.
 */
interface RideShareRepository {

    /**
     * All currently [com.metamatch.app.domain.model.ContractStatus.ACTIVE]
     * intents belonging to [userId]. Used by
     * [com.metamatch.app.domain.usecase.CheckAntiSpamUseCase] to count
     * against the free-tier limit, and by the Publish screen to render the
     * "X / 5 free" indicator.
     */
    suspend fun getActiveIntentsForUser(userId: String): List<RideShareIntent>

    /**
     * Every other active intent in the system that is *not* owned by
     * [excludingUserId] — the raw candidate pool
     * [com.metamatch.app.domain.usecase.FindMatchesUseCase] searches for
     * compatible matches. In the real Supabase implementation this maps to
     * a PostGIS-accelerated query (see `schema.sql`), not a full table
     * scan; the mock implementation simply filters an in-memory list.
     */
    suspend fun getCandidateIntents(excludingUserId: String): List<RideShareIntent>

    /**
     * A live stream of the current user's active intents, so the Publish
     * screen's "X / 5 free" counter updates immediately after a publish
     * or a cancellation without the caller manually re-fetching.
     *
     * Kotlin note: [Flow] is Kotlin's asynchronous stream type — think of
     * it as a `suspend` function that can emit *many* values over time
     * instead of just one. Compose's `collectAsState()` turns a `Flow`
     * directly into Compose UI state.
     */
    fun observeActiveIntentsForUser(userId: String): Flow<List<RideShareIntent>>

    /**
     * Persists a brand-new intent. Returns the stored intent (which may
     * differ slightly from the input, e.g. once the backend assigns a
     * canonical server-side timestamp).
     */
    suspend fun publishIntent(intent: RideShareIntent): RideShareIntent

    /**
     * Marks an intent as
     * [com.metamatch.app.domain.model.ContractStatus.CANCELLED]. Does
     * *not* update [UserIntegrityScore] here — that only happens for
     * intents that had already reached
     * [com.metamatch.app.domain.model.ContractStatus.MATCHED] (see
     * [recordFulfillment] / [recordCancellationAfterMatch]).
     */
    suspend fun cancelIntent(intentId: String)

    /** Persists a computed [MatchResult] once
     * [com.metamatch.app.domain.usecase.FindMatchesUseCase] finds one. */
    suspend fun saveMatchResult(match: MatchResult): MatchResult

    /**
     * Freezes [match] plus the full [RideShareIntent] of every participant
     * into a [ContractRecord] — called once, right after a user accepts a
     * match. See [ContractRecord]'s own documentation for why this data is
     * captured even though this MVP never renders it as a real document.
     */
    suspend fun formalizeContract(match: MatchResult, participants: List<RideShareIntent>): ContractRecord

    /** The [ContractRecord] previously produced for [matchResultId], if
     * that match has been accepted; `null` if it has not (yet). */
    suspend fun getContractRecord(matchResultId: String): ContractRecord?

    /** All [UserRule]s that apply to [userId] — their own blacklist plus
     * any organization-level allow-list they inherit. */
    suspend fun getUserRules(userId: String): List<UserRule>

    /** Current reputation snapshot for [userId]. */
    suspend fun getIntegrityScore(userId: String): UserIntegrityScore

    /** Records that [userId] successfully completed a matched contract. */
    suspend fun recordFulfillment(userId: String)

    /** Records that [userId] canceled a contract *after* being matched. */
    suspend fun recordCancellationAfterMatch(userId: String)

    /**
     * The current, live business-rule configuration (free-tier limit,
     * micro-fee amount, verification threshold). Backed by hard-coded
     * defaults in the mock repository and by the `platform_policy` table
     * in the Supabase implementation — see [PlatformPolicy]'s own
     * documentation for why this is fetched rather than hard-coded at
     * every call site.
     */
    suspend fun getPlatformPolicy(): PlatformPolicy
}
