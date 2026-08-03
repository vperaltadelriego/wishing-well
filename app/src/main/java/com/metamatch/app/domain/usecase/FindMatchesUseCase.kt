package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.GeoPoint
import com.metamatch.app.domain.model.MatchResult
import com.metamatch.app.domain.model.RideShareIntent
import com.metamatch.app.domain.repository.RideShareRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.minus
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * FindMatchesUseCase
 * ====================
 *
 * WHAT: the Kotlin implementation of Module 2's "Geospatial Centroid &
 * Budget Matching" logic — given one of the current user's active
 * [RideShareIntent]s, find every group of other travelers whose
 * destinations converge closely enough to share a ride, and whose pooled
 * budgets can plausibly cover the fare.
 *
 * WHY this logic lives in the domain layer, in plain Kotlin, instead of
 * only as a SQL function:
 * `schema.sql` defines the *production* version of this same idea using
 * PostGIS (`ST_Centroid`, `ST_DWithin`) running inside Postgres, which is
 * the right place for it once thousands of intents exist — the database
 * can use spatial indexes instead of comparing every pair of rows in app
 * memory. But the brief also requires the app to "run and be fully
 * demonstrated standalone in Android Studio" with a `MockRepository`. This
 * class is a faithful, documented, pure-Kotlin *mirror* of that SQL logic,
 * so the demo experience is honest — matches shown in the mock are
 * computed with the same rules a real Postgres deployment would use, not
 * a fake shortcut.
 *
 * HOW the algorithm works
 * ------------------------
 * 1. Pull every other active intent from the repository
 *    ([RideShareRepository.getCandidateIntents]) — the candidate pool.
 * 2. Discard candidates that fail any hard filter: different currency,
 *    a blacklist in either direction, or an email-domain restriction in
 *    either direction (mirrors Module 2's "Community & Security Filters").
 * 3. Greedily grow a cluster starting from the user's own intent: repeatedly
 *    try adding the closest remaining compatible candidate, recompute the
 *    destination centroid ([GeoPoint.centroidOf], the Kotlin mirror of
 *    `ST_Centroid`), and keep the candidate only if *every* current member
 *    of the cluster — including ones already accepted — is still within
 *    their own [RideShareIntent.maxWalkingDistanceMeters] of the new
 *    centroid (the Kotlin mirror of `ST_DWithin`). This models real
 *    behavior: adding a 4th passenger can push the fair meeting point far
 *    enough that an earlier passenger would no longer accept it.
 * 4. Once no more candidates can be added, if the cluster has 2+ members,
 *    compute the pooled budget and an estimated fare
 *    ([estimateFareMxn]) and package the result as a [MatchResult].
 *
 * This is intentionally a *greedy* algorithm, not an exhaustive search for
 * the globally optimal grouping — optimal clustering here is more
 * complex than an MVP (or a Kotlin-learning project) warrants, and greedy
 * nearest-neighbor grouping is exactly the kind of pragmatic first version
 * a real product ships before optimizing further.
 */
class FindMatchesUseCase @Inject constructor(
    private val repository: RideShareRepository,
) {
    companion object {
        /**
         * How close two intents' [RideShareIntent.scheduledAt] values must
         * be to be considered "the same trip." Half an hour comfortably
         * covers a flight's real vs. scheduled landing time while still
         * ruling out two otherwise-identical intents scheduled on
         * different days.
         */
        private val SCHEDULE_TOLERANCE = 30.minutes
    }

    /**
     * @param userId The user asking "who can I share a ride with?"
     * @return one [MatchResult] per compatible group found, one group per
     *   active intent [userId] currently has. Empty if the user has no
     *   active intents or no compatible candidates exist yet.
     */
    suspend operator fun invoke(userId: String): List<MatchResult> {
        val myIntents = repository.getActiveIntentsForUser(userId)
        if (myIntents.isEmpty()) return emptyList()

        val candidatePool = repository.getCandidateIntents(excludingUserId = userId)

        return myIntents.mapNotNull { myIntent -> buildMatchFor(myIntent, candidatePool) }
    }

    private fun buildMatchFor(
        myIntent: RideShareIntent,
        allCandidates: List<RideShareIntent>,
    ): MatchResult? {
        // Hard filters: schedule proximity, currency, blacklists, and
        // email-domain rules must ALL hold before two intents may even be
        // considered for the same ride. Two geospatially perfect intents
        // scheduled a month apart are obviously not the same ride — the
        // schedule check comes first for exactly that reason.
        val eligibleCandidates = allCandidates.filter { candidate ->
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
            val currentCentroid = GeoPoint.centroidOf(cluster.map { it.destination })

            // Try candidates nearest to the current centroid first — the
            // greedy heuristic described in the class doc.
            val ordered = remaining.sortedBy { it.destination.distanceMetersTo(currentCentroid) }
            val accepted = ordered.firstOrNull { candidate ->
                fitsCluster(cluster + candidate)
            } ?: break

            cluster += accepted
            remaining = remaining - accepted
        }

        if (cluster.size < 2) return null

        val meetingPoint = GeoPoint.centroidOf(cluster.map { it.destination })
        val totalContribution = cluster.sumOf { it.financialTerms.amount }
        val averageDepartureDistance = cluster
            .map { it.departure.distanceMetersTo(meetingPoint) }
            .average()

        return MatchResult(
            id = UUID.randomUUID().toString(),
            participantIntentIds = cluster.map { it.id },
            meetingPoint = meetingPoint,
            totalContribution = totalContribution,
            currency = myIntent.financialTerms.currency,
            estimatedFare = estimateFareMxn(averageDepartureDistance),
            createdAt = Clock.System.now(),
        )
    }

    /**
     * True if every intent in [candidateCluster] would still accept the
     * centroid formed by the whole group — i.e. nobody's personal
     * walking-distance tolerance is violated by adding the newest member.
     */
    private fun fitsCluster(candidateCluster: List<RideShareIntent>): Boolean {
        val centroid = GeoPoint.centroidOf(candidateCluster.map { it.destination })
        return candidateCluster.all { member ->
            member.destination.distanceMetersTo(centroid) <= member.maxWalkingDistanceMeters
        }
    }

    /**
     * A deliberately simple, clearly-labeled fare heuristic standing in
     * for a real routing/pricing API (Uber/Didi/Taxi estimate endpoints).
     * Base fare + per-kilometer rate roughly matching Mexican urban taxi
     * pricing as of this writing — tune freely; this is exactly the kind
     * of number [com.metamatch.app.domain.policy.PlatformPolicy] would
     * eventually own if fare estimation needs to vary by city or change
     * after real usage data comes in.
     */
    private fun estimateFareMxn(distanceMeters: Double): Double {
        val baseFareMxn = 25.0
        val perKilometerRateMxn = 8.0
        val distanceKm = distanceMeters / 1000.0
        return baseFareMxn + (distanceKm * perKilometerRateMxn)
    }
}
