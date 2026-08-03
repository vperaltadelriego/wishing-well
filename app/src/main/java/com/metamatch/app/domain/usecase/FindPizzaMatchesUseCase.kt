package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.GeoPoint
import com.metamatch.app.domain.model.PizzaMatchResult
import com.metamatch.app.domain.model.PizzaShareIntent
import com.metamatch.app.domain.repository.PizzaShareRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.minus
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * FindPizzaMatchesUseCase
 * =========================
 *
 * WHAT: the Meta-Match Pizza equivalent of [FindMatchesUseCase] — given
 * one of the current user's active [PizzaShareIntent]s, find every group
 * of other nearby people who want to split the *same order* at the *same
 * establishment*, without the combined group claiming more than the
 * order actually has.
 *
 * WHY the matching rule differs from Ride's: Ride clusters purely on
 * geography and a shared budget vs. fare check. Pizza has to satisfy two
 * constraints *at once* — geography (same as Ride, via
 * [GeoPoint.centroidOf]/[GeoPoint.distanceMetersTo]) **and** a hard
 * capacity limit ([PizzaShareIntent.totalUnits]): a group can be
 * perfectly close together and still not be a valid match if their
 * combined [PizzaShareIntent.desiredUnits] would claim more of the order
 * than exists.
 *
 * WHY the schedule tolerance is tighter than Ride's: a flight landing
 * years from now has [FindMatchesUseCase]'s 30-minute tolerance to allow
 * for real-world schedule slop on a single, far-future instant. Pizza
 * demand is overwhelmingly "I want this now" — [SCHEDULE_TOLERANCE] here
 * is deliberately smaller.
 */
class FindPizzaMatchesUseCase @Inject constructor(
    private val repository: PizzaShareRepository,
) {
    companion object {
        /** How close two intents' [PizzaShareIntent.scheduledAt] values
         * must be to be considered "the same pickup window." */
        private val SCHEDULE_TOLERANCE = 20.minutes
    }

    /**
     * @param userId The user asking "who can I split an order with?"
     * @return one [PizzaMatchResult] per compatible group found, one
     *   group per active intent [userId] currently has.
     */
    suspend operator fun invoke(userId: String): List<PizzaMatchResult> {
        val myIntents = repository.getActiveIntentsForUser(userId)
        if (myIntents.isEmpty()) return emptyList()

        val candidatePool = repository.getCandidateIntents(excludingUserId = userId)

        return myIntents.mapNotNull { myIntent -> buildMatchFor(myIntent, candidatePool) }
    }

    private fun buildMatchFor(
        myIntent: PizzaShareIntent,
        allCandidates: List<PizzaShareIntent>,
    ): PizzaMatchResult? {
        // Hard filters: same order (establishment + product), schedule
        // proximity, currency, blacklists, and email-domain rules must
        // ALL hold before two intents can even be considered for the
        // same shared purchase.
        val eligibleCandidates = allCandidates.filter { candidate ->
            candidate.establishment.trim().equals(myIntent.establishment.trim(), ignoreCase = true) &&
                candidate.productDescription.trim().equals(myIntent.productDescription.trim(), ignoreCase = true) &&
                (candidate.scheduledAt - myIntent.scheduledAt).absoluteValue <= SCHEDULE_TOLERANCE &&
                candidate.financialTerms.currency == myIntent.financialTerms.currency &&
                myIntent.isEligibleUser(candidate.creatorUserId) &&
                candidate.isEligibleUser(myIntent.creatorUserId) &&
                myIntent.acceptsEmailDomain(candidate.creatorEmail) &&
                candidate.acceptsEmailDomain(myIntent.creatorEmail)
        }

        val cluster = mutableListOf(myIntent)
        var remaining = eligibleCandidates

        while (remaining.isNotEmpty()) {
            val currentCentroid = GeoPoint.centroidOf(cluster.map { it.homeLocation })

            // Try candidates nearest to the current centroid first — same
            // greedy heuristic as FindMatchesUseCase.
            val ordered = remaining.sortedBy { it.homeLocation.distanceMetersTo(currentCentroid) }
            val accepted = ordered.firstOrNull { candidate ->
                fitsCluster(cluster + candidate, myIntent.totalUnits)
            } ?: break

            cluster += accepted
            remaining = remaining - accepted
        }

        if (cluster.size < 2) return null

        val pickupPoint = GeoPoint.centroidOf(cluster.map { it.homeLocation })
        val totalUnitsClaimed = cluster.sumOf { it.desiredUnits }
        val totalContribution = cluster.sumOf { it.financialTerms.amount }

        return PizzaMatchResult(
            id = UUID.randomUUID().toString(),
            participantIntentIds = cluster.map { it.id },
            pickupPoint = pickupPoint,
            establishment = myIntent.establishment,
            productDescription = myIntent.productDescription,
            totalUnitsClaimed = totalUnitsClaimed,
            totalUnitsAvailable = myIntent.totalUnits,
            totalContribution = totalContribution,
            currency = myIntent.financialTerms.currency,
            createdAt = Clock.System.now(),
        )
    }

    /**
     * True if [candidateCluster] would still be geographically fair to
     * every member (same rule as [FindMatchesUseCase.fitsCluster]) *and*
     * doesn't claim more of the order than [totalUnits] actually has.
     */
    private fun fitsCluster(candidateCluster: List<PizzaShareIntent>, totalUnits: Int): Boolean {
        if (candidateCluster.sumOf { it.desiredUnits } > totalUnits) return false

        val centroid = GeoPoint.centroidOf(candidateCluster.map { it.homeLocation })
        return candidateCluster.all { member ->
            member.homeLocation.distanceMetersTo(centroid) <= member.maxDistanceMeters
        }
    }
}
