package com.metamatch.app.data.mock

import com.metamatch.app.domain.model.ContractType
import com.metamatch.app.domain.model.UnstructuredWish
import com.metamatch.app.domain.model.WishCategory
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import kotlin.time.Duration.Companion.days

/**
 * WishSeedData
 * ==============
 *
 * WHAT: a large, invented pool of [UnstructuredWish]es —
 * [MockWishRepository]'s seed data — so anyone who clones this repo sees
 * genuinely varied statistics ("most common wish in México/Latin
 * America/the world/Cancún") on first run, not just their own single
 * demo wish.
 *
 * WHY this is *generated* from small template banks rather than ~180
 * individually hand-typed [UnstructuredWish] literals: the outcome
 * (a large, varied, invented dataset) is the same either way, but a
 * cross product of {category text templates} × {country/city pairs}
 * stays readable and easy to extend — adding a city or a category is a
 * one-line change here instead of dozens of near-duplicate literals
 * scattered through a huge list. [buildSeedWishes] is deterministic (no
 * real randomness), so the same seed data appears on every fresh install
 * — important for `ComputeWishStatisticsUseCaseTest` and for anyone
 * comparing what they see against this file's own documentation.
 *
 * HOW the "some wishes are secretly structured requests" flavor works:
 * [STRUCTURED_FLAVOR_WISHES] is a small, separately hand-written list
 * with an explicit [ContractType] hint each — see
 * [UnstructuredWish.structuredContractType]'s own docs for why this is
 * display-only, never joined against the real Ride/Pizza/Roomie data.
 */
object WishSeedData {

    fun buildSeedWishes(): List<UnstructuredWish> {
        val generated = LOCATIONS.flatMap { (country, cities) ->
            cities.flatMap { city ->
                CATEGORY_TEMPLATES.entries.flatMap { (category, texts) ->
                    texts.map { text -> Triple(category, text, country to city) }
                }
            }
        }

        return generated.mapIndexed { index, (category, text, location) ->
            val (country, city) = location
            UnstructuredWish(
                id = "seed-wish-$index",
                creatorUserId = "seed-wisher-${index % WISHER_POOL_SIZE}",
                text = text,
                category = category,
                country = country,
                city = city,
                createdAt = BASE_TIME.minus((index % 90).days),
            )
        } + STRUCTURED_FLAVOR_WISHES
    }

    /** How many distinct fake "people" the generated wishes are spread
     * across — purely cosmetic (no per-user logic reads this). */
    private const val WISHER_POOL_SIZE = 40

    private val BASE_TIME: Instant = Clock.System.now()

    /** Mexican cities get the heaviest representation (the pilot
     * community's home country), with a spread of other Latin American
     * and non-Latin-American locations so "World" vs. "Latin America"
     * vs. "México" vs. a single city each produce genuinely different
     * statistics. */
    private val LOCATIONS: List<Pair<String, List<String>>> = listOf(
        "México" to listOf(
            "Cancún", "Ciudad de México", "Guadalajara", "Monterrey",
            "Mérida", "Playa del Carmen", "Puebla", "Chetumal",
        ),
        "Colombia" to listOf("Bogotá", "Medellín"),
        "Argentina" to listOf("Buenos Aires", "Córdoba"),
        "Brasil" to listOf("São Paulo", "Río de Janeiro"),
        "Chile" to listOf("Santiago"),
        "Perú" to listOf("Lima"),
        "Guatemala" to listOf("Ciudad de Guatemala"),
        "Estados Unidos" to listOf("Miami", "Los Ángeles"),
        "España" to listOf("Madrid", "Barcelona"),
        "Japón" to listOf("Tokio"),
        "Francia" to listOf("París"),
        "India" to listOf("Bombay"),
        "Nigeria" to listOf("Lagos"),
    )

    /** A handful of phrasings per [WishCategory] — cross-multiplied
     * against every location in [LOCATIONS] by [buildSeedWishes]. */
    private val CATEGORY_TEMPLATES: Map<WishCategory, List<String>> = linkedMapOf(
        WishCategory.PEACE to listOf(
            "Deseo la paz mundial.",
            "Deseo que se acaben todas las guerras.",
            "I wish for world peace.",
            "Deseo un mundo sin conflictos.",
        ),
        WishCategory.HEALTH to listOf(
            "Deseo que mi mamá se recupere pronto.",
            "Deseo salud para toda mi familia.",
            "I wish for a cure for cancer.",
            "Deseo que se cure mi abuela.",
            "I wish I could be healthy again.",
        ),
        WishCategory.LOVE to listOf(
            "Deseo encontrar el amor de mi vida.",
            "Deseo que mi novio me pida matrimonio.",
            "I wish my crush noticed me.",
            "Deseo reconciliarme con mi esposa.",
            "I wish to find true love.",
        ),
        WishCategory.FAMILY to listOf(
            "Deseo reunir a toda mi familia otra vez.",
            "Deseo llevarme mejor con mis padres.",
            "I wish my family would stop fighting.",
            "Deseo que mis hermanos se reconcilien.",
        ),
        WishCategory.DECEASED_LOVED_ONE to listOf(
            "Deseo volver a ver a mi abuelo que falleció.",
            "Deseo que mi papá no hubiera fallecido tan joven.",
            "I wish I could talk to my mother who passed away one more time.",
            "Deseo abrazar a mi hermano que ya no está.",
            "Deseo que mi perro que falleció estuviera aquí conmigo.",
        ),
        WishCategory.MONEY to listOf(
            "Deseo ganarme la lotería.",
            "Deseo tener suficiente dinero para no preocuparme más.",
            "I wish I could pay off all my debt.",
            "Deseo ser rico de la noche a la mañana.",
        ),
        WishCategory.CAREER to listOf(
            "Deseo conseguir el trabajo de mis sueños.",
            "Deseo graduarme de la universidad este año.",
            "I wish for a promotion at work.",
            "Deseo que mi negocio finalmente despegue.",
        ),
        WishCategory.PETS to listOf(
            "Deseo que mi perro no hubiera sido atropellado.",
            "Deseo que mi gata viva muchos años más.",
            "I wish my dog could live forever.",
            "Deseo adoptar otra mascota pronto.",
        ),
        WishCategory.IMPOSSIBLE to listOf(
            "Deseo poder volar como un pájaro.",
            "Deseo tener superpoderes.",
            "I wish I could travel through time.",
            "Deseo que la magia fuera real.",
            "I wish I were immortal.",
        ),
        WishCategory.OTHER to listOf(
            "Deseo viajar por todo el mundo.",
            "Deseo aprender a tocar la guitarra.",
            "I wish I had more free time.",
            "Deseo mudarme a una ciudad junto al mar.",
        ),
    )

    /** Small, hand-written subset showing the "this wish was actually a
     * structured request" flavor — see this file's own class doc. */
    private val STRUCTURED_FLAVOR_WISHES: List<UnstructuredWish> = listOf(
        UnstructuredWish(
            id = "seed-wish-structured-ride-1",
            creatorUserId = "seed-wisher-structured-1",
            text = "Ojalá alguien comparta el viaje al aeropuerto conmigo mañana.",
            category = WishCategory.OTHER,
            structuredContractType = ContractType.RIDE_SHARE,
            country = "México",
            city = "Cancún",
            createdAt = BASE_TIME.minus(1.days),
        ),
        UnstructuredWish(
            id = "seed-wish-structured-pizza-1",
            creatorUserId = "seed-wisher-structured-2",
            text = "Ojalá alguien quiera compartir una pizza conmigo esta noche.",
            category = WishCategory.OTHER,
            structuredContractType = ContractType.PIZZA_SHARE,
            country = "México",
            city = "Ciudad de México",
            createdAt = BASE_TIME.minus(2.days),
        ),
        UnstructuredWish(
            id = "seed-wish-structured-roomie-1",
            creatorUserId = "seed-wisher-structured-3",
            text = "Ojalá encuentre un roomie de confianza pronto.",
            category = WishCategory.OTHER,
            structuredContractType = ContractType.ROOMMATE_SEARCH,
            country = "México",
            city = "Guadalajara",
            createdAt = BASE_TIME.minus(3.days),
        ),
        UnstructuredWish(
            id = "seed-wish-structured-ride-2",
            creatorUserId = "seed-wisher-structured-4",
            text = "I wish someone nearby was flying out at the same time as me.",
            category = WishCategory.OTHER,
            structuredContractType = ContractType.RIDE_SHARE,
            country = "Estados Unidos",
            city = "Miami",
            createdAt = BASE_TIME.minus(4.days),
        ),
        UnstructuredWish(
            id = "seed-wish-structured-pizza-2",
            creatorUserId = "seed-wisher-structured-5",
            text = "Ojalá alguien más quisiera la mitad de esta pizza familiar.",
            category = WishCategory.OTHER,
            structuredContractType = ContractType.PIZZA_SHARE,
            country = "Colombia",
            city = "Bogotá",
            createdAt = BASE_TIME.minus(5.days),
        ),
    )
}
