package com.metamatch.app.data.remote

import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.PizzaMatchResult
import com.metamatch.app.domain.model.PizzaShareIntent
import com.metamatch.app.domain.model.UserIntegrityScore
import com.metamatch.app.domain.model.UserRule
import com.metamatch.app.domain.policy.PlatformPolicy
import com.metamatch.app.domain.repository.PizzaShareRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SupabasePizzaShareRepository — INTENTIONALLY UNFINISHED (staged for a
 * later iteration)
 * ======================================================================
 *
 * WHAT this file is: the Pizza vertical's twin of
 * [SupabaseRideShareRepository] — a compiling skeleton that shows the
 * Clean Architecture boundary end-to-end before the real implementation
 * exists. See that class's own doc comment for the general steps to
 * finish a `Supabase*Repository`; the Pizza-specific table would be a new
 * `pizza_share_intents` table mirroring `PizzaShareIntent`'s fields (see
 * `schema.sql` once it's extended for this vertical).
 */
@Singleton
class SupabasePizzaShareRepository @Inject constructor() : PizzaShareRepository {

    override suspend fun getActiveIntentsForUser(userId: String): List<PizzaShareIntent> =
        TODO("Query postgrest['pizza_share_intents'] filtered by creator_user_id + status=ACTIVE.")

    override suspend fun getCandidateIntents(excludingUserId: String): List<PizzaShareIntent> =
        TODO("Query pizza_share_intents filtered by establishment/product, excluding excludingUserId.")

    override fun observeActiveIntentsForUser(userId: String): Flow<List<PizzaShareIntent>> =
        TODO("Subscribe via Supabase Realtime to pizza_share_intents filtered by creator_user_id.")

    override suspend fun publishIntent(intent: PizzaShareIntent): PizzaShareIntent =
        TODO("Insert a row into pizza_share_intents; map homeLocation to a PostGIS geography(Point,4326).")

    override suspend fun cancelIntent(intentId: String): Unit =
        TODO("Update pizza_share_intents.status = 'CANCELLED' where id = intentId, subject to RLS.")

    override suspend fun saveMatchResult(match: PizzaMatchResult): PizzaMatchResult =
        TODO("Insert into pizza_match_results and update every participant intent's status = 'MATCHED'.")

    override suspend fun formalizeContract(match: PizzaMatchResult, participants: List<PizzaShareIntent>): ContractRecord =
        TODO("Insert into contract_records (see schema.sql) capturing each participant's frozen terms.")

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
