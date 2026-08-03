package com.metamatch.app.domain.exception

/**
 * MicroFeeRequiredException
 * ==========================
 *
 * WHAT: thrown by [com.metamatch.app.domain.usecase.CheckAntiSpamUseCase]
 * when a user tries to publish beyond their free-tier limit of
 * simultaneous active intents (Module 2, "Anti-Spam & Rate Limiting").
 *
 * WHY a custom exception instead of returning `false`/`null`:
 * "You are over your free limit" is not really an *error* the way a
 * network timeout is — it is an expected, meaningful business outcome the
 * UI needs to react to specifically (show a "pay $0.10 to publish anyway"
 * dialog, not a generic error toast). Kotlin note: modeling this as a
 * checked-in-spirit, specific exception type (rather than a generic
 * `Exception` or a boolean flag) lets calling code use `catch
 * (e: MicroFeeRequiredException)` to handle *exactly* this situation,
 * while any real, unexpected failure still propagates normally.
 *
 * An alternative, equally valid Kotlin style would model this as a sealed
 * `Result` type instead of throwing (e.g. `PublishResult.NeedsMicroFee`).
 * We chose an exception here specifically because "the 6th listing"
 * is the one abnormal control-flow branch out of an otherwise
 * straightforward, linear publish operation — throwing keeps the happy
 * path in [com.metamatch.app.domain.usecase.PublishRideIntentUseCase]
 * readable, at the small cost of requiring a `try/catch` at the one call
 * site that needs to react to it (the Publish Intent ViewModel).
 *
 * HOW it connects to the architecture
 * ------------------------------------
 * Thrown by: [com.metamatch.app.domain.usecase.CheckAntiSpamUseCase].
 * Caught by: the Publish Intent screen's ViewModel, which then shows the
 * user a confirmation dialog quoting [feeAmountCents] before retrying the
 * publish call with `acceptsMicroFee = true`.
 *
 * @property currentActiveCount How many active intents the user already
 *   has (always >= the policy's free limit when this is thrown).
 * @property freeLimit The free-tier limit that was exceeded, echoed back
 *   so the UI can render "You have 6/5 free listings" without a second
 *   lookup.
 * @property feeAmountCents The fee, in cents, required to publish anyway.
 * @property feeCurrency Currency the fee is denominated in.
 */
class MicroFeeRequiredException(
    val currentActiveCount: Int,
    val freeLimit: Int,
    val feeAmountCents: Int,
    val feeCurrency: String,
) : Exception(
    "User already has $currentActiveCount active intents (free limit: $freeLimit). " +
        "Publishing another one requires a micro-fee of $feeAmountCents cents $feeCurrency."
)
