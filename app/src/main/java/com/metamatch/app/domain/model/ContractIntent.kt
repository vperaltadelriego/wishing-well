package com.metamatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * ContractIntent
 * ===============
 *
 * WHAT this is
 * ------------
 * The single, generic shape of "I want to be matched with someone for X."
 * Every concrete kind of match request in MetaMatch (a ride share today; a
 * study-group session, an equipment loan, or anything else tomorrow) is
 * represented as a subclass of this abstract class.
 *
 * WHY it exists
 * -------------
 * MetaMatch's pitch is a *general* matching engine, not just a carpool app.
 * If every feature module invented its own "request" model from scratch,
 * the matching, anti-spam, and monetization logic would have to be
 * duplicated once per module. By pulling the fields that are common to
 * *any* kind of match request into one abstract class, Module 1 (the core
 * engine: anti-spam counting, integrity scoring, fee rules) can operate on
 * `ContractIntent` without knowing or caring whether the concrete intent is
 * a ride, a loan, or something invented next year.
 *
 * This is the Kotlin/OOP idiom called "program to an abstraction, not a
 * concrete implementation" — the same idea behind interfaces in Java or
 * abstract base classes in Python's `abc` module, if that comparison helps.
 *
 * HOW it connects to the architecture
 * ------------------------------------
 * - Lives in the **domain** layer: no Android imports, no Compose, no
 *   Supabase types. Pure Kotlin so it can be unit-tested instantly and
 *   (later) reused in a non-Android context such as a KMP shared module.
 * - `RideShareIntent` (Module 2) is the first concrete subclass.
 * - `CheckAntiSpamUseCase` counts a user's *active* `ContractIntent`s
 *   regardless of subtype to enforce the "5 free listings" rule.
 * - `UserIntegrityScore` is updated based on how a `ContractIntent`
 *   resolves (`FULFILLED` vs `CANCELLED`).
 *
 * @property id Unique identifier for this intent (UUID string). Assigned
 *   client-side when the user taps "Publish", so the UI can show it
 *   optimistically before the backend confirms it.
 * @property creatorUserId The user who published this intent.
 * @property contractType What kind of match this is. A Kotlin `enum class`
 *   is used instead of a raw `String` so the compiler rejects typos like
 *   `"ridesharee"` at compile time instead of failing silently at runtime.
 * @property createdAt When the intent was published.
 * @property scheduledAt When the *match itself* should happen. This is
 *   deliberately independent from `createdAt`: MetaMatch explicitly
 *   supports publishing an intent today for an event years in the future
 *   (e.g. a flight landing on Oct 10, 2030), so `scheduledAt` can be far
 *   ahead of `createdAt`.
 * @property expiresAt Time-to-live: after this instant, an unmatched
 *   intent should stop being surfaced to other users and stop counting
 *   against the free-tier limit. `null` means "does not expire on its
 *   own" (rare; most intents should set this a little after
 *   `scheduledAt`).
 * @property verificationTier How strongly this user's identity has been
 *   verified. Read by `MonetizationEngine` to decide whether a contract is
 *   even allowed to proceed (see [IdentityVerificationTier]).
 * @property financialTerms The money side of the contract. See
 *   [FinancialTerms] below — kept as its own small data class so it can be
 *   reused by every future contract type, not just ride shares.
 * @property status Current lifecycle state of the intent. See
 *   [ContractStatus].
 * @property legalConsentAcknowledgedAt When the creator explicitly
 *   accepted the platform's legal notice (the platform is an intermediary
 *   only, not a party to the resulting agreement; users must verify their
 *   counterpart in person; etc. — see `ui/components/LegalNoticeCard.kt`)
 *   for *this specific intent*, captured at publish time. `null` means not
 *   yet acknowledged. This is what lets a future
 *   [com.metamatch.app.domain.model.ContractRecord] prove, after the fact,
 *   that every party to a match actually agreed to the platform's terms —
 *   the first piece of data needed to formalize a written contract if one
 *   is ever required.
 */
abstract class ContractIntent(
    open val id: String,
    open val creatorUserId: String,
    open val contractType: ContractType,
    open val createdAt: Instant,
    open val scheduledAt: Instant,
    open val expiresAt: Instant?,
    open val verificationTier: IdentityVerificationTier,
    open val financialTerms: FinancialTerms,
    open val status: ContractStatus,
    open val legalConsentAcknowledgedAt: Instant?,
)

/**
 * The catalogue of match types MetaMatch understands. Starting with
 * [RIDE_SHARE] only (Module 2); the other cases are placeholders that show
 * how the engine is meant to grow without touching Module 1's logic.
 *
 * Kotlin note: an `enum class` is a fixed, closed set of named constants.
 * Unlike a `String`, the compiler *guarantees* every `when` you write over
 * a `ContractType` either handles every case or is forced to add an `else`
 * — that guarantee is what makes enums safer than "stringly typed" code.
 */
enum class ContractType {
    RIDE_SHARE,
    PIZZA_SHARE,

    // Not implemented yet — listed to document where the engine is headed.
    ROOMMATE_SEARCH,
}

/**
 * Lifecycle of a single [ContractIntent].
 *
 * ACTIVE      -> visible to other users, counts against the free-tier limit.
 * MATCHED     -> paired with other intents into a [MatchResult]; no longer
 *                surfaced as a candidate for new matches.
 * FULFILLED   -> the real-world event happened (e.g. the ride was taken).
 *                Feeds a *positive* update into [UserIntegrityScore].
 * CANCELLED   -> the creator backed out before fulfillment. Feeds a
 *                *negative* update into [UserIntegrityScore].
 * EXPIRED     -> `expiresAt` passed with no match found.
 */
enum class ContractStatus {
    ACTIVE,
    MATCHED,
    FULFILLED,
    CANCELLED,
    EXPIRED,
}

/**
 * How strongly a user's real-world identity has been confirmed.
 *
 * Read by [com.metamatch.app.domain.usecase.PublishRideIntentUseCase] and
 * the (future) `MonetizationEngine` to gate high-value contracts: per the
 * brief, anything over $5,000 MXN, or a real-estate transaction, requires
 * at least [ID_VERIFIED].
 */
enum class IdentityVerificationTier {
    /** Only an email address has been confirmed. Default for new users. */
    EMAIL_ONLY,

    /** Phone number confirmed via SMS/OTP. */
    PHONE_VERIFIED,

    /** Official government ID (INE/Passport) has been checked. */
    ID_VERIFIED,
}

/**
 * The money terms of a contract, deliberately separated from
 * [ContractIntent] itself so any future contract type can reuse it as-is.
 *
 * Kotlin note: this is a `data class`. Marking a class `data` asks the
 * compiler to auto-generate `equals()`, `hashCode()`, `toString()`, and a
 * `copy()` function for you — invaluable for small immutable value objects
 * like this one, where two `FinancialTerms` with the same fields really
 * should be considered equal.
 *
 * @property amount The user's individual contribution or budget. `0.0`
 *   means "free ride" — explicitly allowed by the brief.
 * @property currency ISO 4217 code. Defaults to Mexican pesos because the
 *   first pilot communities (UCaribe, Anáhuac, Xcaret) are all in Mexico,
 *   but nothing else in the domain layer assumes a specific currency.
 * @property requiresEscrow Whether the platform should pre-authorize/hold
 *   funds before the match is confirmed (relevant for the per-transaction
 *   fee described in Module 4). Not used by the free MVP flow yet.
 */
data class FinancialTerms(
    val amount: Double,
    val currency: String = "MXN",
    val requiresEscrow: Boolean = false,
) {
    init {
        require(amount >= 0.0) { "A contribution amount cannot be negative." }
    }

    /** Convenience used across the UI layer to label $0 offers as "free". */
    val isFree: Boolean get() = amount == 0.0
}
