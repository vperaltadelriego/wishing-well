package com.metamatch.app.di

import com.metamatch.app.data.mock.MockRideShareRepository
import com.metamatch.app.domain.repository.RideShareRepository
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
 * [RideShareRepository] implementation everything else gets injected
 * with. This is, quite literally, the "RepositoryStrategy toggle" called
 * for in the project brief.
 *
 * WHY a Hilt `@Module` is the right tool for this:
 * every ViewModel and use case in the app asks Hilt for a
 * `RideShareRepository` (the interface), never for a
 * `MockRideShareRepository` or `SupabaseRideShareRepository` by name. Hilt
 * needs exactly one place told "when someone asks for the interface, hand
 * them THIS class" — that instruction lives here, and nowhere else.
 *
 * HOW to flip the toggle from mock data to a real Supabase backend, once
 * `SupabaseRideShareRepository` is finished (see that class's own
 * documentation for the remaining steps): change the single line inside
 * [bindRideShareRepository] from `MockRideShareRepository` to
 * `SupabaseRideShareRepository`. Every ViewModel, use case, and Compose
 * screen in the app keeps working completely unmodified, because they
 * never depended on which implementation was chosen — only on the
 * `RideShareRepository` interface.
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
}
