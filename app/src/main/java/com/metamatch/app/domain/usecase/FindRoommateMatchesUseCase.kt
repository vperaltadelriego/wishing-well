package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.RoommateIntent
import com.metamatch.app.domain.model.RoommateMatchResult
import com.metamatch.app.domain.model.RoommateRole
import com.metamatch.app.domain.repository.RoommateRepository
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * FindRoommateMatchesUseCase
 * ============================
 *
 * WHAT: the Meta-Match Roomie matching algorithm — given one of the
 * current user's active [RoommateIntent]s, find every *opposite-role*
 * candidate compatible on zone and move-in window.
 *
 * WHY this returns a flat list per intent instead of growing one cluster
 * like [FindMatchesUseCase]/[FindPizzaMatchesUseCase]: Ride and Pizza
 * match peers wanting the *same thing*, so it makes sense to grow one
 * group around a starting intent. Roomie is two-sided — a
 * [RoommateRole.SEEKING] intent doesn't grow a group with other seekers,
 * it needs pairing against [RoommateRole.OFFERING] intents (and vice
 * versa). Each compatible opposite-role candidate becomes its own
 * [RoommateMatchResult] — a seeker sees a *list* of candidate places, the
 * same way real roommate/apartment searching works, not one merged group.
 *
 * WHY price is never a filter here (unlike currency, zone, dates, and
 * blacklists, which all hard-exclude): the explicit product requirement
 * is that a near-perfect match with a price gap — the brief's own
 * example, an 8,500 ask against an 8,000 budget — must still surface as
 * a match, with the gap shown honestly via
 * [RoommateMatchResult.priceGapPercent], not silently filtered out. The
 * follow-up move (lowering the ask, or the seeker raising their budget)
 * happens through [UpdateRoommateIntentUseCase], not through re-matching
 * with a wider price tolerance.
 */
class FindRoommateMatchesUseCase @Inject constructor(
    private val repository: RoommateRepository,
) {
    /**
     * @param userId The user asking "who's compatible with my listing?"
     * @return one [RoommateMatchResult] per compatible opposite-role
     *   candidate, across every active intent [userId] currently has.
     */
    suspend operator fun invoke(userId: String): List<RoommateMatchResult> {
        val myIntents = repository.getActiveIntentsForUser(userId)
        if (myIntents.isEmpty()) return emptyList()

        val candidatePool = repository.getCandidateIntents(excludingUserId = userId)

        return myIntents.flatMap { myIntent -> buildMatchesFor(myIntent, candidatePool) }
    }

    private fun buildMatchesFor(
        myIntent: RoommateIntent,
        allCandidates: List<RoommateIntent>,
    ): List<RoommateMatchResult> {
        val oppositeRole = if (myIntent.role == RoommateRole.SEEKING) RoommateRole.OFFERING else RoommateRole.SEEKING

        val eligibleCandidates = allCandidates.filter { candidate ->
            candidate.role == oppositeRole &&
                candidate.zone.trim().equals(myIntent.zone.trim(), ignoreCase = true) &&
                candidate.moveInWindowStart <= myIntent.moveInWindowEnd &&
                myIntent.moveInWindowStart <= candidate.moveInWindowEnd &&
                candidate.financialTerms.currency == myIntent.financialTerms.currency &&
                myIntent.isEligibleUser(candidate.creatorUserId) &&
                candidate.isEligibleUser(myIntent.creatorUserId) &&
                myIntent.acceptsEmailDomain(candidate.creatorEmail) &&
                candidate.acceptsEmailDomain(myIntent.creatorEmail)
        }

        return eligibleCandidates.map { candidate -> buildMatchResult(myIntent, candidate) }
    }

    private fun buildMatchResult(myIntent: RoommateIntent, candidate: RoommateIntent): RoommateMatchResult {
        val seeker = if (myIntent.role == RoommateRole.SEEKING) myIntent else candidate
        val offerer = if (myIntent.role == RoommateRole.OFFERING) myIntent else candidate

        return RoommateMatchResult(
            id = UUID.randomUUID().toString(),
            participantIntentIds = listOf(seeker.id, offerer.id),
            zone = myIntent.zone,
            askingPrice = offerer.financialTerms.amount,
            seekerBudget = seeker.financialTerms.amount,
            currency = myIntent.financialTerms.currency,
            createdAt = Clock.System.now(),
        )
    }
}
