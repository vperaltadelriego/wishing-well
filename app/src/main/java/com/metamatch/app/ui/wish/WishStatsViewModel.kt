package com.metamatch.app.ui.wish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metamatch.app.domain.model.UnstructuredWish
import com.metamatch.app.domain.model.WishScope
import com.metamatch.app.domain.model.WishStatistics
import com.metamatch.app.domain.repository.WishRepository
import com.metamatch.app.domain.usecase.ComputeWishStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * WishStatsViewModel
 * =====================
 *
 * WHAT: backs the "what has everyone been wishing for?" screen — the
 * payoff for tossing a wish in. Recomputes [WishStatistics] via
 * [ComputeWishStatisticsUseCase] whenever the selected [WishScopeOption]
 * changes, and keeps a small "recent wishes" feed live from
 * [WishRepository.observeAllWishes] for flavor.
 */
@HiltViewModel
class WishStatsViewModel @Inject constructor(
    private val computeWishStatisticsUseCase: ComputeWishStatisticsUseCase,
    private val wishRepository: WishRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WishStatsUiState())
    val uiState: StateFlow<WishStatsUiState> = _uiState.asStateFlow()

    init {
        onScopeSelected(WishScopeOption.WORLD)
        viewModelScope.launch {
            wishRepository.observeAllWishes().collect { all ->
                _uiState.update { it.copy(recentWishes = all.takeLast(5).reversed()) }
            }
        }
    }

    fun onScopeSelected(option: WishScopeOption) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedScope = option) }
            val statistics = computeWishStatisticsUseCase(option.toWishScope())
            _uiState.update { it.copy(isLoading = false, statistics = statistics) }
        }
    }
}

/** The four scope choices the brief explicitly asked for: "el deseo más
 * común de México, o el de Latinoamérica, o el más común en el mundo, o
 * en mi ciudad." [MY_CITY] defaults to Cancún, matching every other
 * demo default in this app (see `CLAUDE.md`'s pilot-community note). */
enum class WishScopeOption(val label: String) {
    WORLD("World"),
    LATIN_AMERICA("Latin America"),
    MEXICO("México"),
    MY_CITY("Cancún"),
}

private fun WishScopeOption.toWishScope(): WishScope = when (this) {
    WishScopeOption.WORLD -> WishScope.World
    WishScopeOption.LATIN_AMERICA -> WishScope.Region("Latin America")
    WishScopeOption.MEXICO -> WishScope.Country("México")
    WishScopeOption.MY_CITY -> WishScope.City("Cancún")
}

data class WishStatsUiState(
    val selectedScope: WishScopeOption = WishScopeOption.WORLD,
    val statistics: WishStatistics? = null,
    val recentWishes: List<UnstructuredWish> = emptyList(),
    val isLoading: Boolean = false,
)
