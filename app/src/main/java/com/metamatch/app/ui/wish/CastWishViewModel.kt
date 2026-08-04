package com.metamatch.app.ui.wish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metamatch.app.data.mock.MockRideShareRepository
import com.metamatch.app.domain.model.WishCategory
import com.metamatch.app.domain.usecase.CastWishUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * CastWishViewModel
 * ====================
 *
 * WHAT: backs the "cast a wish" screen — a much smaller ViewModel than
 * the other three verticals' Publish screens, because
 * [com.metamatch.app.domain.model.UnstructuredWish] has no anti-spam
 * check, no legal-consent gate, and no micro-fee dialog to manage (see
 * [CastWishUseCase]'s own docs for why).
 *
 * WHY [CastWishUiState.castSignal] is a plain incrementing counter, not
 * a boolean: [WishGlobeCanvas] watches it to know *a new* wish landed
 * (vs. the same one re-triggering) — `LaunchedEffect(castSignal)` only
 * reacts when the value actually changes, and a counter change is
 * unambiguous where a boolean flip-back-to-false would need an extra
 * "consumed" step.
 */
@HiltViewModel
class CastWishViewModel @Inject constructor(
    private val castWishUseCase: CastWishUseCase,
) : ViewModel() {

    private val currentUserId = MockRideShareRepository.CURRENT_DEMO_USER_ID

    private val _uiState = MutableStateFlow(CastWishUiState())
    val uiState: StateFlow<CastWishUiState> = _uiState.asStateFlow()

    fun onTextChanged(value: String) {
        _uiState.update { it.copy(text = value, lastCastCategory = null) }
    }

    fun onCountryChanged(value: String) {
        _uiState.update { it.copy(country = value) }
    }

    fun onCityChanged(value: String) {
        _uiState.update { it.copy(city = value) }
    }

    fun onTossClicked() {
        val state = _uiState.value
        if (state.text.isBlank() || state.isCasting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCasting = true) }
            val wish = castWishUseCase(
                creatorUserId = currentUserId,
                text = state.text,
                country = state.country,
                city = state.city,
            )
            _uiState.update {
                it.copy(
                    isCasting = false,
                    text = "",
                    lastCastCategory = wish.category,
                    castSignal = it.castSignal + 1,
                )
            }
        }
    }
}

data class CastWishUiState(
    val text: String = "",
    val country: String = "México",
    val city: String = "Cancún",
    val isCasting: Boolean = false,
    val lastCastCategory: WishCategory? = null,
    val castSignal: Int = 0,
)
