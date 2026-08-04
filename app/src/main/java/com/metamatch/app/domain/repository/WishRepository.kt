package com.metamatch.app.domain.repository

import com.metamatch.app.domain.model.UnstructuredWish
import kotlinx.coroutines.flow.Flow

/**
 * WishRepository
 * ================
 *
 * WHAT: the single contract the Wish feature uses to read and write
 * [UnstructuredWish]es. Deliberately **not** a
 * [ContractRepository]&lt;T, M&gt; — that generic interface exists for
 * verticals that match, persist a result, and formalize a contract; an
 * unstructured wish does none of that (see [UnstructuredWish]'s own
 * class doc for the full reasoning). Its interface is correspondingly
 * much smaller.
 *
 * WHY it's still an `interface` with a Mock/Supabase split, same as
 * every other repository in this app: `di/RepositoryModule.kt` stays the
 * one file that decides which implementation is active. Today that's
 * [com.metamatch.app.data.mock.MockWishRepository], seeded from
 * `data/mock/WishSeedData.kt` — no real backend is wired in yet
 * (`SupabaseWishRepository` is a `TODO()` skeleton, same as the other
 * three verticals' Supabase stubs).
 */
interface WishRepository {

    /** Every wish cast so far — a live [Flow] so the Cast/Stats screens
     * update immediately after a new wish lands, no manual refresh. */
    fun observeAllWishes(): Flow<List<UnstructuredWish>>

    /** Persists a brand-new wish, returning the stored value. */
    suspend fun castWish(wish: UnstructuredWish): UnstructuredWish
}
