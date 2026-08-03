package com.metamatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * UserRule
 * =========
 *
 * WHAT: a first-class, storable representation of the "Community &
 * Security Filters" described in Module 2 — email-domain restrictions and
 * user blacklists — modeled as data on their own instead of only living as
 * loose fields on [RideShareIntent].
 *
 * WHY it exists as a *separate* model from the `allowedEmailDomains` /
 * `blockedUserIds` fields already on [RideShareIntent]:
 * those fields are the rule *as applied* to one specific intent at the
 * moment it was published. `UserRule` is the rule as a *reusable,
 * independently managed object* — e.g. a university's admin configures an
 * [EmailDomainAllowList] once ("only @ucaribe.edu.mx may join our
 * community"), and every ride a UCaribe student publishes afterward reuses
 * it, instead of every student re-typing the same domain by hand. This is
 * also the seed of the Module 4 "Enterprise B2B" story: a `UserRule`
 * scoped to an organization is exactly what an Xcaret/UCaribe subscription
 * would configure centrally.
 *
 * Kotlin note: this is a `sealed class`. Sealed classes describe a
 * *closed* set of possible subtypes — the compiler knows every case that
 * can exist (here: exactly three) and will error if a `when` block over a
 * `UserRule` forgets one. This is stronger than a plain `enum` because
 * each case can carry its own different data (a set of domains vs. a
 * single banned user ID), and stronger than an open class hierarchy
 * because nobody outside this file can sneak in a fourth, unhandled case.
 *
 * HOW it connects to the architecture
 * ------------------------------------
 * - Evaluated by [com.metamatch.app.domain.usecase.FindMatchesUseCase]
 *   before two intents are allowed to match.
 * - Persisted in Supabase as rows in a `user_rules` table (see
 *   `schema.sql`), so an admin can update a rule without shipping a new
 *   app version — directly in the spirit of "user limits and charges
 *   should be determined with agility after testing the app."
 */
sealed class UserRule {

    /**
     * Only users whose account email ends in one of [domains] may be
     * matched under this rule. Used both per-intent (a student manually
     * restricts their own post) and per-organization (an admin restricts
     * an entire subscribed community).
     */
    data class EmailDomainAllowList(
        val domains: Set<String>,
        val ownerOrganizationId: String? = null,
    ) : UserRule() {
        init {
            require(domains.isNotEmpty()) { "An allow-list rule needs at least one domain." }
        }
    }

    /**
     * A one-directional "never match me with this person" rule set by an
     * individual user — e.g. blocking an ex-partner or a previously
     * reported user. One-directional and per-user by design: user A
     * blocking user B does not require B's consent or knowledge.
     */
    data class PersonalBlacklist(
        val ownerUserId: String,
        val blockedUserIds: Set<String>,
    ) : UserRule()

    /**
     * A platform-level ban issued by MetaMatch itself (e.g. after a
     * `SecurityAuditLog` investigation in Module 3). Unlike
     * [PersonalBlacklist], this blocks the user from *all* matching,
     * platform-wide, not just against one other person.
     */
    data class PlatformBan(
        val bannedUserId: String,
        val reason: String,
        val issuedAt: Instant,
    ) : UserRule()
}
