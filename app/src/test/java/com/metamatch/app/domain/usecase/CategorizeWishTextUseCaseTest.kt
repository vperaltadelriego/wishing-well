package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.WishCategory
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * CategorizeWishTextUseCaseTest
 * ================================
 * Verifies the keyword heuristic against one representative phrase per
 * [WishCategory], plus the ordering rule (deceased-loved-one before the
 * more generic "impossible" bucket) described in
 * [CategorizeWishTextUseCase]'s own class doc.
 */
class CategorizeWishTextUseCaseTest {

    private val categorize = CategorizeWishTextUseCase()

    @Test
    fun `detects a deceased loved one wish before the more generic impossible bucket`() {
        assertEquals(
            WishCategory.DECEASED_LOVED_ONE,
            categorize("Deseo volver a ver a mi abuelo que falleció."),
        )
    }

    @Test
    fun `detects a physically impossible wish`() {
        assertEquals(WishCategory.IMPOSSIBLE, categorize("Deseo tener superpoderes."))
    }

    @Test
    fun `detects peace`() {
        assertEquals(WishCategory.PEACE, categorize("Deseo la paz mundial."))
    }

    @Test
    fun `detects health over an overlapping family keyword`() {
        assertEquals(WishCategory.HEALTH, categorize("Deseo salud para toda mi familia."))
    }

    @Test
    fun `detects love`() {
        assertEquals(WishCategory.LOVE, categorize("Deseo encontrar el amor de mi vida."))
    }

    @Test
    fun `detects family`() {
        assertEquals(WishCategory.FAMILY, categorize("Deseo llevarme mejor con mis padres."))
    }

    @Test
    fun `detects money`() {
        assertEquals(WishCategory.MONEY, categorize("Deseo ganarme la lotería."))
    }

    @Test
    fun `detects career`() {
        assertEquals(WishCategory.CAREER, categorize("Deseo graduarme de la universidad este año."))
    }

    @Test
    fun `detects pets, matching the product brief's own example`() {
        assertEquals(
            WishCategory.PETS,
            categorize("Deseo que mi perro no hubiera sido atropellado."),
        )
    }

    @Test
    fun `falls back to other when nothing matches`() {
        assertEquals(WishCategory.OTHER, categorize("Deseo viajar por todo el mundo."))
    }
}
