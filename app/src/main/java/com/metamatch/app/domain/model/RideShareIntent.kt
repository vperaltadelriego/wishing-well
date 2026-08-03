package com.metamatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * RideShareIntent
 * ================
 *
 * WHAT: the concrete, ride-share-specific implementation of
 * [ContractIntent] — Module 2 of the brief ("Ride Share Contract &
 * Advance Scheduling"). This is what gets created when a user fills in the
 * "Publish Intent" screen.
 *
 * WHY it exists separately from [ContractIntent]: the abstract parent only
 * knows about things every match request has in common (who, when, money,
 * status). A ride specifically also needs a departure point, a
 * destination, and a tolerance for how far someone is willing to walk to
 * a shared pickup spot — none of which make sense for, say, a study-group
 * intent. Kotlin inheritance lets us add exactly those extra fields here
 * without touching the parent class or any other subclass.
 *
 * HOW it connects to the architecture:
 * - Built by [com.metamatch.app.domain.usecase.PublishRideIntentUseCase].
 * - Read by [com.metamatch.app.domain.usecase.FindMatchesUseCase], which
 *   groups compatible `RideShareIntent`s into a [MatchResult].
 * - Persisted by whichever [com.metamatch.app.domain.repository.RideShareRepository]
 *   implementation is active (`MockRideShareRepository` today,
 *   `SupabaseRideShareRepository` once Module 3/4 wiring lands). In
 *   Supabase this becomes one row of the `ride_share_intents` table
 *   described in `schema.sql`, with `departure`/`destination` stored as
 *   native PostGIS `geography` columns instead of plain lat/lng floats.
 *
 * @property departure Where the rider will be picked up.
 * @property destination Where the rider wants to go. Two or more intents
 *   with *destinations* that converge within [maxWalkingDistanceMeters] of
 *   each other's centroid are candidates for a match — see
 *   [FindMatchesUseCase] for the exact rule.
 * @property maxWalkingDistanceMeters The rider's personal tolerance for
 *   "close enough". A student willing to walk 800 m to save money will
 *   match with more people than one who insists on being dropped off at
 *   the exact door.
 * @property allowedEmailDomains Community filter (Module 2, "Security
 *   Filters"). When non-empty, only users whose account email ends in one
 *   of these domains (e.g. `@ucaribe.edu.mx`) are eligible to be matched
 *   with this intent. An empty set means "open to anyone" — used for the
 *   generic public pilot before a university/company subscribes.
 * @property blockedUserIds Personal blacklist (Module 2, "User
 *   Blacklist"). The matching engine must never pair the creator of this
 *   intent with any user ID in this set, even if every other criterion
 *   matches perfectly — e.g. to guarantee an ex-partner or a previously
 *   reported user is never suggested again.
 * @property creatorEmail The publishing user's account email. Stored on
 *   the intent itself (rather than requiring a separate lookup) so
 *   [com.metamatch.app.domain.usecase.FindMatchesUseCase] can evaluate
 *   *both directions* of an email-domain restriction — "will I accept
 *   them?" and "will they accept me?" — using only the two
 *   `RideShareIntent`s already in hand.
 */
data class RideShareIntent(
    override val id: String,
    override val creatorUserId: String,
    override val createdAt: Instant,
    override val scheduledAt: Instant,
    override val expiresAt: Instant?,
    override val verificationTier: IdentityVerificationTier,
    override val financialTerms: FinancialTerms,
    override val status: ContractStatus,
    val departure: GeoPoint,
    val destination: GeoPoint,
    val maxWalkingDistanceMeters: Double,
    val creatorEmail: String,
    val allowedEmailDomains: Set<String> = emptySet(),
    val blockedUserIds: Set<String> = emptySet(),
    override val legalConsentAcknowledgedAt: Instant? = null,
) : ContractIntent(
    id = id,
    creatorUserId = creatorUserId,
    contractType = ContractType.RIDE_SHARE,
    createdAt = createdAt,
    scheduledAt = scheduledAt,
    expiresAt = expiresAt,
    verificationTier = verificationTier,
    financialTerms = financialTerms,
    status = status,
    legalConsentAcknowledgedAt = legalConsentAcknowledgedAt,
) {
    init {
        require(maxWalkingDistanceMeters > 0.0) {
            "maxWalkingDistanceMeters must be positive; got $maxWalkingDistanceMeters."
        }
    }

    /**
     * True if [candidateEmail] is allowed to be matched with this intent
     * under the [allowedEmailDomains] community filter. An empty filter
     * means everyone is welcome.
     *
     * Kotlin note: `any { }` short-circuits on the first match, so this is
     * cheap even with several allowed domains.
     */
    fun acceptsEmailDomain(candidateEmail: String): Boolean {
        if (allowedEmailDomains.isEmpty()) return true
        val domain = candidateEmail.substringAfter('@', missingDelimiterValue = "")
        return allowedEmailDomains.any { it.equals(domain, ignoreCase = true) }
    }

    /** True if [candidateUserId] has *not* been blacklisted by this intent's creator. */
    fun isEligibleUser(candidateUserId: String): Boolean = candidateUserId !in blockedUserIds
}
