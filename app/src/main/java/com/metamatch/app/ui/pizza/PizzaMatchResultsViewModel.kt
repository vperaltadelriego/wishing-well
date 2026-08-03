package com.metamatch.app.ui.pizza

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metamatch.app.data.mock.MockRideShareRepository
import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.PizzaMatchResult
import com.metamatch.app.domain.model.PizzaShareIntent
import com.metamatch.app.domain.repository.PizzaShareRepository
import com.metamatch.app.domain.usecase.FindPizzaMatchesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PizzaMatchResultsViewModel
 * ============================
 *
 * WHAT: backs the Pizza "Match Results" screen — runs
 * [FindPizzaMatchesUseCase] for the current user and resolves each
 * match's bare participant IDs back into full [PizzaShareIntent]s the UI
 * can show. The Pizza vertical's twin of
 * [com.metamatch.app.ui.match.MatchResultsViewModel] — same reasoning
 * throughout, see that class's own docs.
 */
@HiltViewModel
class PizzaMatchResultsViewModel @Inject constructor(
    private val repository: PizzaShareRepository,
    private val findPizzaMatchesUseCase: FindPizzaMatchesUseCase,
) : ViewModel() {

    private val currentUserId = MockRideShareRepository.CURRENT_DEMO_USER_ID

    private val _uiState = MutableStateFlow(PizzaMatchResultsUiState())
    val uiState: StateFlow<PizzaMatchResultsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val matches = findPizzaMatchesUseCase(currentUserId)
                val enriched = matches.map { match -> enrich(match) }
                _uiState.update { it.copy(isLoading = false, matches = enriched) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = t.message ?: "Could not load matches.")
                }
            }
        }
    }

    fun onAcceptMatch(match: PizzaMatchDisplayModel) {
        viewModelScope.launch {
            repository.saveMatchResult(match.raw)
            val contractRecord = repository.formalizeContract(match.raw, match.participants)
            _uiState.update {
                it.copy(
                    confirmationMessage = "Match confirmed! Coordinate pickup in the chat once it arrives.",
                    lastContractRecord = contractRecord,
                )
            }
            refresh()
        }
    }

    fun dismissConfirmation() {
        _uiState.update { it.copy(confirmationMessage = null, lastContractRecord = null) }
    }

    private suspend fun enrich(match: PizzaMatchResult): PizzaMatchDisplayModel {
        val myIntents = repository.getActiveIntentsForUser(currentUserId)
        val candidateIntents = repository.getCandidateIntents(excludingUserId = currentUserId)
        val allKnownIntents = (myIntents + candidateIntents).associateBy { it.id }

        val participants = match.participantIntentIds.mapNotNull { allKnownIntents[it] }
        return PizzaMatchDisplayModel(raw = match, participants = participants)
    }
}

data class PizzaMatchResultsUiState(
    val isLoading: Boolean = false,
    val matches: List<PizzaMatchDisplayModel> = emptyList(),
    val errorMessage: String? = null,
    val confirmationMessage: String? = null,
    val lastContractRecord: ContractRecord? = null,
)

/**
 * A [PizzaMatchResult] paired with the full [PizzaShareIntent] of every
 * participant, so the Compose screen can render human-meaningful details
 * instead of bare IDs.
 */
data class PizzaMatchDisplayModel(
    val raw: PizzaMatchResult,
    val participants: List<PizzaShareIntent>,
)
