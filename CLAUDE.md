# CLAUDE.md — Wishing Well (MetaMatch engine) repo map (for AI agents)

Read this file first. It is written to answer "where is X" and "how does this
fit together" in the fewest tokens possible, so an agent does not need to
read every source file to make a safe change. Full human-oriented context
(setup, screenshots, roadmap) lives in `README.md`; deep design rationale
lives in the doc comments (WHAT/WHY/HOW) at the top of each source file.

## Brand name vs. engine name

**"Wishing Well"** is the user-facing product name (app label, intro
screen, Play Store listing if this ever ships) — "a meta-finder for wild
coincidences," with a wishing-well visual motif (toss a coin = publish an
intent, a match = a coincidence). **"MetaMatch"** is the underlying
general-purpose matching engine's name, and is what the Kotlin package
(`com.metamatch.app`), class names (`MetaMatchTheme`,
`MetaMatchApplication`), and this repo's folder name still use — credited
on the intro screen as "powered by the MetaMatch engine." Do not rename
package/class identifiers to "Wishing Well"; that naming split (branded
product on top of a named engine) is intentional.

## What this project is

Native Android app, Kotlin + Jetpack Compose. A general-purpose "meta-match"
engine, first applied to ride-share carpooling (Module 2). Clean
Architecture + MVVM + Hilt DI. Runs standalone with an in-memory mock
repository; a Supabase (Postgres + PostGIS) backend is designed but not
wired in yet.

Status: **MVP iteration 1 of 4** — Module 1 (core engine) + Module 2 (ride
share matching) are implemented and buildable. Module 3 (chat/handshake
ticket) and Module 4 (monetization engine, beyond the anti-spam micro-fee)
are intentionally deferred — see "Not yet implemented" below. The app is
a multi-vertical hub: **Ride** and **Pizza** are both live; **Roomie** is
still a "coming soon" card in `ui/hub/HubScreen.kt`, staged as its own
planned/approved stage before being built.

## Stack / versions (see `gradle/libs.versions.toml` for exact numbers)

Kotlin 2.2.10 · AGP 9.3.1 · Compose BOM 2026.02.01 · Hilt 2.59.2 (via KSP) ·
Navigation-Compose 2.9.8 · kotlinx-coroutines · kotlinx-datetime ·
minSdk 24 / targetSdk 37.

## Package layout (`app/src/main/java/com/metamatch/app/`)

```
domain/model/       Pure Kotlin data classes. No Android imports. Zero deps.
                     ContractIntent (abstract base, carries
                     legalConsentAcknowledgedAt), RideShareIntent,
                     PizzaShareIntent, MatchResult, PizzaMatchResult,
                     ContractRecord/ContractPartySnapshot (the frozen
                     "this match = a contract" data captured on
                     acceptance), UserRule (sealed), UserIntegrityScore,
                     GeoPoint (Haversine distance + centroid math).
domain/policy/       PlatformPolicy — every tunable business rule (free-tier
                     limit, micro-fee, verification threshold) in ONE place.
domain/exception/    MicroFeeRequiredException.
domain/repository/   ContractRepository<T, M> — the generic seam every
                     vertical's repository extends (getActiveIntentsForUser,
                     publishIntent, formalizeContract, getPlatformPolicy,
                     etc.). RideShareRepository and PizzaShareRepository are
                     both one-line interfaces over it. Every use
                     case/ViewModel depends on its own vertical's interface
                     only, never on a concrete implementation.
domain/usecase/      PublishRideIntentUseCase, FindMatchesUseCase (Ride's
                     geospatial/budget matching — Kotlin mirror of
                     schema.sql's PostGIS functions); PublishPizzaIntentUseCase,
                     FindPizzaMatchesUseCase (Pizza's proximity + fractional-
                     unit matching). AntiSpamRules.kt holds the ONE
                     anti-spam rule both CheckAntiSpamUseCase (Ride) and
                     CheckPizzaAntiSpamUseCase (Pizza) delegate to.
data/mock/           MockRideShareRepository, MockPizzaShareRepository —
                     in-memory, seeded, what the app runs on by default.
                     @Singleton, no network.
data/remote/         SupabaseRideShareRepository, SupabasePizzaShareRepository
                     — UNFINISHED skeletons (every method is `TODO()`). See
                     each file's doc for the exact steps to finish it.
di/                  Hilt modules. RepositoryModule.kt is THE ONE FILE that
                     picks Mock vs Supabase, per vertical (one @Binds each)
                     — everything else is unaffected by that choice.
                     DispatchersModule.kt provides qualified
                     CoroutineDispatchers for testability.
ui/theme/            Retro 8-bit design system: Color.kt, Type.kt (monospace
                     everywhere), Shape.kt (zero corner radius), Theme.kt.
ui/components/       RetroButton, RetroCard, PixelBadge, LegalNoticeCard (the
                     shared consent gate every Publish screen shows),
                     EstablishmentSuggestionRow (Pizza's chip suggestions)
                     — the only widgets screens should build UI out of.
ui/intro/            IntroScreen — the "Wishing Well" brand splash (hand-
                     drawn pixel-art well via Canvas, no image assets).
ui/hub/              HubScreen — vertical picker (Ride + Pizza live; Roomie
                     shown as a "coming soon" card, staged separately).
ui/publish/          Publish Intent screen + ViewModel (Ride vertical).
ui/match/            Match Results screen + ViewModel (Ride vertical).
ui/pizza/            Publish + Match Results screen/ViewModel pairs for the
                     Pizza vertical — same shape as ui/publish + ui/match.
MainActivity.kt       NavHost: intro -> hub -> {ride, pizza} (each its own
                     Publish/Matches tab switch). See its file doc for why
                     Navigation-Compose was adopted here.
MetaMatchApplication.kt  @HiltAndroidApp entry point.
```

`schema.sql` (repo root) — the production Supabase/PostGIS schema. Table and
column names map 1:1 to the domain models above (see comments in the file).
Not required to run the app today; becomes load-bearing once
`SupabaseRideShareRepository` is finished.

## The one rule that matters most

**Never import a `Mock*Repository` or `Supabase*Repository` outside of
`di/RepositoryModule.kt`.** Every use case, every ViewModel depends on its
vertical's repository interface only (`RideShareRepository`,
`PizzaShareRepository`). This is what makes the mock/production swap a
one-line change, per vertical. If you're adding a new use case or
ViewModel and find yourself importing a concrete repository class, stop —
inject the interface instead.

## Conventions

- Every class/interface has a WHAT/WHY/HOW doc comment at the top. Match
  that style in new code — this project's explicit purpose is to be
  learned from, not just to run.
- Domain layer (`domain/`) must never import `android.*` or
  `androidx.*` (except `javax.inject`, which is plain JDK/DI, not Android).
  This is what keeps use cases unit-testable on the plain JVM.
- Money: `Double` amounts, ISO currency code string (default `"MXN"`).
- Time: always `kotlinx.datetime.Instant`, never `java.util.Date` or
  Android's `Calendar` in the domain layer (`Calendar`/`DatePickerDialog`
  are fine in the `ui/` layer, where the platform date picker lives).
- IDs: `String` (UUID), generated client-side via `UUID.randomUUID()`.

## Not yet implemented (do not assume these exist)

- `SupabaseRideShareRepository` / `SupabasePizzaShareRepository` —
  skeletons only, throw `TODO()`.
- Any real network/auth calls, Supabase SDK dependency, or API keys.
- Module 3: visual meeting ticket, in-app chat, `SecurityAuditLog`.
- Module 4: `MonetizationEngine` interface, payment processing,
  enterprise subscription billing, ID-verification flow.
- Meta-Match Roomie — hub card exists and is marked "coming soon"; no
  domain model, repository, or screens exist yet. Its own planned/approved
  stage; see project memory `roomie_pizza_requirements.md` and
  `anatomía_de_meta-match_finder.md` §5.1 for the gathered requirements
  (sublease/lease minimum contract fields, objective vs. subjective
  matching, post-match verification flow, negotiable terms after a match).
- Contract-record → real written-document generation (PDF/e-signature).
  `ContractRecord` only captures the data; no export flow exists.
- Instrumented/UI tests (`androidTest/` is currently empty).

## Running / testing

- Open in Android Studio, sync Gradle, Run. No setup, no API keys.
- `./gradlew testDebugUnitTest` — fast JVM unit tests for `domain/`
  (`FindMatchesUseCaseTest`, `FindPizzaMatchesUseCaseTest`,
  `CheckAntiSpamUseCaseTest`, `GeoPointTest`, `UserIntegrityScoreTest`).
  These are the tests to run after touching any matching/anti-spam logic.
