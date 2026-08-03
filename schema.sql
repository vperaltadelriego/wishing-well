-- =============================================================================
-- MetaMatch — Supabase / PostgreSQL + PostGIS schema
-- =============================================================================
-- WHAT this file is: the production database schema the app is DESIGNED for.
-- The app does not require this schema to run — MockRideShareRepository.kt
-- (Kotlin) mirrors this same data model entirely in memory so the project
-- builds and demos standalone in Android Studio. This file becomes load-
-- bearing the moment `SupabaseRideShareRepository.kt` is finished and wired
-- in via `di/RepositoryModule.kt` — see that file's own documentation.
--
-- HOW to apply this file: paste it into the Supabase SQL Editor for a new
-- project, or run it via the Supabase CLI (`supabase db push`). It is
-- idempotent-ish (uses IF NOT EXISTS / CREATE OR REPLACE throughout) so it
-- is safe to re-run while iterating.
--
-- WHY PostGIS specifically: Postgres alone has no native concept of "how
-- far apart are these two coordinates" or "what's the geometric center of
-- these five points" — PostGIS adds exactly those spatial data types and
-- functions (`geography`, `ST_DWithin`, `ST_Centroid`) directly inside the
-- database, so a query like "find every ride destination within 500 m of
-- this point" runs as one indexed SQL query instead of pulling every row
-- into the app and computing distances in Kotlin (which is what
-- FindMatchesUseCase.kt does today, purely because there is no database
-- yet in the standalone/demo configuration).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. EXTENSIONS
-- -----------------------------------------------------------------------------
-- PostGIS: adds the `geography`/`geometry` types and every ST_* spatial
-- function used below. Supabase projects can enable this with one click in
-- Database > Extensions, or via this statement.
create extension if not exists postgis;
-- uuid generation for primary keys, matching the client-generated
-- java.util.UUID.randomUUID() ids already used in RideShareIntent.id.
create extension if not exists "uuid-ossp";


-- -----------------------------------------------------------------------------
-- 1. ORGANIZATIONS  (Module 4 — Enterprise B2B groundwork)
-- -----------------------------------------------------------------------------
-- WHAT: a subscribing institution (UCaribe, Anáhuac, Xcaret...) whose
-- members get MetaMatch for free because the institution pays a flat
-- monthly fee instead of individual users paying per-listing fees.
-- WHY it exists already, even though Module 4 billing isn't implemented in
-- the app yet: `user_rules.owner_organization_id` (below) needs a table to
-- reference, and having the shape of this table nailed down now avoids a
-- breaking schema migration later.
create table if not exists organizations (
    id uuid primary key default uuid_generate_v4(),
    name text not null,
    -- The email domain this org's community filter is built around, e.g.
    -- 'ucaribe.edu.mx'. Mirrors RideShareIntent.allowedEmailDomains /
    -- UserRule.EmailDomainAllowList.domains on the Kotlin side.
    email_domain text not null unique,
    -- NULL = not yet on a paid plan (e.g. running the free public pilot).
    subscription_active_until timestamptz,
    created_at timestamptz not null default now()
);


-- -----------------------------------------------------------------------------
-- 2. PLATFORM POLICY  (Module 1/4 — the "agility after testing" table)
-- -----------------------------------------------------------------------------
-- WHAT: the live, editable business-rule configuration described in
-- PlatformPolicy.kt — free-tier listing limit, micro-fee amount, and the
-- ID-verification threshold. This table is designed to have EXACTLY ONE
-- ROW (enforced by the check constraint below), edited directly from the
-- Supabase dashboard's Table Editor.
-- WHY a table instead of an environment variable or a hard-coded constant:
-- per the product brief, "user limits and charges should be determined
-- with agility after testing the app" — an admin should be able to change
-- the free-tier limit from 5 to 3, or the micro-fee from $0.10 to $0.25,
-- without the Android app being rebuilt or resubmitted to the Play Store.
-- `SupabaseRideShareRepository.getPlatformPolicy()` will simply SELECT this
-- single row every time a use case needs current limits.
create table if not exists platform_policy (
    id int primary key default 1,
    free_active_intent_limit int not null default 5,
    micro_fee_amount_cents int not null default 10,
    fee_currency text not null default 'MXN',
    high_value_verification_threshold numeric not null default 5000.00,
    updated_at timestamptz not null default now(),
    constraint platform_policy_singleton check (id = 1)
);

insert into platform_policy (id) values (1)
    on conflict (id) do nothing;


-- -----------------------------------------------------------------------------
-- 3. USER INTEGRITY SCORES  (Module 1 — UserIntegrityScore.kt)
-- -----------------------------------------------------------------------------
-- WHAT: one row per user, tracking fulfilled vs. canceled-after-match
-- contracts. Mirrors UserIntegrityScore.kt's `completedMatches` /
-- `canceledMatches` fields exactly — the 0.0-5.0 `score` itself is NOT
-- stored (same reasoning as the Kotlin `get()` computed property: it must
-- never drift out of sync with the two counts, so it is always derived).
create table if not exists user_integrity_scores (
    user_id uuid primary key references auth.users (id) on delete cascade,
    completed_matches int not null default 0 check (completed_matches >= 0),
    canceled_matches int not null default 0 check (canceled_matches >= 0),
    updated_at timestamptz not null default now()
);

-- A convenience view that computes the 0.0-5.0 score in SQL, using the
-- exact same "default to a neutral 4.0 with no history" rule as
-- UserIntegrityScore.kt's `score` getter.
create or replace view user_integrity_scores_view as
select
    user_id,
    completed_matches,
    canceled_matches,
    case
        when (completed_matches + canceled_matches) = 0 then 4.0
        else round(
            least(greatest(
                (completed_matches::numeric / (completed_matches + canceled_matches)) * 5.0,
                0.0
            ), 5.0),
            2
        )
    end as score
from user_integrity_scores;

-- SECURITY DEFINER: runs with the PRIVILEGES OF THE FUNCTION'S OWNER
-- (an admin role), not the calling user's own privileges. This is the
-- mechanism that stops a malicious client from ever writing directly to
-- `user_integrity_scores` to inflate their own reputation — the table's
-- RLS policies (section 8) only allow SELECT for ordinary users; every
-- write MUST go through one of these two functions instead.
create or replace function increment_completed_matches(target_user uuid)
returns void
language plpgsql
security definer
as $$
begin
    insert into user_integrity_scores (user_id, completed_matches)
    values (target_user, 1)
    on conflict (user_id)
    do update set completed_matches = user_integrity_scores.completed_matches + 1,
                  updated_at = now();
end;
$$;

create or replace function increment_canceled_matches(target_user uuid)
returns void
language plpgsql
security definer
as $$
begin
    insert into user_integrity_scores (user_id, canceled_matches)
    values (target_user, 1)
    on conflict (user_id)
    do update set canceled_matches = user_integrity_scores.canceled_matches + 1,
                  updated_at = now();
end;
$$;

-- Auto-create a neutral integrity score row the moment a new user signs
-- up via Supabase Auth, so every other query can assume the row exists.
create or replace function handle_new_user_integrity_score()
returns trigger
language plpgsql
security definer
as $$
begin
    insert into user_integrity_scores (user_id) values (new.id)
    on conflict (user_id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created_integrity_score on auth.users;
create trigger on_auth_user_created_integrity_score
    after insert on auth.users
    for each row execute function handle_new_user_integrity_score();


-- -----------------------------------------------------------------------------
-- 4. USER RULES  (Module 2 — UserRule.kt: blacklists + domain allow-lists)
-- -----------------------------------------------------------------------------
-- WHAT: one row per rule. Mirrors the Kotlin `sealed class UserRule` with
-- three cases via a `rule_type` discriminator column — the closest plain
-- SQL equivalent of a Kotlin sealed class hierarchy (a single table with a
-- "which variant is this" column, sometimes called single-table
-- inheritance). Only the columns relevant to a given `rule_type` are
-- populated; the rest stay NULL.
create table if not exists user_rules (
    id uuid primary key default uuid_generate_v4(),
    rule_type text not null check (rule_type in ('EMAIL_DOMAIN_ALLOW_LIST', 'PERSONAL_BLACKLIST', 'PLATFORM_BAN')),

    -- EMAIL_DOMAIN_ALLOW_LIST fields (UserRule.EmailDomainAllowList):
    allowed_domains text[],
    owner_organization_id uuid references organizations (id) on delete cascade,

    -- PERSONAL_BLACKLIST fields (UserRule.PersonalBlacklist):
    owner_user_id uuid references auth.users (id) on delete cascade,
    blocked_user_id uuid references auth.users (id) on delete cascade,

    -- PLATFORM_BAN fields (UserRule.PlatformBan):
    banned_user_id uuid references auth.users (id) on delete cascade,
    ban_reason text,

    created_at timestamptz not null default now()
);

create index if not exists idx_user_rules_owner_user on user_rules (owner_user_id);
create index if not exists idx_user_rules_banned_user on user_rules (banned_user_id);


-- -----------------------------------------------------------------------------
-- 5. RIDE SHARE INTENTS  (Module 2 — RideShareIntent.kt / ContractIntent.kt)
-- -----------------------------------------------------------------------------
-- WHAT: one row per published ride request. Every column below has a
-- direct 1:1 counterpart on RideShareIntent.kt / ContractIntent.kt — see
-- the inline comments mapping each one.
--
-- WHY `departure`/`destination` are `geography(Point, 4326)` instead of two
-- plain `numeric` columns per point: `geography` is PostGIS's type for
-- "a point on the literal surface of the Earth," which is what makes
-- `ST_DWithin`/`ST_Centroid` (used in section 7) give real, accurate meter
-- distances. SRID 4326 is the standard "GPS coordinates" reference system
-- (WGS 84) — the same one Android's `Location` API and every consumer GPS
-- already use, so no conversion is needed on the client.
create table if not exists ride_share_intents (
    id uuid primary key default uuid_generate_v4(),
    creator_user_id uuid not null references auth.users (id) on delete cascade,
    creator_email text not null,                              -- RideShareIntent.creatorEmail

    -- ContractIntent fields:
    contract_type text not null default 'RIDE_SHARE',         -- ContractIntent.contractType
    created_at timestamptz not null default now(),            -- ContractIntent.createdAt
    scheduled_at timestamptz not null,                        -- ContractIntent.scheduledAt
    expires_at timestamptz,                                   -- ContractIntent.expiresAt
    verification_tier text not null default 'EMAIL_ONLY'      -- ContractIntent.verificationTier
        check (verification_tier in ('EMAIL_ONLY', 'PHONE_VERIFIED', 'ID_VERIFIED')),
    status text not null default 'ACTIVE'                     -- ContractIntent.status
        check (status in ('ACTIVE', 'MATCHED', 'FULFILLED', 'CANCELLED', 'EXPIRED')),

    -- FinancialTerms fields (embedded, not a separate table — a ride's
    -- money terms have no independent existence/lifecycle of their own):
    contribution_amount numeric not null default 0 check (contribution_amount >= 0),
    currency text not null default 'MXN',
    requires_escrow boolean not null default false,

    -- RideShareIntent-specific fields:
    departure geography(Point, 4326) not null,
    destination geography(Point, 4326) not null,
    max_walking_distance_meters numeric not null check (max_walking_distance_meters > 0),
    allowed_email_domains text[] not null default '{}',
    blocked_user_ids uuid[] not null default '{}'
);

-- GIST spatial indexes: without these, ST_DWithin/ST_Centroid queries below
-- would have to scan every single row and compute distance manually — the
-- exact "full table scan" problem PostGIS indexes exist to avoid. This is
-- the single most important index in this schema for the app to stay fast
-- as the number of published intents grows.
create index if not exists idx_ride_share_intents_departure on ride_share_intents using gist (departure);
create index if not exists idx_ride_share_intents_destination on ride_share_intents using gist (destination);
create index if not exists idx_ride_share_intents_status on ride_share_intents (status);
create index if not exists idx_ride_share_intents_creator on ride_share_intents (creator_user_id);
create index if not exists idx_ride_share_intents_scheduled_at on ride_share_intents (scheduled_at);


-- -----------------------------------------------------------------------------
-- 6. MATCH RESULTS  (MatchResult.kt)
-- -----------------------------------------------------------------------------
create table if not exists match_results (
    id uuid primary key default uuid_generate_v4(),
    meeting_point geography(Point, 4326) not null,
    total_contribution numeric not null,
    currency text not null default 'MXN',
    estimated_fare numeric not null,
    created_at timestamptz not null default now()
);

-- Many-to-many join: which ride_share_intents participate in which match.
-- Mirrors MatchResult.kt's `participantIntentIds: List<String>` — modeled
-- as a real join table (rather than an array column) so a foreign key can
-- guarantee every participant ID actually refers to a real intent.
create table if not exists match_participants (
    match_id uuid not null references match_results (id) on delete cascade,
    intent_id uuid not null references ride_share_intents (id) on delete cascade,
    primary key (match_id, intent_id)
);


-- -----------------------------------------------------------------------------
-- 7. SPATIAL SQL FUNCTIONS  (the PostGIS mirror of FindMatchesUseCase.kt)
-- -----------------------------------------------------------------------------

-- find_candidate_intents: the production version of
-- RideShareRepository.getCandidateIntents(). Returns every ACTIVE intent
-- other than the caller's own, scheduled within +/- 30 minutes of
-- `p_scheduled_at` (matching FindMatchesUseCase.SCHEDULE_TOLERANCE) and
-- within `p_max_distance_meters` of `p_destination` — pushing both the
-- schedule filter and the geospatial filter down into the database instead
-- of pulling every active row into the app.
create or replace function find_candidate_intents(
    p_exclude_user_id uuid,
    p_destination geography,
    p_max_distance_meters double precision,
    p_scheduled_at timestamptz,
    p_currency text
)
returns setof ride_share_intents
language sql
stable
as $$
    select *
    from ride_share_intents
    where status = 'ACTIVE'
      and creator_user_id <> p_exclude_user_id
      and currency = p_currency
      and scheduled_at between (p_scheduled_at - interval '30 minutes')
                            and (p_scheduled_at + interval '30 minutes')
      -- ST_DWithin on a `geography` column is measured in METERS directly
      -- (unlike `geometry`, which uses the coordinate system's own units) —
      -- this is the PostGIS equivalent of GeoPoint.distanceMetersTo(...)
      -- in the Kotlin mock, but evaluated by the database's spatial index
      -- instead of a Haversine formula running in application memory.
      and ST_DWithin(destination, p_destination, p_max_distance_meters);
$$;

-- compute_centroid: the PostGIS equivalent of GeoPoint.centroidOf(...).
-- ST_Collect gathers multiple points into one multi-point geometry;
-- ST_Centroid then finds their geometric mean, exactly mirroring the
-- Kotlin implementation's plain average-of-latitudes/average-of-longitudes
-- approach (both are intentionally simple flat-mean centroids, not a
-- geodesically exact one — accurate enough at the scale of a single city).
create or replace function compute_centroid(points geography[])
returns geography
language sql
immutable
as $$
    select ST_Centroid(ST_Collect(points::geometry[]))::geography;
$$;


-- -----------------------------------------------------------------------------
-- 8. ROW LEVEL SECURITY (RLS)
-- -----------------------------------------------------------------------------
-- WHAT: Postgres-enforced access rules that apply no matter how a request
-- reaches the database — directly through the Supabase client SDK, a
-- future admin dashboard, or anything else. This is the actual security
-- boundary; the Kotlin app's own checks (e.g. isEligibleUser()) are a
-- convenience for good UX, not a substitute for these policies.

alter table ride_share_intents enable row level security;
alter table user_integrity_scores enable row level security;
alter table user_rules enable row level security;
alter table platform_policy enable row level security;
alter table match_results enable row level security;
alter table match_participants enable row level security;

-- Everyone can read ACTIVE intents (needed for matching); only the
-- creator can read their own non-active (e.g. CANCELLED) intents.
create policy "ride_share_intents_select_active_or_own"
    on ride_share_intents for select
    using (status = 'ACTIVE' or creator_user_id = auth.uid());

-- Users may only ever publish intents as themselves — never forge another
-- user's creator_user_id.
create policy "ride_share_intents_insert_own"
    on ride_share_intents for insert
    with check (creator_user_id = auth.uid());

-- Users may only update/cancel their own intents.
create policy "ride_share_intents_update_own"
    on ride_share_intents for update
    using (creator_user_id = auth.uid());

-- Integrity scores are public to read (reputation should be visible to
-- everyone deciding whether to match with someone) but NEVER directly
-- writable by a client — only the SECURITY DEFINER functions in section 3
-- may change these rows.
create policy "user_integrity_scores_select_all"
    on user_integrity_scores for select
    using (true);

-- Users may manage their own personal blacklist rules; org-wide and
-- platform-ban rules are written by admin tooling only (no INSERT/UPDATE
-- policy for those rule_types is granted to ordinary authenticated users).
create policy "user_rules_select_own_or_public_allowlist"
    on user_rules for select
    using (owner_user_id = auth.uid() or rule_type = 'EMAIL_DOMAIN_ALLOW_LIST');

create policy "user_rules_insert_own_blacklist"
    on user_rules for insert
    with check (rule_type = 'PERSONAL_BLACKLIST' and owner_user_id = auth.uid());

-- The single platform_policy row is readable by any signed-in user (the
-- app needs it to render the "X / 5 FREE" indicator) but only writable via
-- the Supabase dashboard / service-role key, never from the mobile client.
create policy "platform_policy_select_authenticated"
    on platform_policy for select
    using (auth.role() = 'authenticated');

create policy "match_results_select_participant"
    on match_results for select
    using (
        exists (
            select 1 from match_participants mp
            join ride_share_intents ri on ri.id = mp.intent_id
            where mp.match_id = match_results.id
              and ri.creator_user_id = auth.uid()
        )
    );

create policy "match_participants_select_own"
    on match_participants for select
    using (
        exists (
            select 1 from ride_share_intents ri
            where ri.id = match_participants.intent_id
              and ri.creator_user_id = auth.uid()
        )
    );

-- =============================================================================
-- End of MVP schema. Deliberately NOT included in this pass (staged for the
-- Module 3 / Module 4 iteration, alongside SupabaseRideShareRepository.kt):
--   - security_audit_log (cryptographic hashes, device metadata, IPs for
--     the Module 3 "chain of custody" requirement)
--   - chat_messages (Module 3 in-app chat)
--   - subscriptions / payments (Module 4 monetization ledger)
-- Each will extend this same schema without altering anything above.
-- =============================================================================
