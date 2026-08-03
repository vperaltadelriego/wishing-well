package com.metamatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * ContractRecord
 * ================
 *
 * WHAT: the durable proof that a [MatchResult] was accepted — a frozen
 * snapshot of exactly what every participant agreed to, at the moment
 * they agreed to it. Produced once, by
 * [com.metamatch.app.domain.repository.RideShareRepository.formalizeContract],
 * the instant a user taps "Accept Match", and never mutated afterward.
 *
 * WHY it exists
 * -------------
 * Victor's brief treats every accepted match as, conceptually, a
 * contract between the participants — even though this MVP never
 * generates a PDF or any other physical document. What a *real* contract
 * needs, should one ever have to be formalized in writing, is a record of
 * who agreed to what, and proof they consented to the platform's terms.
 * That is exactly the data this class exists to freeze: each
 * [ContractPartySnapshot] captures a participant's financial terms,
 * verification tier, and legal-consent timestamp *as they stood at match
 * time* — deliberately copied out of the live [ContractIntent], rather
 * than referenced by ID, so a later edit to that user's now-matched intent
 * (or the intent itself expiring/being deleted) can never retroactively
 * change what the record says was agreed.
 *
 * HOW it connects to the architecture
 * ------------------------------------
 * - Built by [com.metamatch.app.domain.repository.RideShareRepository
 *   .formalizeContract] from a [MatchResult] plus the full
 *   [RideShareIntent] of every participant.
 * - Read back by [com.metamatch.app.domain.repository.RideShareRepository
 *   .getContractRecord] and displayed by the Match Results screen right
 *   after acceptance ("CONTRACT RECORD SAVED").
 * - This is intentionally *data only* — no PDF export, no e-signature, no
 *   legal enforcement. It is the methodological seed a real
 *   contract-formalization feature would consume later, not that feature
 *   itself.
 *
 * @property id Unique identifier for this record.
 * @property matchResultId The [MatchResult.id] this record formalizes.
 * @property contractType What kind of match this was (mirrors
 *   [ContractIntent.contractType] — [ContractType.RIDE_SHARE] today).
 * @property participants One snapshot per matched party.
 * @property formalizedAt When the match was accepted (i.e. when this
 *   record was produced).
 * @property currency Currency shared by every participant's terms.
 * @property totalContribution Sum of every participant's contribution at
 *   formalization time — copied from [MatchResult.totalContribution]
 *   rather than recomputed, for the same "frozen snapshot" reason as
 *   everything else on this class.
 */
data class ContractRecord(
    val id: String,
    val matchResultId: String,
    val contractType: ContractType,
    val participants: List<ContractPartySnapshot>,
    val formalizedAt: Instant,
    val currency: String,
    val totalContribution: Double,
) {
    init {
        require(participants.size >= 2) {
            "A contract record requires at least 2 participants; got ${participants.size}."
        }
    }
}

/**
 * One participant's terms, frozen at the moment a [ContractRecord] was
 * created.
 *
 * @property userId The participant.
 * @property email The participant's account email at the time of match —
 *   the same field [RideShareIntent.creatorEmail] already carries, copied
 *   here rather than looked up later.
 * @property financialTerms What this participant agreed to contribute.
 * @property verificationTier How strongly this participant's identity was
 *   confirmed at the time of the match — relevant if a dispute later asks
 *   "how sure were we this person was who they claimed to be?"
 * @property legalConsentAcknowledgedAt When this participant accepted the
 *   platform's legal notice for the intent that became part of this
 *   match — `null` should not occur in practice (the Publish screen gates
 *   on this), but stays nullable here for the same reason
 *   [ContractIntent.legalConsentAcknowledgedAt] does.
 */
data class ContractPartySnapshot(
    val userId: String,
    val email: String,
    val financialTerms: FinancialTerms,
    val verificationTier: IdentityVerificationTier,
    val legalConsentAcknowledgedAt: Instant?,
)
