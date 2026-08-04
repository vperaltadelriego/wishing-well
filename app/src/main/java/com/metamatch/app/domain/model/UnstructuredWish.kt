package com.metamatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * UnstructuredWish
 * ==================
 *
 * WHAT: a wish with no structure at all — "deseo la paz del mundo,"
 * "deseo que mi perro no hubiera sido atropellado." No matching, no
 * financial terms, no contract lifecycle. This is deliberately **not** a
 * [ContractIntent] subclass — see the "Where this fits architecturally"
 * discussion in this feature's own planning notes and
 * `anatomía_de_meta-match_finder.md`: forcing an expression with nothing
 * to match on through the engine's matching abstraction would mean
 * carrying fields (`financialTerms`, `verificationTier`, `status`
 * lifecycle) that make no sense for it. Every other model in
 * `domain/model/` inherits from `ContractIntent` *because* it represents
 * "I want to be matched with someone for X" — this is the one concept in
 * the app that genuinely doesn't.
 *
 * WHY [structuredContractType] exists on an otherwise-unstructured
 * model: a handful of seeded wishes represent someone who, in the same
 * gesture of "tossing a wish in the well," was actually looking for
 * something specific (a ride, a pizza split, a roommate). Reusing
 * [ContractType] here — rather than inventing a parallel string enum —
 * keeps that flavor honest without pretending this model participates in
 * real matching; it is **display-only**, read by
 * [com.metamatch.app.domain.usecase.ComputeWishStatisticsUseCase] to
 * report "X% of wishes were already looking for something specific," and
 * never joined against the real Ride/Pizza/Roomie repositories.
 *
 * @property text The wish itself, verbatim, as the person typed it.
 * @property category See [WishCategory] — assigned by
 *   [com.metamatch.app.domain.usecase.CategorizeWishTextUseCase].
 * @property structuredContractType `null` for a pure expression wish;
 *   set only on the small subset of seeded entries standing in for "this
 *   wish was actually a structured request in disguise."
 * @property country Free text (e.g. "México") — the coarsest scope
 *   [com.metamatch.app.domain.model.WishScope] statistics group by.
 * @property city Free text (e.g. "Cancún") — the finest scope.
 */
data class UnstructuredWish(
    val id: String,
    val creatorUserId: String,
    val text: String,
    val category: WishCategory,
    val structuredContractType: ContractType? = null,
    val country: String,
    val city: String,
    val createdAt: Instant,
) {
    init {
        require(text.isNotBlank()) { "A wish cannot be blank." }
    }
}
