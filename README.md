# MetaMatch

A general-purpose **meta-matching engine** for Android, built in Kotlin with
Jetpack Compose and Clean Architecture. The first feature built on top of
the engine is **ride-share carpooling with advance scheduling** — publish a
ride request today for a trip years from now (a flight landing in 2030 is a
first-class use case), get matched with nearby travelers by a
geospatial-centroid + pooled-budget algorithm, with built-in anti-spam
limits, community/security filters, and a reputation system.

> **Status: MVP, iteration 1 of a staged build.** The core engine and the
> ride-share matching module are implemented and run out of the box with
> zero backend setup. A production Supabase (PostgreSQL + PostGIS) backend
> is fully designed (`schema.sql`) but not wired in yet — see
> [Roadmap](#roadmap). This project doubles as a structured Kotlin-learning
> exercise: every class carries a WHAT/WHY/HOW doc comment explaining the
> code itself and the architectural decision behind it.

---

## Why this project exists

MetaMatch's premise is that "matching people for a shared arrangement" is a
general problem — ride shares today, study groups or equipment loans
tomorrow — and the hard parts (anti-spam limits, reputation, monetization
hooks, security filters) should be built once, in a core engine, rather
than re-invented per feature. This repo demonstrates that engine applied to
its first concrete use case.

## Screenshots

*(Run the app in Android Studio's Compose Preview or an emulator to capture
these — the retro 8-bit theme, the Publish Intent form with its live
anti-spam badge, and the Match Results list are the three views to
showcase.)*

## Architecture

Clean Architecture, three layers, dependencies always pointing inward:

```mermaid
flowchart TB
    subgraph UI["ui/ — Jetpack Compose + ViewModel"]
        PS[PublishIntentScreen]
        MS[MatchResultsScreen]
        PVM[PublishIntentViewModel]
        MVM[MatchResultsViewModel]
    end
    subgraph Domain["domain/ — pure Kotlin, zero Android deps"]
        UC1[PublishRideIntentUseCase]
        UC2[FindMatchesUseCase]
        UC3[CheckAntiSpamUseCase]
        Repo[("RideShareRepository\n(interface)")]
        Models[ContractIntent · RideShareIntent\nMatchResult · UserRule\nUserIntegrityScore · GeoPoint]
    end
    subgraph Data["data/ — RepositoryStrategy toggle"]
        Mock[MockRideShareRepository\n(in-memory, seeded, active today)]
        Supa[SupabaseRideShareRepository\n(skeleton, staged for next iteration)]
    end

    PS --> PVM --> UC1
    MS --> MVM --> UC2
    UC1 --> UC3
    UC1 & UC2 & UC3 --> Repo
    Repo -.Hilt @Binds.-> Mock
    Repo -.future flip.-> Supa
    UC1 & UC2 & UC3 -.operate on.-> Models
```

**The one seam that matters:** every ViewModel and use case depends on the
`RideShareRepository` *interface* only. `di/RepositoryModule.kt` is the
single file that decides which concrete implementation — the in-memory
`MockRideShareRepository` (today) or `SupabaseRideShareRepository`
(next iteration) — gets injected. Nothing else in the app changes when that
one binding flips.

| Layer | Package | Rule |
|---|---|---|
| Presentation | `ui/` | Compose screens + `@HiltViewModel`s. No business logic — only reads state / calls use cases. |
| Domain | `domain/` | Pure Kotlin. Models, use cases, the repository *interface*. No `android.*` imports — unit-testable on the plain JVM. |
| Data | `data/` | Repository *implementations*. Swappable via Hilt without touching the two layers above. |

## Tech stack

- **Kotlin** 2.2.10, **Jetpack Compose** (Compose BOM 2026.02.01)
- **Hilt** 2.59.2 (via KSP) for dependency injection
- **kotlinx-coroutines** + **kotlinx-datetime** for async work and
  timezone-correct scheduling
- **Clean Architecture + MVVM**
- Target backend (designed, not yet wired in): **Supabase** — PostgreSQL +
  **PostGIS** for spatial queries, Realtime, and Auth
- Min SDK 24 (Android 7.0) / Target SDK 37

## Getting started

```bash
git clone <this-repo>
```

Open the folder in Android Studio (Ladybug/Meerkat or newer), let Gradle
sync, hit **Run**. No API keys, no `.env` file, no backend — the app runs
entirely on seeded in-memory data via `MockRideShareRepository`.

Try the guided demo path: open **Publish**, accept every pre-filled default
value, tap **Publish**. Switch to **Matches** — you'll immediately see a
real match against a seeded demo rider (María, a UCaribe student sharing a
free ride to Cancún airport), computed by the same geospatial/budget rules
that would run against a live PostGIS database in production.

### Running tests

```bash
./gradlew testDebugUnitTest
```

Fast, plain-JVM tests covering the matching algorithm
(`FindMatchesUseCaseTest`), the anti-spam rule (`CheckAntiSpamUseCaseTest`),
and the geometry helpers (`GeoPointTest`). No emulator required — this is a
direct payoff of keeping the `domain/` layer free of Android dependencies.

## Core concepts

- **`ContractIntent`** — the abstract shape of "I want to be matched with
  someone for X." `RideShareIntent` is the first concrete implementation;
  the engine is designed for more (study groups, item loans) to follow
  without touching the matching/anti-spam/reputation logic.
- **Anti-spam / free tier** — 5 simultaneous free active listings per user
  (`PlatformPolicy.freeActiveIntentLimit`); a 6th listing requires a
  small micro-fee (`PlatformPolicy.microFeeAmountCents`). Both numbers are
  read from a config object designed to eventually live in a live-editable
  Supabase table — not hard-coded per the product brief's explicit
  requirement to tune limits "with agility after testing the app."
- **Geospatial + budget matching** — `FindMatchesUseCase` greedily clusters
  compatible ride intents whose destinations converge within each rider's
  own walking tolerance (a Kotlin mirror of PostGIS's `ST_Centroid` /
  `ST_DWithin`), then checks whether the group's pooled contribution meets
  a fare estimate.
- **Community & security filters** — per-intent email-domain allow-lists
  (e.g. `@ucaribe.edu.mx`) and personal blacklists, modeled as a reusable
  `UserRule` sealed class so an institution's admin can configure a
  community once instead of every member retyping it.
- **`UserIntegrityScore`** — a 0.0–5.0 reputation score derived from
  fulfilled vs. canceled-after-match contracts, defaulting new users to a
  neutral 4.0 rather than penalizing them for having no history yet.

## Database schema

`schema.sql` (repo root) is the full Supabase/PostGIS schema this app is
designed against: `ride_share_intents`, `user_integrity_scores`,
`user_rules`, `match_results`, `platform_policy`, `organizations`, GIST
spatial indexes, Row Level Security policies, and SQL functions
(`find_candidate_intents`, `compute_centroid`) that mirror
`FindMatchesUseCase.kt`'s logic server-side. Apply it via the Supabase SQL
Editor or CLI once `SupabaseRideShareRepository.kt` is wired in.

## Roadmap

This project is being built in stages on purpose, so each piece of Kotlin
can be reviewed and understood before the next lands:

1. ✅ **Module 1 — Core Meta-Match Engine**: `ContractIntent`,
   `UserIntegrityScore`, anti-spam rate limiting.
2. ✅ **Module 2 — Ride Share Contract & Advance Scheduling**: geospatial
   centroid + budget matching, community/security filters, Publish + Match
   Results screens.
3. ⏳ **Module 3 — Post-Match Protocol & Handshake**: visual meeting ticket
   ("Yellow Circle #37"), in-app chat, `SecurityAuditLog`.
4. ⏳ **Module 4 — Monetization & Verification Locks**: `MonetizationEngine`
   (per-transaction fee, enterprise B2B subscriptions), ID-verification gate
   for high-value contracts.
5. ⏳ **Backend cutover**: finish `SupabaseRideShareRepository.kt`, apply
   `schema.sql`, flip `di/RepositoryModule.kt` from Mock to Supabase.

See `CLAUDE.md` for a machine-readable map of exactly what exists today
versus what's staged next.

## License

MIT — see `LICENSE`. (Add one if this repo doesn't have one yet before
making it public.)
