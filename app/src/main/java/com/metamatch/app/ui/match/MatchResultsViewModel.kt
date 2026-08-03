package com.metamatch.app.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metamatch.app.data.mock.MockRideShareRepository
import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.MatchResult
import com.metamatch.app.domain.model.RideShareIntent
import com.metamatch.app.domain.repository.RideShareRepository
import com.metamatch.app.domain.usecase.FindMatchesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MatchResultsViewModel
 * ========================
 *
 * WHAT: backs the "Match Results / Active Offers" screen — runs
 * [FindMatchesUseCase] for the current user and prepares the result for
 * display, including resolving each match's bare participant IDs back
 * into full [RideShareIntent] details the UI can actually show (a
 * distance, a contribution amount, a domain).
 *
 * WHY resolution happens here and not in [FindMatchesUseCase]: the use
 * case's job is the *matching decision* — it deliberately returns
 * [MatchResult.participantIntentIds] rather than embedded intent objects,
 * keeping that model small and serializable (see [MatchResult]'s own
 * docs). Turning IDs back into full intents for display is a *screen*
 * concern, not a *domain* concern, so it belongs in this ViewModel, one
 * layer up.
 *
 * HOW "Accept" is handled in this MVP pass: tapping accept calls
 * [RideShareRepository.saveMatchResult], which marks every participating
 * intent [com.metamatch.app.domain.model.ContractStatus.MATCHED] — the
 * same repository call the real Module 3 hand-off (visual ticket + chat)
 * will build on top of once that module is implemented; this screen
 * simply confirms the match today and tells the user what's coming next.
 */
@HiltViewModel
class MatchResultsViewModel @Inject constructor(
    private val repository: RideShareRepository,
    private val findMatchesUseCase: FindMatchesUseCase,
) : ViewModel() {

    private val currentUserId = MockRideShareRepository.CURRENT_DEMO_USER_ID

    private val _uiState = MutableStateFlow(MatchResultsUiState())
    val uiState: StateFlow<MatchResultsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val matches = findMatchesUseCase(currentUserId)
                val enriched = matches.map { match -> enrich(match) }
                _uiState.update { it.copy(isLoading = false, matches = enriched) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = t.message ?: "Could not load matches.")
                }
            }
        }
    }

    fun onAcceptMatch(match: MatchDisplayModel) {
        viewModelScope.launch {
            repository.saveMatchResult(match.raw)
            val contractRecord = repository.formalizeContract(match.raw, match.participants)
            _uiState.update {
                it.copy(
                    confirmationMessage = "Match confirmed! Visual ticket + chat arrive in the Module 3 iteration.",
                    lastContractRecord = contractRecord,
                )
            }
            refresh()
        }
    }

    fun dismissConfirmation() {
        _uiState.update { it.copy(confirmationMessage = null, lastContractRecord = null) }
    }

    private suspend fun enrich(match: MatchResult): MatchDisplayModel {
        // The candidate pool plus the user's own intents cover every
        // possible participant ID a match could reference.
        val myIntents = repository.getActiveIntentsForUser(currentUserId)
        val candidateIntents = repository.getCandidateIntents(excludingUserId = currentUserId)
        val allKnownIntents = (myIntents + candidateIntents).associateBy { it.id }

        val participants = match.participantIntentIds.mapNotNull { allKnownIntents[it] }
        return MatchDisplayModel(raw = match, participants = participants)
    }
}

/**
 * MatchResultsUiState
 * ======================
 * Everything the Match Results screen needs, as one immutable snapshot —
 * same Unidirectional Data Flow pattern used by [com.metamatch.app.ui
 * .publish.PublishUiState]; see that class's documentation for why.
 */
data class MatchResultsUiState(
    val isLoading: Boolean = false,
    val matches: List<MatchDisplayModel> = emptyList(),
    val errorMessage: String? = null,
    val confirmationMessage: String? = null,
    val lastContractRecord: ContractRecord? = null,
)

/**
 * A [MatchResult] paired with the full [RideShareIntent] of every
 * participant, so the Compose screen can render human-meaningful details
 * (how many people, how much each contributed) instead of bare IDs.
 */
data class MatchDisplayModel(
    val raw: MatchResult,
    val participants: List<RideShareIntent>,
)
