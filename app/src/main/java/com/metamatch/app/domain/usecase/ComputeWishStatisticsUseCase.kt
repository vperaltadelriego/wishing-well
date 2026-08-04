package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.LATIN_AMERICAN_COUNTRIES
import com.metamatch.app.domain.model.UnstructuredWish
import com.metamatch.app.domain.model.WishCategory
import com.metamatch.app.domain.model.WishScope
import com.metamatch.app.domain.model.WishStatistics
import com.metamatch.app.domain.repository.WishRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * ComputeWishStatisticsUseCase
 * ===============================
 *
 * WHAT: turns the full pool of [UnstructuredWish]es into a
 * [WishStatistics] snapshot for one [WishScope] — "what has the world
 * (or Latin America, or México, or Cancún) been wishing for?"
 *
 * HOW scope filtering works: [WishScope.World] takes everything;
 * [WishScope.Region] currently only supports grouping by
 * [LATIN_AMERICAN_COUNTRIES] (the one region the product brief asked
 * for — "el deseo... de Latinoamérica"); [WishScope.Country]/
 * [WishScope.City] do a case-insensitive exact match against
 * [UnstructuredWish.country]/[UnstructuredWish.city]. This is the same
 * "exact string match, not fuzzy geography" simplification
 * [PizzaShareIntent.establishment] and [RoommateIntent.zone] already use
 * elsewhere in this codebase.
 */
class ComputeWishStatisticsUseCase @Inject constructor(
    private val repository: WishRepository,
) {
    suspend operator fun invoke(scope: WishScope): WishStatistics {
        val scoped = repository.observeAllWishes().first().filter { wish -> matchesScope(wish, scope) }
        val total = scoped.size

        if (total == 0) {
            return WishStatistics(
                scopeLabel = labelFor(scope),
                totalWishes = 0,
                mostCommonCategory = null,
                mostCommonCategoryCount = 0,
                impossibleWishPercent = 0.0,
                deceasedLovedOnePercent = 0.0,
                structuredWishPercent = 0.0,
            )
        }

        val mostCommonEntry = scoped.groupingBy { it.category }.eachCount().maxByOrNull { it.value }
        val impossibleCount = scoped.count { it.category == WishCategory.IMPOSSIBLE }
        val deceasedCount = scoped.count { it.category == WishCategory.DECEASED_LOVED_ONE }
        val structuredCount = scoped.count { it.structuredContractType != null }

        return WishStatistics(
            scopeLabel = labelFor(scope),
            totalWishes = total,
            mostCommonCategory = mostCommonEntry?.key,
            mostCommonCategoryCount = mostCommonEntry?.value ?: 0,
            impossibleWishPercent = percentOf(impossibleCount, total),
            deceasedLovedOnePercent = percentOf(deceasedCount, total),
            structuredWishPercent = percentOf(structuredCount, total),
        )
    }

    private fun matchesScope(wish: UnstructuredWish, scope: WishScope): Boolean = when (scope) {
        is WishScope.World -> true
        is WishScope.Region -> wish.country in LATIN_AMERICAN_COUNTRIES
        is WishScope.Country -> wish.country.equals(scope.name, ignoreCase = true)
        is WishScope.City -> wish.city.equals(scope.name, ignoreCase = true)
    }

    private fun labelFor(scope: WishScope): String = when (scope) {
        is WishScope.World -> "World"
        is WishScope.Region -> scope.name
        is WishScope.Country -> scope.name
        is WishScope.City -> scope.name
    }

    private fun percentOf(count: Int, total: Int): Double = (count.toDouble() / total) * 100.0
}
