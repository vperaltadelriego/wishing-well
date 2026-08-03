package com.metamatch.app.data.mock

import com.metamatch.app.domain.model.ContractPartySnapshot
import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.ContractStatus
import com.metamatch.app.domain.model.FinancialTerms
import com.metamatch.app.domain.model.GeoPoint
import com.metamatch.app.domain.model.IdentityVerificationTier
import com.metamatch.app.domain.model.MatchResult
import com.metamatch.app.domain.model.RideShareIntent
import com.metamatch.app.domain.model.UserIntegrityScore
import com.metamatch.app.domain.model.UserRule
import com.metamatch.app.domain.policy.PlatformPolicy
import com.metamatch.app.domain.repository.RideShareRepository
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
import kotlin.time.Duration.Companion.hours

/**
 * MockRideShareRepository
 * =========================
 *
 * WHAT: an in-memory, zero-network implementation of
 * [RideShareRepository]. This is the "RepositoryStrategy" half of the
 * project brief that lets MetaMatch "run and be fully demonstrated
 * standalone in Android Studio" — clone the repo, hit Run, and the app
 * works immediately with no Supabase project, no API keys, and no
 * internet connection.
 *
 * WHY the seed data looks the way it does:
 * every seeded intent below is a candidate already "in the pool" when the
 * app starts, seeded around the exact scenario described in the product
 * brief — a flight landing at the Mexico City airport (AICM) far in the
 * future, plus a couple of local Cancún-area rides matching the pilot
 * communities named in the brief (`@ucaribe.edu.mx`, `@anahuac.mx`,
 * `@xcaret.com`). One seeded intent deliberately uses an email domain
 * *outside* that allow-list (a plain `gmail.com` address) specifically so
 * a developer exploring the demo can see the Module 2 "Community &
 * Security Filters" actually excluding someone, not just matching
 * everyone by default.
 *
 * The intent to actually interact with belongs to [CURRENT_DEMO_USER_ID]
 * ("you", the person running the app) and starts empty on purpose — the
 * whole point of the Publish Intent screen is to create it live.
 *
 * HOW this satisfies "Configurable parameters... agility after testing":
 * [platformPolicy] starts from [PlatformPolicy.default] and is exposed
 * through a plain `var` specifically so a future debug/admin screen (or a
 * unit test) can call `mockRepository.updatePolicyForTesting(...)` and
 * observe every use case immediately respect the new limits — modeling,
 * in miniature, what editing the real `platform_policy` Supabase table
 * would do in production.
 *
 * Kotlin note: this class is annotated `@Singleton` and constructed via
 * `@Inject constructor()` with no arguments — Hilt will create exactly
 * one instance for the whole app process, which is essential here: if a
 * new instance were created every time something asked for a
 * `RideShareRepository`, the in-memory list would reset every time and
 * nothing published would ever be visible on the next screen.
 */
@Singleton
class MockRideShareRepository @Inject constructor() : RideShareRepository {

    companion object {
        /** The account the demo runs as — analogous to "the logged-in user"
         * before real Supabase Auth is wired in. */
        const val CURRENT_DEMO_USER_ID = "demo-user-you"

        /**
         * The scheduled time used by the "seed-cancun-1" demo intent
         * (María's free UCaribe -> airport ride) — computed as "13 days
         * from whenever the app happens to start" rather than a fixed
         * calendar date.
         *
         * WHY this is computed instead of hard-coded: [Instant] arithmetic
         * via [kotlin.time.Duration] (`.plus(13.days)`) is pure absolute
         * time — it does not care what timezone the device is set to. If
         * this were instead a fixed string like `"2026-08-15T07:30:00-05:00"`,
         * the guided "publish the defaults, immediately see a match" demo
         * described in [com.metamatch.app.ui.publish.PublishUiState] would
         * only actually produce a match on devices whose system timezone
         * happens to agree with the offset baked into that string — fragile
         * for anyone reviewing this project on a machine set to a different
         * timezone. `PublishIntentViewModel` reads this exact same constant
         * to seed its date/time picker defaults, so the two always land
         * within [com.metamatch.app.domain.usecase.FindMatchesUseCase]'s
         * schedule-tolerance window of each other, regardless of timezone.
         */
        val DEMO_RIDE_SCHEDULED_AT: Instant = Clock.System.now().plus(13.days)
    }

    private val pilotCommunityDomains = setOf("ucaribe.edu.mx", "anahuac.mx", "xcaret.com")

    private val intentsFlow = MutableStateFlow(seedIntents())
    private val matchResultsStore = mutableListOf<MatchResult>()
    private val contractRecordsStore = mutableMapOf<String, ContractRecord>()
    private val userRulesStore = mutableMapOf<String, List<UserRule>>()
    private val integrityScoresStore = mutableMapOf<String, UserIntegrityScore>()
    private var platformPolicy = PlatformPolicy.default()

    override suspend fun getActiveIntentsForUser(userId: String): List<RideShareIntent> =
        intentsFlow.value.filter { it.creatorUserId == userId && it.status == ContractStatus.ACTIVE }

    override suspend fun getCandidateIntents(excludingUserId: String): List<RideShareIntent> =
        intentsFlow.value.filter {
            it.creatorUserId != excludingUserId && it.status == ContractStatus.ACTIVE
        }

    override fun observeActiveIntentsForUser(userId: String): Flow<List<RideShareIntent>> =
        intentsFlow.map { all -> all.filter { it.creatorUserId == userId && it.status == ContractStatus.ACTIVE } }

    override suspend fun publishIntent(intent: RideShareIntent): RideShareIntent {
        intentsFlow.update { current -> current + intent }
        return intent
    }

    override suspend fun cancelIntent(intentId: String) {
        intentsFlow.update { current ->
            current.map { if (it.id == intentId) it.copy(status = ContractStatus.CANCELLED) else it }
        }
    }

    override suspend fun saveMatchResult(match: MatchResult): MatchResult {
        matchResultsStore += match
        // Mark every participating intent as MATCHED so it stops showing
        // up as a candidate for further matching.
        intentsFlow.update { current ->
            current.map { intent ->
                if (intent.id in match.participantIntentIds) intent.copy(status = ContractStatus.MATCHED) else intent
            }
        }
        return match
    }

    override suspend fun formalizeContract(
        match: MatchResult,
        participants: List<RideShareIntent>,
    ): ContractRecord {
        val record = ContractRecord(
            id = "contract-${match.id}",
            matchResultId = match.id,
            contractType = com.metamatch.app.domain.model.ContractType.RIDE_SHARE,
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

    /**
     * Testing/demo-only hook: lets a debug screen or a unit test change the
     * live policy at runtime, the same way editing the Supabase
     * `platform_policy` table would in production. Not part of the
     * [RideShareRepository] interface on purpose — it is specific to this
     * mock's ability to simulate "agility after testing the app," not a
     * general repository capability.
     */
    fun updatePolicyForTesting(newPolicy: PlatformPolicy) {
        platformPolicy = newPolicy
    }

    private fun seedIntents(): List<RideShareIntent> = listOf(
        // Scenario straight from the product brief: a flight landing at
        // Mexico City International Airport (AICM) years in the future.
        RideShareIntent(
            id = "seed-aicm-1",
            creatorUserId = "seed-user-ana",
            creatorEmail = "ana@ucaribe.edu.mx",
            createdAt = Instant.parse("2026-08-01T10:00:00-06:00"),
            scheduledAt = Instant.parse("2030-10-10T20:00:00-06:00"),
            expiresAt = Instant.parse("2030-10-11T00:00:00-06:00"),
            verificationTier = IdentityVerificationTier.EMAIL_ONLY,
            financialTerms = FinancialTerms(amount = 50.0, currency = "MXN"),
            status = ContractStatus.ACTIVE,
            departure = GeoPoint(19.4363, -99.0721), // AICM Terminal 2
            destination = GeoPoint(19.4270, -99.1677), // Polanco
            maxWalkingDistanceMeters = 500.0,
            allowedEmailDomains = pilotCommunityDomains,
            legalConsentAcknowledgedAt = Instant.parse("2026-08-01T10:00:00-06:00"),
        ),
        RideShareIntent(
            id = "seed-aicm-2",
            creatorUserId = "seed-user-luis",
            creatorEmail = "luis@anahuac.mx",
            createdAt = Instant.parse("2026-08-01T11:30:00-06:00"),
            scheduledAt = Instant.parse("2030-10-10T20:00:00-06:00"),
            expiresAt = Instant.parse("2030-10-11T00:00:00-06:00"),
            verificationTier = IdentityVerificationTier.PHONE_VERIFIED,
            financialTerms = FinancialTerms(amount = 30.0, currency = "MXN"),
            status = ContractStatus.ACTIVE,
            departure = GeoPoint(19.4363, -99.0721), // Same flight, same airport
            destination = GeoPoint(19.4326, -99.1622), // A few blocks from Polanco: within walking tolerance
            maxWalkingDistanceMeters = 600.0,
            allowedEmailDomains = pilotCommunityDomains,
            legalConsentAcknowledgedAt = Instant.parse("2026-08-01T11:30:00-06:00"),
        ),
        // Deliberately OUTSIDE the pilot-community allow-list: shows the
        // Module 2 security filter actually excluding a candidate instead
        // of matching everyone unconditionally.
        RideShareIntent(
            id = "seed-aicm-3-filtered-out",
            creatorUserId = "seed-user-stranger",
            creatorEmail = "random.person@gmail.com",
            createdAt = Instant.parse("2026-08-01T12:00:00-06:00"),
            scheduledAt = Instant.parse("2030-10-10T20:00:00-06:00"),
            expiresAt = Instant.parse("2030-10-11T00:00:00-06:00"),
            verificationTier = IdentityVerificationTier.EMAIL_ONLY,
            financialTerms = FinancialTerms(amount = 40.0, currency = "MXN"),
            status = ContractStatus.ACTIVE,
            departure = GeoPoint(19.4363, -99.0721),
            destination = GeoPoint(19.4300, -99.1650), // Geospatially perfect fit...
            maxWalkingDistanceMeters = 600.0,
            allowedEmailDomains = emptySet(), // ...but this intent accepts anyone,
            // it's the OTHER two intents' allow-list that rejects this
            // creator's @gmail.com domain, mutually excluding them.
            legalConsentAcknowledgedAt = Instant.parse("2026-08-01T12:00:00-06:00"),
        ),
        // A local, free (¢0) Cancún-area ride: UCaribe campus to the
        // airport, demonstrating the "$0 for free rides" requirement.
        // Scheduled dynamically via DEMO_RIDE_SCHEDULED_AT (see that
        // constant's docs) so the guided "publish the defaults, see an
        // instant match" demo works on any device, in any timezone.
        RideShareIntent(
            id = "seed-cancun-1",
            creatorUserId = "seed-user-maria",
            creatorEmail = "maria@ucaribe.edu.mx",
            createdAt = Clock.System.now(),
            scheduledAt = DEMO_RIDE_SCHEDULED_AT,
            expiresAt = DEMO_RIDE_SCHEDULED_AT.plus(4.hours),
            verificationTier = IdentityVerificationTier.EMAIL_ONLY,
            financialTerms = FinancialTerms(amount = 0.0, currency = "MXN"),
            status = ContractStatus.ACTIVE,
            departure = GeoPoint(21.0894, -86.8459), // Universidad del Caribe, Cancún
            destination = GeoPoint(21.0367, -86.8770), // Cancún International Airport
            maxWalkingDistanceMeters = 300.0,
            allowedEmailDomains = pilotCommunityDomains,
            legalConsentAcknowledgedAt = Clock.System.now(),
        ),
    )
}
