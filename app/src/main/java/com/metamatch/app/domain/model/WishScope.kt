package com.metamatch.app.domain.model

/**
 * WishScope
 * ===========
 *
 * WHAT: which slice of all [UnstructuredWish]es a statistics query
 * should look at — the product brief asked for "el deseo más común de
 * México, o el de Latinoamérica, o el más común en el mundo, o en mi
 * ciudad," which are exactly these four cases.
 *
 * WHY a `sealed class` instead of an `enum`: [Region], [Country], and
 * [City] all need to carry a name (`"Latin America"`, `"México"`,
 * `"Cancún"`), which a plain enum constant can't hold. A `sealed class`
 * gives the same "closed set the compiler can exhaustively check" benefit
 * enums have, while still letting each case carry its own data.
 *
 * HOW [Region] answers "Latin America": [LATIN_AMERICAN_COUNTRIES] is a
 * small, hard-coded list of country names used to filter
 * [UnstructuredWish.country] — not a geography API, just enough for the
 * demo dataset in `data/mock/WishSeedData.kt` to produce a genuinely
 * different result for "Latin America" than for "the world."
 */
sealed class WishScope {
    /** Every wish, everywhere. */
    data object World : WishScope()

    /** Every wish whose [UnstructuredWish.country] is in [name]'s region. */
    data class Region(val name: String) : WishScope()

    data class Country(val name: String) : WishScope()

    data class City(val name: String) : WishScope()
}

/** Country names [WishScope.Region] ("Latin America") matches against. */
val LATIN_AMERICAN_COUNTRIES = setOf(
    "México", "Guatemala", "Honduras", "El Salvador", "Nicaragua", "Costa Rica",
    "Panamá", "Colombia", "Venezuela", "Ecuador", "Perú", "Bolivia", "Chile",
    "Argentina", "Uruguay", "Paraguay", "Brasil", "Cuba", "República Dominicana",
)
