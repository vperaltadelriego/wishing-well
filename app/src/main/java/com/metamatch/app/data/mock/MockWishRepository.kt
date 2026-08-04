package com.metamatch.app.data.mock

import com.metamatch.app.domain.model.UnstructuredWish
import com.metamatch.app.domain.repository.WishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MockWishRepository
 * =====================
 *
 * WHAT: an in-memory, zero-network implementation of [WishRepository] —
 * seeded from [WishSeedData] so the Wish Globe's statistics look real
 * the moment the app is cloned and run, the same "zero setup" promise
 * every other mock repository in this app keeps.
 *
 * WHY there is no anti-spam/policy/integrity-score machinery here, unlike
 * [MockRideShareRepository]/[MockPizzaShareRepository]/
 * [MockRoommateRepository]: [WishRepository]'s interface is intentionally
 * much smaller than [com.metamatch.app.domain.repository
 * .ContractRepository] — see that interface's own docs for why an
 * [UnstructuredWish] doesn't participate in any of that.
 */
@Singleton
class MockWishRepository @Inject constructor() : WishRepository {

    private val wishesFlow = MutableStateFlow(WishSeedData.buildSeedWishes())

    override fun observeAllWishes(): Flow<List<UnstructuredWish>> = wishesFlow.asStateFlow()

    override suspend fun castWish(wish: UnstructuredWish): UnstructuredWish {
        wishesFlow.update { current -> current + wish }
        return wish
    }
}
