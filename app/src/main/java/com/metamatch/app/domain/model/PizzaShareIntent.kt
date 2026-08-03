package com.metamatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * PizzaShareIntent
 * ==================
 *
 * WHAT: the second concrete implementation of [ContractIntent] — Module 2
 * of the "Wishing Well" hub, Meta-Match Pizza. Same underlying idea as
 * [RideShareIntent] (proximity-based matching), but the thing being
 * shared is a **purchase**, not a destination: someone wants part of an
 * order (a pizza, a Costco run, anything sold as one bigger unit) that is
 * too much for one person alone.
 *
 * WHY the fields look the way they do: the driving example is "I'd pay
 * 100 MXN for 4 of an 8-slice, 200 MXN pizza, rather than 200 MXN for all
 * 8 alone." That means this intent has to represent a *fraction* of a
 * larger purchase ([desiredUnits] out of [totalUnits]) — a concept
 * [RideShareIntent] has no need for, since every rider there contributes
 * an independent, uncapped amount toward a shared fare rather than
 * claiming a slice of one fixed-size order.
 *
 * HOW this connects to the architecture: built by
 * [com.metamatch.app.domain.usecase.PublishPizzaIntentUseCase], matched
 * by [com.metamatch.app.domain.usecase.FindPizzaMatchesUseCase] into a
 * [PizzaMatchResult], and persisted through whichever
 * [com.metamatch.app.domain.repository.PizzaShareRepository]
 * implementation is active — the exact same
 * `ContractRepository<PizzaShareIntent, PizzaMatchResult>` seam
 * [RideShareIntent] uses, just with a different type parameter.
 *
 * @property homeLocation Where this person is picking up their share.
 *   Named generically (not "pickupLocation") because it is also the
 *   point [FindPizzaMatchesUseCase] uses to compute proximity — mirrors
 *   [RideShareIntent.departure]'s role.
 * @property establishment Where the order is being placed — a pizzeria
 *   name today, any vendor tomorrow. Free text; the Publish screen offers
 *   common and less-common suggestions but never restricts input to them.
 * @property productDescription What is being ordered (e.g. "Pizza grande
 *   de pepperoni"). Matching requires this to agree (case-insensitively)
 *   between participants — two people wanting different products from
 *   the same establishment are not a match.
 * @property totalUnits The size of the whole order this intent is trying
 *   to split, in whatever unit makes sense for the product (slices,
 *   pieces, a case of something). Purely a reference value shared by
 *   everyone matching on the same order.
 * @property totalPriceForWholeOrder The reference price of the *entire*
 *   order (e.g. 200 MXN for the whole pizza) — informational context for
 *   the UI, deliberately independent of [financialTerms], the same way
 *   [RideShareIntent]'s estimated fare is independent of any one rider's
 *   contribution.
 * @property desiredUnits How much of [totalUnits] this specific person
 *   wants (e.g. 4 of 8 slices). Must be at least 1 and cannot exceed
 *   [totalUnits] — see the `init` block.
 * @property maxDistanceMeters This person's walking-distance tolerance
 *   for picking up a shared order, the direct analogue of
 *   [RideShareIntent.maxWalkingDistanceMeters].
 * @property creatorEmail See [RideShareIntent.creatorEmail] — same role.
 * @property allowedEmailDomains See [RideShareIntent.allowedEmailDomains].
 * @property blockedUserIds See [RideShareIntent.blockedUserIds].
 */
data class PizzaShareIntent(
    override val id: String,
    override val creatorUserId: String,
    override val createdAt: Instant,
    override val scheduledAt: Instant,
    override val expiresAt: Instant?,
    override val verificationTier: IdentityVerificationTier,
    override val financialTerms: FinancialTerms,
    override val status: ContractStatus,
    val homeLocation: GeoPoint,
    val establishment: String,
    val productDescription: String,
    val totalUnits: Int,
    val totalPriceForWholeOrder: Double,
    val desiredUnits: Int,
    val maxDistanceMeters: Double,
    val creatorEmail: String,
    val allowedEmailDomains: Set<String> = emptySet(),
    val blockedUserIds: Set<String> = emptySet(),
    override val legalConsentAcknowledgedAt: Instant? = null,
) : ContractIntent(
    id = id,
    creatorUserId = creatorUserId,
    contractType = ContractType.PIZZA_SHARE,
    createdAt = createdAt,
    scheduledAt = scheduledAt,
    expiresAt = expiresAt,
    verificationTier = verificationTier,
    financialTerms = financialTerms,
    status = status,
    legalConsentAcknowledgedAt = legalConsentAcknowledgedAt,
) {
    init {
        require(totalUnits > 0) { "totalUnits must be positive; got $totalUnits." }
        require(desiredUnits in 1..totalUnits) {
            "desiredUnits must be between 1 and totalUnits ($totalUnits); got $desiredUnits."
        }
        require(maxDistanceMeters > 0.0) {
            "maxDistanceMeters must be positive; got $maxDistanceMeters."
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
