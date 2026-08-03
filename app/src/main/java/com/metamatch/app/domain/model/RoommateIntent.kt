package com.metamatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * RoommateIntent
 * ================
 *
 * WHAT: the third concrete implementation of [ContractIntent] — Meta-
 * Match Roomie, the sublease/lease vertical. Unlike [RideShareIntent] and
 * [PizzaShareIntent], which match peers wanting the *same thing*, Roomie
 * is two-sided: someone [RoommateRole.OFFERING] a place to live and
 * someone [RoommateRole.SEEKING] one. [role] is what
 * [com.metamatch.app.domain.usecase.FindRoommateMatchesUseCase] uses to
 * pair opposite sides instead of clustering same-role peers.
 *
 * WHY the fields look the way they do — three requirements drove this
 * shape (see project memory `roomie_pizza_requirements.md`):
 *
 * 1. **The contracting party isn't always the occupant.** A parent
 *    arranging housing for a university-age child is a real, expected
 *    case. [isThirdPartyArrangement] + [occupantDescription] separate
 *    "who is negotiating" (always [ContractIntent.creatorUserId]) from
 *    "who actually lives there," without needing a second user account.
 * 2. **Objective vs. subjective conditions matter differently.** [zone],
 *    [moveInWindowStart]/[moveInWindowEnd], [leaseDurationMonths], and
 *    [financialTerms] are objective and comparable by
 *    [com.metamatch.app.domain.usecase.FindRoommateMatchesUseCase].
 *    [preferenceNotes] is deliberately free text that the matching
 *    algorithm never reads — "the place looks nice," "I like this
 *    person" are judgments only a human can make, and the whole point of
 *    a Roomie match is to stay a low-risk *introduction*, never a
 *    promise of compatibility.
 * 3. **A real Mexican lease's minimum content** (identification of both
 *    parties — via [ContractIntent.creatorUserId]/[creatorEmail] —
 *    property description, rent, deposit, duration, and a guarantor or
 *    alternative) needs to already exist as data, even though this MVP
 *    never assembles the actual written document. [propertyDescription],
 *    [depositAmount], [leaseDurationMonths], and [guarantorArrangement]
 *    exist for exactly that reason.
 *
 * WHY there is no [GeoPoint] here, unlike Ride/Pizza: roommate search is
 * neighborhood-scale ("busco en esta zona"), not walking-distance-scale —
 * [zone] (free text, matched case-insensitively) is the right grain, the
 * same simplification style [PizzaShareIntent.establishment] uses.
 *
 * @property role Whether this intent is offering a place or seeking one.
 * @property zone Free-text neighborhood/area — the geographic axis
 *   matching is performed on (see class doc above for why this isn't a
 *   [GeoPoint]).
 * @property propertyDescription What's being offered (if [role] is
 *   [RoommateRole.OFFERING]) or what's being sought (if
 *   [RoommateRole.SEEKING]) — free text.
 * @property moveInWindowStart Earliest acceptable move-in date.
 * @property moveInWindowEnd Latest acceptable move-in date. Must not
 *   precede [moveInWindowStart].
 * @property leaseDurationMonths How long the arrangement is meant to
 *   last — informational, displayed on a match, not a hard filter (a
 *   1-month gap in expectations is exactly the kind of thing the parties
 *   should discuss during their own post-match diligence, not something
 *   the engine should silently exclude on).
 * @property depositAmount The security deposit (depósito en garantía) —
 *   `0.0` if none is being asked for/offered.
 * @property guarantorArrangement Free text describing the guarantor or
 *   alternative (e.g. "Aval," "Depósito adicional de 2 meses," "Aún sin
 *   definir") — Mexican lease practice varies by state on what's
 *   acceptable here, so this stays flexible text rather than a fixed enum.
 * @property preferenceNotes Free-text subjective preferences — see class
 *   doc point 2. Never read by matching logic.
 * @property isThirdPartyArrangement Whether [ContractIntent.creatorUserId]
 *   is negotiating on behalf of someone else who will actually occupy
 *   the place.
 * @property occupantDescription Who actually lives there, if different
 *   from the account holder (e.g. "Mi hija, estudiante universitaria de
 *   19 años"). Blank when [isThirdPartyArrangement] is `false`.
 * @property creatorEmail See [RideShareIntent.creatorEmail] — same role.
 * @property allowedEmailDomains See [RideShareIntent.allowedEmailDomains].
 * @property blockedUserIds See [RideShareIntent.blockedUserIds].
 */
data class RoommateIntent(
    override val id: String,
    override val creatorUserId: String,
    override val createdAt: Instant,
    override val scheduledAt: Instant,
    override val expiresAt: Instant?,
    override val verificationTier: IdentityVerificationTier,
    override val financialTerms: FinancialTerms,
    override val status: ContractStatus,
    val role: RoommateRole,
    val zone: String,
    val propertyDescription: String,
    val moveInWindowStart: Instant,
    val moveInWindowEnd: Instant,
    val leaseDurationMonths: Int,
    val depositAmount: Double,
    val guarantorArrangement: String,
    val preferenceNotes: String,
    val isThirdPartyArrangement: Boolean,
    val occupantDescription: String,
    val creatorEmail: String,
    val allowedEmailDomains: Set<String> = emptySet(),
    val blockedUserIds: Set<String> = emptySet(),
    override val legalConsentAcknowledgedAt: Instant? = null,
) : ContractIntent(
    id = id,
    creatorUserId = creatorUserId,
    contractType = ContractType.ROOMMATE_SEARCH,
    createdAt = createdAt,
    scheduledAt = scheduledAt,
    expiresAt = expiresAt,
    verificationTier = verificationTier,
    financialTerms = financialTerms,
    status = status,
    legalConsentAcknowledgedAt = legalConsentAcknowledgedAt,
) {
    init {
        require(leaseDurationMonths > 0) { "leaseDurationMonths must be positive; got $leaseDurationMonths." }
        require(depositAmount >= 0.0) { "depositAmount cannot be negative; got $depositAmount." }
        require(moveInWindowEnd >= moveInWindowStart) {
            "moveInWindowEnd ($moveInWindowEnd) cannot precede moveInWindowStart ($moveInWindowStart)."
        }
    }

    /** Same rule as [RideShareIntent.acceptsEmailDomain] — an empty filter welcomes everyone. */
    fun acceptsEmailDomain(candidateEmail: String): Boolean {
        if (allowedEmailDomains.isEmpty()) return true
        val domain = candidateEmail.substringAfter('@', missingDelimiterValue = "")
        return allowedEmailDomains.any { it.equals(domain, ignoreCase = true) }
    }

    /** Same rule as [RideShareIntent.isEligibleUser]. */
    fun isEligibleUser(candidateUserId: String): Boolean = candidateUserId !in blockedUserIds
}

/**
 * Which side of a Roomie match this intent represents. See
 * [RoommateIntent]'s class doc for why this replaces the peer-clustering
 * shape [RideShareIntent]/[PizzaShareIntent] use.
 */
enum class RoommateRole { SEEKING, OFFERING }
