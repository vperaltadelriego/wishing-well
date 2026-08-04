package com.metamatch.app.domain.usecase

import com.metamatch.app.domain.model.UnstructuredWish
import com.metamatch.app.domain.repository.WishRepository
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * CastWishUseCase
 * ==================
 *
 * WHAT: the entry point for "a user types a wish and taps Toss It In."
 * Categorizes the raw text via [CategorizeWishTextUseCase], builds the
 * [UnstructuredWish], and persists it.
 *
 * WHY there is no anti-spam check here, unlike every `Publish*IntentUseCase`
 * in the other three verticals: [UnstructuredWish] doesn't count against
 * the free-tier active-listing limit — it isn't a listing waiting to be
 * matched, it never expires or gets cancelled, so
 * [com.metamatch.app.domain.policy.PlatformPolicy.freeActiveIntentLimit]
 * simply doesn't apply to it.
 */
class CastWishUseCase @Inject constructor(
    private val repository: WishRepository,
    private val categorizeWishTextUseCase: CategorizeWishTextUseCase,
) {
    suspend operator fun invoke(
        creatorUserId: String,
        text: String,
        country: String,
        city: String,
    ): UnstructuredWish {
        val wish = UnstructuredWish(
            id = UUID.randomUUID().toString(),
            creatorUserId = creatorUserId,
            text = text,
            category = categorizeWishTextUseCase(text),
            country = country,
            city = city,
            createdAt = Clock.System.now(),
        )
        return repository.castWish(wish)
    }
}
