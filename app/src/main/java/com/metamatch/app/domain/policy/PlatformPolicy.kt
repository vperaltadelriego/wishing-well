package com.metamatch.app.domain.policy

/**
 * PlatformPolicy
 * ================
 *
 * WHAT: every adjustable business rule in MetaMatch, collected into one
 * small, swappable object — the free-tier listing limit, the micro-fee
 * charged past that limit, and the peso threshold above which ID
 * verification becomes mandatory.
 *
 * WHY it exists
 * -------------
 * Victor's brief is explicit that these numbers should NOT be baked into
 * the app permanently: "User limits and charges should be determined with
 * agility after testing the app... the app can go viral, and thus further
 * charges should be put in place, or institutions can pay a fee..." In
 * other words, the *values* below are a hypothesis to be tuned after real
 * usage data comes in — not a promise.
 *
 * The fix for "numbers that need to change often" is almost never to hunt
 * down every place a magic number like `5` or `0.10` is hard-coded through
 * the codebase. Instead, every use case and screen reads its limits from
 * this ONE object. Today [default] is a hard-coded fallback so the app
 * runs standalone in Android Studio with zero backend setup. Once
 * Supabase is wired in (Module 3/4), the exact same object will instead be
 * loaded from the `platform_policy` config table in `schema.sql` — a
 * table designed to be edited directly in the Supabase dashboard, no app
 * redeploy required. Every call site keeps working unchanged because it
 * only ever depends on this data class, never on literal numbers.
 *
 * HOW it connects to the architecture
 * ------------------------------------
 * - Injected via Hilt (see `di/AppModule.kt`) so it can be swapped for
 *   tests (`PlatformPolicy(freeActiveIntentLimit = 1, ...)`) without
 *   touching production code.
 * - Read by [com.metamatch.app.domain.usecase.CheckAntiSpamUseCase] to
 *   decide when to throw
 *   [com.metamatch.app.domain.exception.MicroFeeRequiredException].
 * - Read by (future) `MonetizationEngine` to decide when a contract needs
 *   [com.metamatch.app.domain.model.IdentityVerificationTier.ID_VERIFIED].
 *
 * @property freeActiveIntentLimit How many simultaneous, unpaid, ACTIVE
 *   intents a single user may have. The brief's starting hypothesis is 5.
 * @property microFeeAmountCents The fee — in cents of [feeCurrency] — for
 *   each additional active intent beyond [freeActiveIntentLimit]. The
 *   brief's starting hypothesis is 10 cents (i.e. $0.10).
 * @property feeCurrency Currency the micro-fee is denominated in.
 * @property highValueVerificationThreshold Contracts whose
 *   [com.metamatch.app.domain.model.FinancialTerms.amount] equals or
 *   exceeds this value require ID verification, per Module 4.
 */
data class PlatformPolicy(
    val freeActiveIntentLimit: Int = 5,
    val microFeeAmountCents: Int = 10,
    val feeCurrency: String = "MXN",
    val highValueVerificationThreshold: Double = 5_000.0,
) {
    init {
        require(freeActiveIntentLimit >= 0) { "freeActiveIntentLimit cannot be negative." }
        require(microFeeAmountCents >= 0) { "microFeeAmountCents cannot be negative." }
    }

    companion object {
        /**
         * The values baked in for standalone/demo use, matching the
         * numbers written in the original product brief. Swap this out
         * (via Hilt) once policy values are being fetched live from
         * Supabase.
         */
        fun default() = PlatformPolicy()
    }
}
