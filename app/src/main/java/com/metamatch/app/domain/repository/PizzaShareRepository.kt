package com.metamatch.app.domain.repository

import com.metamatch.app.domain.model.PizzaMatchResult
import com.metamatch.app.domain.model.PizzaShareIntent

/**
 * PizzaShareRepository
 * ======================
 *
 * WHAT: the single contract the Meta-Match Pizza vertical uses to read
 * and write shared-purchase data — every operation [ContractRepository]
 * defines, specialized to [PizzaShareIntent] and [PizzaMatchResult].
 *
 * WHY a separate interface instead of every screen depending on
 * `ContractRepository<PizzaShareIntent, PizzaMatchResult>` directly:
 * a short, named interface per vertical is easier to inject with Hilt
 * (see `di/RepositoryModule.kt`) and easier to read at call sites
 * (`PizzaShareRepository` reads as "the Pizza data source"; the generic
 * form doesn't). See [RideShareRepository]'s docs for the twin
 * explanation on the Ride side.
 *
 * Two implementations exist, chosen by `di/RepositoryModule.kt`:
 * [com.metamatch.app.data.mock.MockPizzaShareRepository] (active today)
 * and `SupabasePizzaShareRepository` (skeleton, staged for later).
 */
interface PizzaShareRepository : ContractRepository<PizzaShareIntent, PizzaMatchResult>
