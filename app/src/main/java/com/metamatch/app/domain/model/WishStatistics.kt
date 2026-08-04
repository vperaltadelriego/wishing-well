package com.metamatch.app.domain.model

/**
 * WishStatistics
 * ================
 *
 * WHAT: the aggregate numbers [com.metamatch.app.domain.usecase
 * .ComputeWishStatisticsUseCase] produces for one [WishScope] — the
 * payoff of tossing an unstructured wish in: seeing what everyone else
 * has been wishing for.
 *
 * @property scopeLabel Human-readable label for the scope this was
 *   computed over (e.g. "World," "Latin America," "México," "Cancún") —
 *   kept as a plain string here so the UI never has to re-derive display
 *   text from a [WishScope] value.
 * @property totalWishes How many wishes fell within this scope.
 * @property mostCommonCategory `null` only if [totalWishes] is `0`.
 * @property mostCommonCategoryCount How many wishes shared
 *   [mostCommonCategory].
 * @property impossibleWishPercent % of wishes in this scope tagged
 *   [WishCategory.IMPOSSIBLE].
 * @property deceasedLovedOnePercent % tagged
 *   [WishCategory.DECEASED_LOVED_ONE].
 * @property structuredWishPercent % carrying a non-null
 *   [UnstructuredWish.structuredContractType] — "already looking for
 *   something specific" rather than a pure expression.
 */
data class WishStatistics(
    val scopeLabel: String,
    val totalWishes: Int,
    val mostCommonCategory: WishCategory?,
    val mostCommonCategoryCount: Int,
    val impossibleWishPercent: Double,
    val deceasedLovedOnePercent: Double,
    val structuredWishPercent: Double,
)
