package com.metamatch.app.data.mock

import com.metamatch.app.domain.model.ContractPartySnapshot
import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.ContractStatus
import com.metamatch.app.domain.model.ContractType
import com.metamatch.app.domain.model.FinancialTerms
import com.metamatch.app.domain.model.IdentityVerificationTier
import com.metamatch.app.domain.model.RoommateIntent
import com.metamatch.app.domain.model.RoommateMatchResult
import com.metamatch.app.domain.model.RoommateRole
import com.metamatch.app.domain.model.UserIntegrityScore
import com.metamatch.app.domain.model.UserRule
import com.metamatch.app.domain.policy.PlatformPolicy
import com.metamatch.app.domain.repository.RoommateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

/**
 * MockRoommateRepository
 * =========================
 *
 * WHAT: an in-memory, zero-network implementation of
 * [RoommateRepository] — the Roomie vertical's twin of
 * [MockRideShareRepository]/[MockPizzaShareRepository].
 *
 * WHY the seed data looks the way it does: [seedIntents] includes one
 * [RoommateRole.OFFERING] listing asking 8,500 MXN in a zone that matches
 * the Publish screen's own defaults — the exact "asking 8,500, candidate
 * has 8,000" scenario from the product brief. Accepting every default on
 * the Publish screen (a [RoommateRole.SEEKING] listing with an 8,000 MXN
 * budget) produces an instant match here that is compatible but *not*
 * price-aligned, ready for the "Adjust My Price" action to resolve live —
 * the same guided-demo property Ride and Pizza have, extended to also
 * demonstrate the price-negotiation feature this vertical is built
 * around.
 */
@Singleton
class MockRoommateRepository @Inject constructor() : RoommateRepository {

    companion object {
        /** "Today" for the seeded listing's move-in window — computed at
         * runtime for the same timezone-safety reason as
         * [MockRideShareRepository.DEMO_RIDE_SCHEDULED_AT]. */
        val DEMO_MOVE_IN_WINDOW_START: Instant = Clock.System.now()
        val DEMO_MOVE_IN_WINDOW_END: Instant = DEMO_MOVE_IN_WINDOW_START.plus(60.days)
    }

    private val pilotCommunityDomains = setOf("ucaribe.edu.mx", "anahuac.mx", "xcaret.com")

    private val intentsFlow = MutableStateFlow(seedIntents())
    private val matchResultsStore = mutableListOf<RoommateMatchResult>()
    private val contractRecordsStore = mutableMapOf<String, ContractRecord>()
    private val userRulesStore = mutableMapOf<String, List<UserRule>>()
    private val integrityScoresStore = mutableMapOf<String, UserIntegrityScore>()
    private var platformPolicy = PlatformPolicy.default()

    override suspend fun getActiveIntentsForUser(userId: String): List<RoommateIntent> =
        intentsFlow.value.filter { it.creatorUserId == userId && it.status == ContractStatus.ACTIVE }

    override suspend fun getCandidateIntents(excludingUserId: String): List<RoommateIntent> =
        intentsFlow.value.filter {
            it.creatorUserId != excludingUserId && it.status == ContractStatus.ACTIVE
        }

    override fun observeActiveIntentsForUser(userId: String): Flow<List<RoommateIntent>> =
        intentsFlow.map { all -> all.filter { it.creatorUserId == userId && it.status == ContractStatus.ACTIVE } }

    override suspend fun publishIntent(intent: RoommateIntent): RoommateIntent {
        intentsFlow.update { current -> current + intent }
        return intent
    }

    override suspend fun updateIntent(intent: RoommateIntent): RoommateIntent {
        intentsFlow.update { current ->
            current.map { if (it.id == intent.id) intent else it }
        }
        return intent
    }

    override suspend fun cancelIntent(intentId: String) {
        intentsFlow.update { current ->
            current.map { if (it.id == intentId) it.copy(status = ContractStatus.CANCELLED) else it }
        }
    }

    override suspend fun saveMatchResult(match: RoommateMatchResult): RoommateMatchResult {
        matchResultsStore += match
        intentsFlow.update { current ->
            current.map { intent ->
                if (intent.id in match.participantIntentIds) intent.copy(status = ContractStatus.MATCHED) else intent
            }
        }
        return match
    }

    override suspend fun formalizeContract(
        match: RoommateMatchResult,
        participants: List<RoommateIntent>,
    ): ContractRecord {
        val record = ContractRecord(
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
        contractRecordsStore[match.id] = record
        return record
    }

    override suspend fun getContractRecord(matchResultId: String): ContractRecord? =
        contractRecordsStore[matchResultId]

    override suspend fun getUserRules(userId: String): List<UserRule> = userRulesStore[userId].orEmpty()

    override suspend fun getIntegrityScore(userId: String): UserIntegrityScore =
        integrityScoresStore.getOrPut(userId) { UserIntegrityScore(userId = userId) }

    override suspend fun recordFulfillment(userId: String) {
        integrityScoresStore[userId] = getIntegrityScore(userId).recordFulfillment()
    }

    override suspend fun recordCancellationAfterMatch(userId: String) {
        integrityScoresStore[userId] = getIntegrityScore(userId).recordCancellation()
    }

    override suspend fun getPlatformPolicy(): PlatformPolicy = platformPolicy

    private fun seedIntents(): List<RoommateIntent> = listOf(
        // The matchable demo listing: same zone the Publish screen
        // defaults to, asking 8,500 MXN — the demo user's own default
        // 8,000 MXN budget produces an instant, compatible-but-not-
        // price-aligned match, exactly the scenario "Adjust My Price"
        // exists to resolve.
        RoommateIntent(
            id = "seed-roomie-offering-1",
            creatorUserId = "seed-user-landlord",
            creatorEmail = "landlord@ucaribe.edu.mx",
            createdAt = Clock.System.now(),
            scheduledAt = DEMO_MOVE_IN_WINDOW_START,
            expiresAt = DEMO_MOVE_IN_WINDOW_END,
            verificationTier = IdentityVerificationTier.PHONE_VERIFIED,
            financialTerms = FinancialTerms(amount = 8500.0, currency = "MXN"),
            status = ContractStatus.ACTIVE,
            role = RoommateRole.OFFERING,
            zone = "Cancún - Región 15",
            propertyDescription = "Cuarto amueblado, baño compartido, cerca del campus.",
            moveInWindowStart = DEMO_MOVE_IN_WINDOW_START,
            moveInWindowEnd = DEMO_MOVE_IN_WINDOW_END,
            leaseDurationMonths = 12,
            depositAmount = 8500.0,
            guarantorArrangement = "Aval o depósito adicional de un mes.",
            preferenceNotes = "Buscamos a alguien tranquilo y ordenado.",
            isThirdPartyArrangement = false,
            occupantDescription = "",
            allowedEmailDomains = pilotCommunityDomains,
            legalConsentAcknowledgedAt = Clock.System.now(),
        ),
    )
}
