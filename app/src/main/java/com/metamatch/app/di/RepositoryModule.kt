package com.metamatch.app.di

import com.metamatch.app.data.mock.MockPizzaShareRepository
import com.metamatch.app.data.mock.MockRideShareRepository
import com.metamatch.app.data.mock.MockRoommateRepository
import com.metamatch.app.data.mock.MockWishRepository
import com.metamatch.app.domain.repository.PizzaShareRepository
import com.metamatch.app.domain.repository.RideShareRepository
import com.metamatch.app.domain.repository.RoommateRepository
import com.metamatch.app.domain.repository.WishRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * RepositoryModule
 * ==================
 *
 * WHAT: the ONE file in the entire app that decides which concrete
 * repository implementation every vertical gets injected with — one
 * `@Binds` function per vertical ([RideShareRepository],
 * [PizzaShareRepository], and whichever comes next). This is, quite
 * literally, the "RepositoryStrategy toggle" called for in the project
 * brief.
 *
 * WHY a Hilt `@Module` is the right tool for this:
 * every ViewModel and use case in the app asks Hilt for an interface
 * (`RideShareRepository`, `PizzaShareRepository`, ...), never for a
 * `Mock*Repository` or `Supabase*Repository` by name. Hilt needs exactly
 * one place told "when someone asks for this interface, hand them THIS
 * class" — that instruction lives here, and nowhere else, for every
 * vertical.
 *
 * HOW to flip a vertical's toggle from mock data to a real Supabase
 * backend, once its `Supabase*Repository` is finished: change the single
 * line inside that vertical's `@Binds` function from `Mock*Repository` to
 * `Supabase*Repository`. Every ViewModel, use case, and Compose screen in
 * the app keeps working completely unmodified, because they never
 * depended on which implementation was chosen — only on the repository
 * interface.
 *
 * Kotlin/Hilt note: `@Binds` is a lighter-weight alternative to `@Provides`
 * used specifically for "interface X should resolve to implementation Y"
 * mappings. The function body is never actually executed — Hilt only
 * reads the function *signature* (parameter type -> return type) to know
 * what to wire together, which is why the function can be `abstract` with
 * no body at all.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRideShareRepository(
        mockRideShareRepository: MockRideShareRepository,
    ): RideShareRepository

    @Binds
    @Singleton
    abstract fun bindPizzaShareRepository(
        mockPizzaShareRepository: MockPizzaShareRepository,
    ): PizzaShareRepository

    @Binds
    @Singleton
    abstract fun bindRoommateRepository(
        mockRoommateRepository: MockRoommateRepository,
    ): RoommateRepository

    @Binds
    @Singleton
    abstract fun bindWishRepository(
        mockWishRepository: MockWishRepository,
    ): WishRepository
}
