package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.UnstructuredWish
import com.metamatch.app.domain.repository.WishRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FakeWishRepository
 * =====================
 * A minimal, test-only implementation of [WishRepository] — same role as
 * [FakeRideShareRepository]/[FakePizzaShareRepository]/
 * [FakeRoommateRepository]: lets a test seed exactly the wishes it cares
 * about instead of depending on the large [com.metamatch.app.data.mock
 * .WishSeedData] pool.
 */
class FakeWishRepository(initialWishes: List<UnstructuredWish> = emptyList()) : WishRepository {

    private val wishesFlow = MutableStateFlow(initialWishes)

    override fun observeAllWishes() = wishesFlow.asStateFlow()

    override suspend fun castWish(wish: UnstructuredWish): UnstructuredWish {
        wishesFlow.value = wishesFlow.value + wish
        return wish
    }
}
