package com.metamatch.app.domain.model

/**
 * WishCategory
 * ==============
 *
 * WHAT: the closed set of buckets [UnstructuredWish]es get sorted into so
 * aggregate statistics ("the most common wish in Mexico") are possible at
 * all — a free-text wish has no structure to compare on its own, so
 * every wish is tagged with exactly one of these.
 *
 * WHY these specific categories: they map directly onto the concrete
 * statistics the product brief asked for — "el deseo más común," "cuánta
 * gente desea algo físicamente imposible" ([IMPOSSIBLE]), "cuántos
 * quisieran volver a ver a un familiar muerto" ([DECEASED_LOVED_ONE],
 * kept separate from [IMPOSSIBLE] even though bringing someone back is
 * *also* physically impossible, because the brief asks for it as its own
 * distinct statistic).
 *
 * HOW a wish gets a category: see
 * [com.metamatch.app.domain.usecase.CategorizeWishTextUseCase] — a
 * simple keyword heuristic, not real text classification. Good enough to
 * make the demo statistics feel real; a real backend iteration would
 * likely replace it with a proper classifier or embeddings-based
 * similarity search.
 */
enum class WishCategory {
    PEACE,
    HEALTH,
    LOVE,
    FAMILY,
    DECEASED_LOVED_ONE,
    MONEY,
    CAREER,
    PETS,
    IMPOSSIBLE,
    OTHER,
}
