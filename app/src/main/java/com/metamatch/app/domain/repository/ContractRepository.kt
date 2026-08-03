package com.metamatch.app.domain.repository

import com.metamatch.app.domain.model.ContractIntent
import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.UserIntegrityScore
import com.metamatch.app.domain.model.UserRule
import com.metamatch.app.domain.policy.PlatformPolicy
import kotlinx.coroutines.flow.Flow

/**
 * ContractRepository
 * ====================
 *
 * WHAT: the generic shape every vertical-specific repository
 * ([RideShareRepository], [com.metamatch.app.domain.repository
 * .PizzaShareRepository]) turns out to need — read/write a user's
 * intents, compute and persist matches, track reputation, and read the
 * live platform policy. Extracted here once [RideShareRepository] and
 * `PizzaShareRepository` turned out to be identical in every method
 * *except* which concrete [ContractIntent]/match-result types they
 * operate on.
 *
 * WHY this wasn't extracted in Stage 1, when [ContractIntent] already
 * existed for exactly this reason: with only one concrete vertical
 * (Ride), a generic interface would have been speculative — designing
 * for a "maybe someday" second vertical instead of a real one. Meta-Match
 * Pizza (Stage 2) is that second vertical, so the abstraction now
 * removes real duplication instead of anticipating hypothetical
 * duplication. This is the same "wait for the second real case" judgment
 * call the project's own conventions ask for elsewhere.
 *
 * HOW each vertical uses this: `RideShareRepository` and
 * `PizzaShareRepository` are now both just one-line interfaces —
 * `interface RideShareRepository : ContractRepository<RideShareIntent, MatchResult>`
 * — so every existing call site (`repository.getActiveIntentsForUser(...)`
 * etc.) keeps compiling and behaving exactly as before; only *where* the
 * method signatures are declared changed.
 *
 * Kotlin note: this interface has **two** type parameters — `T` for the
 * concrete [ContractIntent] subtype (`RideShareIntent`, `PizzaShareIntent`,
 * ...) and `M` for that vertical's own match-result type (`MatchResult`,
 * `PizzaMatchResult`, ...), since the two verticals' match results are
 * different shapes on purpose (see `PizzaMatchResult`'s own docs for why).
 * `T : ContractIntent` is a type-parameter *bound*: it tells the compiler
 * "whatever `T` ends up being, treat it as at least a `ContractIntent`" —
 * without it, `formalizeContract` couldn't read `intent.creatorUserId`
 * etc. from a plain, unconstrained `T`.
 */
interface ContractRepository<T : ContractIntent, M> {

    /** See [RideShareRepository.getActiveIntentsForUser]'s original docs — same role, generic. */
    suspend fun getActiveIntentsForUser(userId: String): List<T>

    /** See [RideShareRepository.getCandidateIntents]'s original docs — same role, generic. */
    suspend fun getCandidateIntents(excludingUserId: String): List<T>

    /** See [RideShareRepository.observeActiveIntentsForUser]'s original docs — same role, generic. */
    fun observeActiveIntentsForUser(userId: String): Flow<List<T>>

    /** Persists a brand-new intent, returning the stored value. */
    suspend fun publishIntent(intent: T): T

    /** Marks an intent as [com.metamatch.app.domain.model.ContractStatus.CANCELLED]. */
    suspend fun cancelIntent(intentId: String)

    /** Persists a computed match result of this vertical's own shape. */
    suspend fun saveMatchResult(match: M): M

    /** Freezes [match] plus every participant's terms into a [ContractRecord]
     * — see that class's own docs for why this is captured at all. */
    suspend fun formalizeContract(match: M, participants: List<T>): ContractRecord

    /** The [ContractRecord] previously produced for [matchResultId], if any. */
    suspend fun getContractRecord(matchResultId: String): ContractRecord?

    /** All [UserRule]s that apply to [userId]. */
    suspend fun getUserRules(userId: String): List<UserRule>

    /** Current reputation snapshot for [userId]. */
    suspend fun getIntegrityScore(userId: String): UserIntegrityScore

    /** Records that [userId] successfully completed a matched contract. */
    suspend fun recordFulfillment(userId: String)

    /** Records that [userId] canceled a contract *after* being matched. */
    suspend fun recordCancellationAfterMatch(userId: String)

    /** The current, live business-rule configuration — see [PlatformPolicy]'s own docs. */
    suspend fun getPlatformPolicy(): PlatformPolicy
}
