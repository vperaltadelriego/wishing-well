package com.metamatch.app.data.remote

import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.RoommateIntent
import com.metamatch.app.domain.model.RoommateMatchResult
import com.metamatch.app.domain.model.UserIntegrityScore
import com.metamatch.app.domain.model.UserRule
import com.metamatch.app.domain.policy.PlatformPolicy
import com.metamatch.app.domain.repository.RoommateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SupabaseRoommateRepository — INTENTIONALLY UNFINISHED (staged for a
 * later iteration)
 * ======================================================================
 *
 * WHAT this file is: the Roomie vertical's twin of
 * [SupabaseRideShareRepository]/`SupabasePizzaShareRepository` — a
 * compiling skeleton. See [SupabaseRideShareRepository]'s own doc comment
 * for the general steps to finish a `Supabase*Repository`; the
 * Roomie-specific table would be a new `roommate_intents` table
 * mirroring [RoommateIntent]'s fields.
 */
@Singleton
class SupabaseRoommateRepository @Inject constructor() : RoommateRepository {

    override suspend fun getActiveIntentsForUser(userId: String): List<RoommateIntent> =
        TODO("Query postgrest['roommate_intents'] filtered by creator_user_id + status=ACTIVE.")

    override suspend fun getCandidateIntents(excludingUserId: String): List<RoommateIntent> =
        TODO("Query roommate_intents filtered by zone/role, excluding excludingUserId.")

    override fun observeActiveIntentsForUser(userId: String): Flow<List<RoommateIntent>> =
        TODO("Subscribe via Supabase Realtime to roommate_intents filtered by creator_user_id.")

    override suspend fun publishIntent(intent: RoommateIntent): RoommateIntent =
        TODO("Insert a row into roommate_intents.")

    override suspend fun updateIntent(intent: RoommateIntent): RoommateIntent =
        TODO("Update the existing roommate_intents row by id, subject to RLS (owner-only).")

    override suspend fun cancelIntent(intentId: String): Unit =
        TODO("Update roommate_intents.status = 'CANCELLED' where id = intentId, subject to RLS.")

    override suspend fun saveMatchResult(match: RoommateMatchResult): RoommateMatchResult =
        TODO("Insert into roommate_match_results and update both participant intents' status = 'MATCHED'.")

    override suspend fun formalizeContract(match: RoommateMatchResult, participants: List<RoommateIntent>): ContractRecord =
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
