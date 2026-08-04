package com.metamatch.app.data.remote

import com.metamatch.app.domain.model.UnstructuredWish
import com.metamatch.app.domain.repository.WishRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SupabaseWishRepository — INTENTIONALLY UNFINISHED (staged for a later
 * iteration)
 * ======================================================================
 *
 * WHAT this file is: the Wish feature's twin of
 * [SupabaseRideShareRepository] and the other vertical Supabase
 * skeletons — a compiling stub showing exactly where a real shared
 * backend would plug in. See [SupabaseRideShareRepository]'s own doc
 * comment for the general steps to finish a `Supabase*Repository`.
 *
 * WHY this matters more here than for the other verticals: the whole
 * point of the Wish Globe — "the most common wish in the world right
 * now" — only becomes *true* once wishes are shared across every
 * device, not seeded per-install from [com.metamatch.app.data.mock
 * .WishSeedData]. Until this class is finished and
 * `di/RepositoryModule.kt` points at it instead of
 * [com.metamatch.app.data.mock.MockWishRepository], every statistic this
 * feature shows is demo data — see the in-app "DEMO DATA" banner on
 * `ui/wish/CastWishScreen.kt`/`WishStatsScreen.kt` and this repo's
 * `README.md`.
 *
 * The Wish-specific table would be a new `unstructured_wishes` table:
 * `id`, `creator_user_id`, `text`, `category`, `structured_contract_type`
 * (nullable), `country`, `city`, `created_at` — a direct mirror of
 * [UnstructuredWish]'s fields, with `country`/`city` indexed for the
 * scope-filtered statistics queries
 * [com.metamatch.app.domain.usecase.ComputeWishStatisticsUseCase]
 * currently computes client-side over the full in-memory list. At real
 * scale, that aggregation should move into a SQL `GROUP BY` (or a
 * materialized view refreshed periodically), not a client-side filter
 * over every row ever written.
 */
@Singleton
class SupabaseWishRepository @Inject constructor() : WishRepository {

    override fun observeAllWishes(): Flow<List<UnstructuredWish>> =
        TODO("Subscribe via Supabase Realtime to the unstructured_wishes table (all rows, not filtered by user).")

    override suspend fun castWish(wish: UnstructuredWish): UnstructuredWish =
        TODO("Insert a row into unstructured_wishes.")
}
