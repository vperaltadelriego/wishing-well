package com.metamatch.app.data.mock

import com.metamatch.app.domain.model.ContractPartySnapshot
import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.ContractStatus
import com.metamatch.app.domain.model.ContractType
import com.metamatch.app.domain.model.FinancialTerms
import com.metamatch.app.domain.model.GeoPoint
import com.metamatch.app.domain.model.IdentityVerificationTier
import com.metamatch.app.domain.model.PizzaMatchResult
import com.metamatch.app.domain.model.PizzaShareIntent
import com.metamatch.app.domain.model.UserIntegrityScore
import com.metamatch.app.domain.model.UserRule
import com.metamatch.app.domain.policy.PlatformPolicy
import com.metamatch.app.domain.repository.PizzaShareRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * MockPizzaShareRepository
 * ==========================
 *
 * WHAT: an in-memory, zero-network implementation of
 * [PizzaShareRepository] — the Pizza vertical's twin of
 * [MockRideShareRepository]. Same role: makes Meta-Match Pizza runnable
 * and demoable the instant the app is cloned, with no backend.
 *
 * WHY the seed data looks the way it does: [seedIntents] includes one
 * intent that the default "accept every pre-filled value, tap Publish"
 * demo path will immediately match against (same establishment/product,
 * compatible schedule and distance, desired units that don't overflow
 * the order) — the same "guided demo" property
 * [MockRideShareRepository]'s seeded María intent has for Ride — plus one
 * intent deliberately outside the community email allow-list, so the
 * Module 2 security filter visibly excludes someone here too, not just
 * in Ride.
 *
 * Kotlin note: reuses [MockRideShareRepository.CURRENT_DEMO_USER_ID]
 * rather than declaring its own — it's the same logged-in demo user
 * across every vertical of the app, not a per-vertical concept.
 */
@Singleton
class MockPizzaShareRepository @Inject constructor() : PizzaShareRepository {

    companion object {
        /**
         * The scheduled pickup time used by the seeded, matchable demo
         * intent — computed as "20 minutes from whenever the app happens
         * to start" rather than a fixed clock time, for the same
         * timezone-safety reason as
         * [MockRideShareRepository.DEMO_RIDE_SCHEDULED_AT]: Pizza demand
         * is "now," so a fixed wall-clock string would only produce a
         * demo match at certain times of day.
         */
        val DEMO_PIZZA_SCHEDULED_AT: Instant = Clock.System.now().plus(20.minutes)
    }

    private val pilotCommunityDomains = setOf("ucaribe.edu.mx", "anahuac.mx", "xcaret.com")

    private val intentsFlow = MutableStateFlow(seedIntents())
    private val matchResultsStore = mutableListOf<PizzaMatchResult>()
    private val contractRecordsStore = mutableMapOf<String, ContractRecord>()
    private val userRulesStore = mutableMapOf<String, List<UserRule>>()
    private val integrityScoresStore = mutableMapOf<String, UserIntegrityScore>()
    private var platformPolicy = PlatformPolicy.default()

    override suspend fun getActiveIntentsForUser(userId: String): List<PizzaShareIntent> =
        intentsFlow.value.filter { it.creatorUserId == userId && it.status == ContractStatus.ACTIVE }

    override suspend fun getCandidateIntents(excludingUserId: String): List<PizzaShareIntent> =
        intentsFlow.value.filter {
            it.creatorUserId != excludingUserId && it.status == ContractStatus.ACTIVE
        }

    override fun observeActiveIntentsForUser(userId: String): Flow<List<PizzaShareIntent>> =
        intentsFlow.map { all -> all.filter { it.creatorUserId == userId && it.status == ContractStatus.ACTIVE } }

    override suspend fun publishIntent(intent: PizzaShareIntent): PizzaShareIntent {
        intentsFlow.update { current -> current + intent }
        return intent
    }

    override suspend fun cancelIntent(intentId: String) {
        intentsFlow.update { current ->
            current.map { if (it.id == intentId) it.copy(status = ContractStatus.CANCELLED) else it }
        }
    }

    override suspend fun saveMatchResult(match: PizzaMatchResult): PizzaMatchResult {
        matchResultsStore += match
        intentsFlow.update { current ->
            current.map { intent ->
                if (intent.id in match.participantIntentIds) intent.copy(status = ContractStatus.MATCHED) else intent
            }
        }
        return match
    }

    override suspend fun formalizeContract(
        match: PizzaMatchResult,
        participants: List<PizzaShareIntent>,
    ): ContractRecord {
        val record = ContractRecord(
            id = "contract-${match.id}",
            matchResultId = match.id,
            contractType = ContractType.PIZZA_SHARE,
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

    private fun seedIntents(): List<PizzaShareIntent> = listOf(
        // Matchable demo intent: same establishment/product the Publish
        // screen's defaults use, wanting 4 of the 8 slices — leaves
        // exactly enough room for the demo user's own default 4-slice
        // request, so "accept every default, tap Publish" produces an
        // instant match here too, same guided-demo property Ride has.
        PizzaShareIntent(
            id = "seed-pizza-carlos",
            creatorUserId = "seed-user-carlos",
            creatorEmail = "carlos@ucaribe.edu.mx",
            createdAt = Clock.System.now(),
            scheduledAt = DEMO_PIZZA_SCHEDULED_AT,
            expiresAt = DEMO_PIZZA_SCHEDULED_AT.plus(1.hours),
            verificationTier = IdentityVerificationTier.EMAIL_ONLY,
            financialTerms = FinancialTerms(amount = 100.0, currency = "MXN"),
            status = ContractStatus.ACTIVE,
            homeLocation = GeoPoint(21.0899, -86.8462),
            establishment = "Domino's Pizza",
            productDescription = "Pizza grande especial",
            totalUnits = 8,
            totalPriceForWholeOrder = 200.0,
            desiredUnits = 4,
            maxDistanceMeters = 400.0,
            allowedEmailDomains = pilotCommunityDomains,
            legalConsentAcknowledgedAt = Clock.System.now(),
        ),
        // Deliberately OUTSIDE the pilot-community allow-list — same
        // establishment/product/schedule, geospatially perfect, but its
        // own gmail.com creator is excluded by the OTHER intents' filter
        // (mirrors MockRideShareRepository's "seed-aicm-3-filtered-out").
        PizzaShareIntent(
            id = "seed-pizza-stranger-filtered-out",
            creatorUserId = "seed-user-pizza-stranger",
            creatorEmail = "random.person@gmail.com",
            createdAt = Clock.System.now(),
            scheduledAt = DEMO_PIZZA_SCHEDULED_AT,
            expiresAt = DEMO_PIZZA_SCHEDULED_AT.plus(1.hours),
            verificationTier = IdentityVerificationTier.EMAIL_ONLY,
            financialTerms = FinancialTerms(amount = 50.0, currency = "MXN"),
            status = ContractStatus.ACTIVE,
            homeLocation = GeoPoint(21.0897, -86.8460),
            establishment = "Domino's Pizza",
            productDescription = "Pizza grande especial",
            totalUnits = 8,
            totalPriceForWholeOrder = 200.0,
            desiredUnits = 2,
            maxDistanceMeters = 400.0,
            allowedEmailDomains = emptySet(),
            legalConsentAcknowledgedAt = Clock.System.now(),
        ),
    )
}
