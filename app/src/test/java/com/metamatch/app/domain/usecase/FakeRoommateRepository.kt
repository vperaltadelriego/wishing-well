package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.ContractPartySnapshot
import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.ContractType
import com.metamatch.app.domain.model.RoommateIntent
import com.metamatch.app.domain.model.RoommateMatchResult
import com.metamatch.app.domain.model.UserIntegrityScore
import com.metamatch.app.domain.model.UserRule
import com.metamatch.app.domain.policy.PlatformPolicy
import com.metamatch.app.domain.repository.RoommateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * FakeRoommateRepository
 * ========================
 *
 * WHAT: a minimal, test-only implementation of [RoommateRepository] —
 * the Roomie vertical's twin of [FakeRideShareRepository]/
 * [FakePizzaShareRepository]. Also implements [updateIntent], so tests
 * can exercise the price-renegotiation flow without a real backend.
 */
class FakeRoommateRepository(
    initialIntents: List<RoommateIntent> = emptyList(),
    private var policy: PlatformPolicy = PlatformPolicy.default(),
) : RoommateRepository {

    private val intentsFlow = MutableStateFlow(initialIntents)

    override suspend fun getActiveIntentsForUser(userId: String): List<RoommateIntent> =
        intentsFlow.value.filter { it.creatorUserId == userId }

    override suspend fun getCandidateIntents(excludingUserId: String): List<RoommateIntent> =
        intentsFlow.value.filter { it.creatorUserId != excludingUserId }

    override fun observeActiveIntentsForUser(userId: String) =
        intentsFlow.map { all -> all.filter { it.creatorUserId == userId } }

    override suspend fun publishIntent(intent: RoommateIntent): RoommateIntent {
        intentsFlow.value = intentsFlow.value + intent
        return intent
    }

    override suspend fun updateIntent(intent: RoommateIntent): RoommateIntent {
        intentsFlow.value = intentsFlow.value.map { if (it.id == intent.id) intent else it }
        return intent
    }

    override suspend fun cancelIntent(intentId: String) {
        intentsFlow.value = intentsFlow.value.filterNot { it.id == intentId }
    }

    override suspend fun saveMatchResult(match: RoommateMatchResult): RoommateMatchResult = match

    override suspend fun formalizeContract(
        match: RoommateMatchResult,
        participants: List<RoommateIntent>,
    ): ContractRecord = ContractRecord(
        id = "contract-${match.id}",
        matchResultId = match.id,
        contractType = ContractType.ROOMMATE_SEARCH,
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
        totalContribution = match.askingPrice,
    )

    override suspend fun getContractRecord(matchResultId: String): ContractRecord? = null

    override suspend fun getUserRules(userId: String): List<UserRule> = emptyList()

    override suspend fun getIntegrityScore(userId: String): UserIntegrityScore =
        UserIntegrityScore(userId = userId)

    override suspend fun recordFulfillment(userId: String) = Unit

    override suspend fun recordCancellationAfterMatch(userId: String) = Unit

    override suspend fun getPlatformPolicy(): PlatformPolicy = policy
}
