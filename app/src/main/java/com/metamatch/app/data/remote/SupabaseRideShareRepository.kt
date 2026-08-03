package com.metamatch.app.data.remote

import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.MatchResult
import com.metamatch.app.domain.model.RideShareIntent
import com.metamatch.app.domain.model.UserIntegrityScore
import com.metamatch.app.domain.model.UserRule
import com.metamatch.app.domain.policy.PlatformPolicy
import com.metamatch.app.domain.repository.RideShareRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SupabaseRideShareRepository — INTENTIONALLY UNFINISHED (staged for the
 * next iteration)
 * ======================================================================
 *
 * WHAT this file is: the *other half* of the RepositoryStrategy toggle
 * described in [RideShareRepository]'s documentation, and in the project
 * README's roadmap. It exists now, as a compiling skeleton, so:
 *   1. The Clean Architecture boundary is visible end-to-end from day one
 *      — anyone reading the `data/` package sees both implementations
 *      side by side, even before the real one is finished.
 *   2. Wiring it up later is a matter of *filling in* these methods and
 *      flipping one line in `di/RepositoryModule.kt`, never touching the
 *      domain layer, use cases, or UI.
 *
 * WHY it is not fully implemented in this MVP pass: per the project's
 * staged learning plan, this first iteration focuses on Module 1 (core
 * engine) and Module 2 (ride share matching) running entirely on
 * [com.metamatch.app.data.mock.MockRideShareRepository]. Wiring real
 * Supabase Auth/Postgres/Realtime calls is deliberately deferred to the
 * next iteration together with Module 3 (chat/handshake) and Module 4
 * (monetization), so each new piece of Kotlin can be learned and reviewed
 * on its own instead of arriving all at once.
 *
 * HOW to finish this class (for the next iteration):
 * 1. Add the Supabase Kotlin SDK + Ktor engine to `app/build.gradle.kts`:
 *    ```kotlin
 *    implementation(platform("io.github.jan-tennert.supabase:bom:<latest>"))
 *    implementation("io.github.jan-tennert.supabase:postgrest-kt")
 *    implementation("io.github.jan-tennert.supabase:realtime-kt")
 *    implementation("io.github.jan-tennert.supabase:auth-kt")
 *    implementation("io.ktor:ktor-client-android:<latest>")
 *    ```
 *    (Check the current stable versions on Maven Central before adding —
 *    they move quickly.)
 * 2. Inject a configured `SupabaseClient` here instead of the empty
 *    constructor below.
 * 3. Implement each method as a `postgrest["ride_share_intents"]` query
 *    (see `schema.sql` for exact table/column names) or a `channel(...)`
 *    Realtime subscription for [observeActiveIntentsForUser].
 * 4. In `di/RepositoryModule.kt`, change the `@Binds` target from
 *    `MockRideShareRepository` to this class — that one-line change is
 *    the entire migration; nothing else in the app needs to know.
 *
 * Kotlin note: every method below calls `TODO("...")`. `TODO()` is a
 * standard-library function with return type `Nothing`, which means the
 * Kotlin compiler accepts it as a valid (temporary) body for a function
 * declared to return *any* type — this class compiles and satisfies the
 * `RideShareRepository` interface today, it simply throws
 * `NotImplementedError` with a helpful message if actually called before
 * this class is finished.
 */
@Singleton
class SupabaseRideShareRepository @Inject constructor() : RideShareRepository {

    override suspend fun getActiveIntentsForUser(userId: String): List<RideShareIntent> =
        TODO("Query postgrest['ride_share_intents'] filtered by creator_user_id + status=ACTIVE.")

    override suspend fun getCandidateIntents(excludingUserId: String): List<RideShareIntent> =
        TODO("Run the PostGIS-accelerated candidate query described in schema.sql's find_candidate_intents() function.")

    override fun observeActiveIntentsForUser(userId: String): Flow<List<RideShareIntent>> =
        TODO("Subscribe via Supabase Realtime to the ride_share_intents table filtered by creator_user_id.")

    override suspend fun publishIntent(intent: RideShareIntent): RideShareIntent =
        TODO("Insert a row into ride_share_intents; map lat/lng to a PostGIS geography(Point,4326).")

    override suspend fun cancelIntent(intentId: String): Unit =
        TODO("Update ride_share_intents.status = 'CANCELLED' where id = intentId, subject to RLS.")

    override suspend fun saveMatchResult(match: MatchResult): MatchResult =
        TODO("Insert into match_results and update every participant intent's status = 'MATCHED'.")

    override suspend fun formalizeContract(match: MatchResult, participants: List<RideShareIntent>): ContractRecord =
        TODO("Insert into a contract_records table (see schema.sql) capturing each participant's frozen terms.")

    override suspend fun getContractRecord(matchResultId: String): ContractRecord? =
        TODO("Query contract_records by match_result_id.")

    override suspend fun getUserRules(userId: String): List<UserRule> =
        TODO("Query the user_rules table for rows owned by userId or by their organization.")

    override suspend fun getIntegrityScore(userId: String): UserIntegrityScore =
        TODO("Query user_integrity_scores; create a default row via the on-signup SQL trigger if missing.")

    override suspend fun recordFulfillment(userId: String): Unit =
        TODO("Call the increment_completed_matches() SECURITY DEFINER SQL function from schema.sql.")

    override suspend fun recordCancellationAfterMatch(userId: String): Unit =
        TODO("Call the increment_canceled_matches() SECURITY DEFINER SQL function from schema.sql.")

    override suspend fun getPlatformPolicy(): PlatformPolicy =
        TODO("Query the single-row platform_policy config table described in schema.sql.")
}
