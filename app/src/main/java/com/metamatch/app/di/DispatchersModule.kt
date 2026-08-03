package com.metamatch.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * DispatchersModule
 * ===================
 *
 * WHAT: provides the [CoroutineDispatcher]s ViewModels use to decide
 * *which thread* their coroutines run on — `Default` for CPU-bound work
 * (the greedy clustering math in [com.metamatch.app.domain.usecase
 * .FindMatchesUseCase]) and `Main` for anything that has to touch Compose
 * state directly.
 *
 * WHY inject dispatchers instead of hard-coding `Dispatchers.IO` /
 * `Dispatchers.Default` inside a ViewModel:
 * a ViewModel that calls `Dispatchers.Default` directly cannot be unit
 * tested deterministically — tests want a *single-threaded test
 * dispatcher* so coroutines run predictably and finish before assertions
 * run. By injecting the dispatcher instead, a test can substitute
 * `kotlinx-coroutines-test`'s `StandardTestDispatcher` with zero changes
 * to the ViewModel itself. This is the same Dependency Inversion idea
 * used for [com.metamatch.app.domain.repository.RideShareRepository],
 * applied to *threading* instead of *data access*.
 *
 * HOW the qualifiers work: Kotlin/Hilt cannot tell two
 * `CoroutineDispatcher` providers apart by return type alone (both
 * `@Provides` functions below return the exact same type). A `@Qualifier`
 * annotation acts as a compile-time label Hilt uses to disambiguate which
 * one a given `@Inject` constructor parameter wants — see
 * `@DefaultDispatcher` and `@MainDispatcher` used below.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @DefaultDispatcher
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}

/** CPU-bound work: geometry math, sorting, filtering. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** UI-thread work: anything that touches Compose state or Views directly. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
