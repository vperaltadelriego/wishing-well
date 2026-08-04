package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.WishCategory
import javax.inject.Inject

/**
 * CategorizeWishTextUseCase
 * ============================
 *
 * WHAT: turns a free-text wish into one [WishCategory] so aggregate
 * statistics ("the most common wish in Mexico") are possible at all.
 *
 * WHY this is a plain keyword heuristic, not real text classification:
 * this app runs entirely on seeded, in-memory data with no backend (see
 * `data/mock/WishSeedData.kt`) — a real classifier or embeddings-based
 * similarity search would be the honest next step once a shared backend
 * exists, the same way [FindMatchesUseCase]'s greedy clustering is
 * explicitly *not* claimed to be an optimal matching algorithm. Good
 * enough to make the demo statistics feel real; not a claim that this is
 * production-grade natural language understanding.
 *
 * HOW category order matters: categories are checked most-specific-first
 * — [WishCategory.DECEASED_LOVED_ONE] before [WishCategory.IMPOSSIBLE],
 * since "quiero revivir a mi abuelo" would otherwise match the more
 * generic "revivir" keyword under [WishCategory.IMPOSSIBLE] and lose the
 * more specific, brief-requested statistic. The first matching category
 * wins; [WishCategory.OTHER] is the fallback when nothing matches.
 */
class CategorizeWishTextUseCase @Inject constructor() {

    operator fun invoke(text: String): WishCategory {
        val lower = text.lowercase()
        return CATEGORY_KEYWORDS.entries
            .firstOrNull { (_, keywords) -> keywords.any { keyword -> lower.contains(keyword) } }
            ?.key
            ?: WishCategory.OTHER
    }

    companion object {
        // Order matters — see class doc. A LinkedHashMap (what mapOf
        // preserves iteration order as here) is checked top to bottom.
        private val CATEGORY_KEYWORDS: Map<WishCategory, List<String>> = linkedMapOf(
            WishCategory.DECEASED_LOVED_ONE to listOf(
                "fallecido", "fallecida", "falleció", "murió", "muerto", "muerta",
                "difunto", "difunta", "en el cielo", "que no hubiera muerto",
                "que no hubiera fallecido", "deceased", "passed away", "who passed",
            ),
            WishCategory.IMPOSSIBLE to listOf(
                "volar como", "superpoderes", "viajar en el tiempo", "magia",
                "revivir", "inmortal", "teletransportarme", "unicornio", "dragón",
                "resucitar", "time travel", "superpower", "immortal", "teleport",
            ),
            WishCategory.PEACE to listOf(
                "paz mundial", "paz en el mundo", "se acaben las guerras",
                "world peace", "end all war", "end war",
            ),
            WishCategory.HEALTH to listOf(
                "salud", "que se cure", "cáncer", "cancer", "sane", "recupere",
                "healthy", "recover from", "cure for",
            ),
            WishCategory.PETS to listOf(
                "mi perro", "mi perra", "mi gato", "mi gata", "mi mascota",
                "my dog", "my cat", "my pet",
            ),
            WishCategory.LOVE to listOf(
                "el amor", "una pareja", "mi novio", "mi novia", "mi esposo",
                "mi esposa", "enamorarme", "casarme", "find love", "my boyfriend",
                "my girlfriend", "get married",
            ),
            WishCategory.FAMILY to listOf(
                "mi familia", "mis padres", "reconciliar", "my family", "reunite",
            ),
            WishCategory.MONEY to listOf(
                "dinero", "la lotería", "ser rico", "riqueza", "money", "lottery",
                "get rich",
            ),
            WishCategory.CAREER to listOf(
                "mi trabajo", "un empleo", "graduarme", "un ascenso", "my job",
                "my career", "graduate", "promotion",
            ),
        )
    }
}
