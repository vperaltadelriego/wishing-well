package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.ContractType
import com.metamatch.app.domain.model.UnstructuredWish
import com.metamatch.app.domain.model.WishCategory
import com.metamatch.app.domain.model.WishScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ComputeWishStatisticsUseCaseTest
 * ===================================
 * Verifies scope filtering (World / Region / Country / City) and the
 * percentage math against a small, hand-built pool of wishes with known
 * categories and locations — the Wish vertical's twin of
 * `FindMatchesUseCaseTest`'s approach.
 */
class ComputeWishStatisticsUseCaseTest {

    private val instant = Instant.parse("2026-08-01T10:00:00-06:00")

    private fun wish(
        id: String,
        category: WishCategory,
        country: String,
        city: String,
        structuredContractType: ContractType? = null,
    ) = UnstructuredWish(
        id = id,
        creatorUserId = "user-$id",
        text = "some wish",
        category = category,
        structuredContractType = structuredContractType,
        country = country,
        city = city,
        createdAt = instant,
    )

    // 5 wishes total: 2x LOVE in Cancún/México, 1x IMPOSSIBLE in CDMX/
    // México, 1x DECEASED_LOVED_ONE in Bogotá/Colombia, 1x OTHER
    // (structured) in Miami/Estados Unidos.
    private val pool = listOf(
        wish("w1", WishCategory.LOVE, "México", "Cancún"),
        wish("w2", WishCategory.LOVE, "México", "Cancún"),
        wish("w3", WishCategory.IMPOSSIBLE, "México", "Ciudad de México"),
        wish("w4", WishCategory.DECEASED_LOVED_ONE, "Colombia", "Bogotá"),
        wish("w5", WishCategory.OTHER, "Estados Unidos", "Miami", ContractType.RIDE_SHARE),
    )

    @Test
    fun `world scope includes every wish`() = runTest {
        val stats = ComputeWishStatisticsUseCase(FakeWishRepository(pool))(WishScope.World)

        assertEquals(5, stats.totalWishes)
        assertEquals(WishCategory.LOVE, stats.mostCommonCategory)
        assertEquals(2, stats.mostCommonCategoryCount)
        assertEquals(20.0, stats.impossibleWishPercent, 0.01)
        assertEquals(20.0, stats.deceasedLovedOnePercent, 0.01)
        assertEquals(20.0, stats.structuredWishPercent, 0.01)
    }

    @Test
    fun `country scope filters to Mexico only`() = runTest {
        val stats = ComputeWishStatisticsUseCase(FakeWishRepository(pool))(WishScope.Country("México"))

        assertEquals(3, stats.totalWishes)
        assertEquals(WishCategory.LOVE, stats.mostCommonCategory)
        assertEquals(33.33, stats.impossibleWishPercent, 0.1)
        assertEquals(0.0, stats.deceasedLovedOnePercent, 0.01)
    }

    @Test
    fun `city scope filters to a single city`() = runTest {
        val stats = ComputeWishStatisticsUseCase(FakeWishRepository(pool))(WishScope.City("Cancún"))

        assertEquals(2, stats.totalWishes)
        assertEquals(WishCategory.LOVE, stats.mostCommonCategory)
        assertEquals(2, stats.mostCommonCategoryCount)
    }

    @Test
    fun `latin america region excludes non-latin-american countries`() = runTest {
        val stats = ComputeWishStatisticsUseCase(FakeWishRepository(pool))(WishScope.Region("Latin America"))

        // México (3) + Colombia (1) = 4; Estados Unidos is excluded.
        assertEquals(4, stats.totalWishes)
        assertEquals(25.0, stats.deceasedLovedOnePercent, 0.01)
    }

    @Test
    fun `an empty scope reports zero without dividing by zero`() = runTest {
        val stats = ComputeWishStatisticsUseCase(FakeWishRepository(pool))(WishScope.City("Nowhere"))

        assertEquals(0, stats.totalWishes)
        assertEquals(null, stats.mostCommonCategory)
        assertEquals(0.0, stats.impossibleWishPercent, 0.01)
    }
}
