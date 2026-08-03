package com.metamatch.app.domain.repository

import com.metamatch.app.domain.model.MatchResult
import com.metamatch.app.domain.model.RideShareIntent

/**
 * RideShareRepository
 * =====================
 *
 * WHAT: the single contract the rest of the app uses to read and write
 * ride-share data — every operation a [ContractRepository] defines,
 * specialized to [RideShareIntent] and [MatchResult]. It says *what*
 * operations exist, never *how* they are carried out.
 *
 * WHY it exists (the Repository pattern + the "RepositoryStrategy toggle")
 * -----------------------------------------------------------------------
 * This is the seam the whole "run standalone in Android Studio, deploy
 * against real Supabase later" requirement hangs on. ViewModels and use
 * cases are written once, against this interface, and never import a
 * concrete class directly. Two implementations exist:
 *
 *   - [com.metamatch.app.data.mock.MockRideShareRepository] — pure
 *     in-memory Kotlin, zero network calls, seeded with realistic sample
 *     data. This is what makes the app buildable and demoable the moment
 *     it is cloned, with no Supabase project, no API keys, nothing.
 *   - `SupabaseRideShareRepository` (stub in this pass, fully wired in a
 *     later iteration) — talks to Postgres/PostGIS/Realtime over the
 *     Supabase Kotlin SDK, using the schema in `schema.sql`.
 *
 * Hilt's `di/RepositoryModule.kt` decides, in exactly one place, which of
 * the two gets constructed — everything else in the app is unaffected by
 * that choice. This is the Dependency Inversion Principle (the "D" in
 * SOLID) in practice: high-level code (use cases) depends on an
 * abstraction (this interface), and low-level code (the two repository
 * classes) depends on that same abstraction too, instead of high-level
 * code depending directly on low-level details.
 *
 * HOW this became a one-line interface: every method this used to declare
 * directly was identical, method-for-method, to what
 * [com.metamatch.app.domain.repository.PizzaShareRepository] needed once
 * Meta-Match Pizza (Stage 2) landed — the only difference was which
 * concrete [com.metamatch.app.domain.model.ContractIntent] and match-
 * result types were involved. [ContractRepository] pulls that common
 * shape out once; see its own docs for why this wasn't done back when
 * Ride was the only vertical (it would have been a speculative
 * abstraction with no second case to prove it against).
 */
interface RideShareRepository : ContractRepository<RideShareIntent, MatchResult>
