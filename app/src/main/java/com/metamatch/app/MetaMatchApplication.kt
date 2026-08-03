package com.metamatch.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * MetaMatchApplication
 * =======================
 *
 * WHAT: the app's `Application` subclass — the very first object Android
 * creates when the process starts, before any `Activity`.
 *
 * WHY it exists and why it is (almost) empty: Hilt needs exactly one
 * `Application` class annotated `@HiltAndroidApp` per app. That
 * annotation triggers Hilt's code generation for the app-wide dependency
 * graph (the "SingletonComponent" that every `@Singleton`-scoped class in
 * `di/` lives in) — everything downstream (`MainActivity`, every
 * `@HiltViewModel`) depends on this class existing and being registered
 * in `AndroidManifest.xml` via `android:name=".MetaMatchApplication"`.
 * Beyond that annotation, this class deliberately has no other logic —
 * any real app-wide initialization (crash reporting, analytics) would go
 * in `onCreate()` here, but none is needed for this MVP.
 *
 * HOW it connects to the architecture: every class in `di/` (e.g.
 * `RepositoryModule`, `DispatchersModule`) is installed into the
 * `SingletonComponent`, which is scoped to the lifetime of this
 * `Application` instance — i.e. "lives as long as the app process does."
 */
@HiltAndroidApp
class MetaMatchApplication : Application()
