package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.ContractPartySnapshot
import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.ContractType
import com.metamatch.app.domain.model.MatchResult
import com.metamatch.app.domain.model.RideShareIntent
import com.metamatch.app.domain.model.UserIntegrityScore
import com.metamatch.app.domain.model.UserRule
import kotlinx.datetime.Clock
import com.metamatch.app.domain.policy.PlatformPolicy
import com.metamatch.app.domain.repository.RideShareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * FakeRideShareRepository
 * ==========================
 *
 * WHAT: a minimal, test-only implementation of [RideShareRepository],
 * backed by a plain in-memory list the test itself controls.
 *
 * WHY a *third* implementation, alongside Mock (demo) and Supabase
 * (production): this is the payoff of programming to the
 * [RideShareRepository] interface everywhere. A "fake" built purely for
 * tests can seed *exactly* the scenario one test cares about (e.g. "two
 * intents 50 meters apart, one over budget") without depending on
 * whatever sample data happens to live in
 * [com.metamatch.app.data.mock.MockRideShareRepository], which is free to
 * change for demo purposes without ever breaking a unit test.
 */
class FakeRideShareRepository(
    initialIntents: List<RideShareIntent> = emptyList(),
    private var policy: PlatformPolicy = PlatformPolicy.default(),
) : RideShareRepository {

    private val intentsFlow = MutableStateFlow(initialIntents)

    override suspend fun getActiveIntentsForUser(userId: String): List<RideShareIntent> =
        intentsFlow.value.filter { it.creatorUserId == userId }

    override suspend fun getCandidateIntents(excludingUserId: String): List<RideShareIntent> =
        intentsFlow.value.filter { it.creatorUserId != excludingUserId }

    override fun observeActiveIntentsForUser(userId: String) =
        intentsFlow.map { all -> all.filter { it.creatorUserId == userId } }

    override suspend fun publishIntent(intent: RideShareIntent): RideShareIntent {
        intentsFlow.value = intentsFlow.value + intent
        return intent
    }

    override suspend fun cancelIntent(intentId: String) {
        intentsFlow.value = intentsFlow.value.filterNot { it.id == intentId }
    }

    override suspend fun saveMatchResult(match: MatchResult): MatchResult = match

    override suspend fun formalizeContract(
        match: MatchResult,
        participants: List<RideShareIntent>,
    ): ContractRecord = ContractRecord(
        id = "contract-${match.id}",
        matchResultId = match.id,
        contractType = ContractType.RIDE_SHARE,
        participants = participants.map { intent ->
            ContractPartySnapshot(
                userId = intent.creatorUserId,
                email = intent.creatorEmail,
                financialTerms = intent.financialTerms,
                verificationTier = intent.verificationTier,
                legalConsentAcknowledgedAt = intent.legalConsentAcknowledgedAt,
            )
        },
        formalizedAt = Clock.System.now(),
        currency = match.currency,
        totalContribution = match.totalContribution,
    )

    override suspend fun getContractRecord(matchResultId: String): ContractRecord? = null

    override suspend fun getUserRules(userId: String): List<UserRule> = emptyList()

    override suspend fun getIntegrityScore(userId: String): UserIntegrityScore =
        UserIntegrityScore(userId = userId)

    override suspend fun recordFulfillment(userId: String) = Unit

    override suspend fun recordCancellationAfterMatch(userId: String) = Unit

    override suspend fun getPlatformPolicy(): PlatformPolicy = policy
}
