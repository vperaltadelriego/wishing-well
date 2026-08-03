package com.metamatch.app.ui.roomie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metamatch.app.data.mock.MockRideShareRepository
import com.metamatch.app.domain.model.ContractRecord
import com.metamatch.app.domain.model.RoommateIntent
import com.metamatch.app.domain.model.RoommateMatchResult
import com.metamatch.app.domain.repository.RoommateRepository
import com.metamatch.app.domain.usecase.FindRoommateMatchesUseCase
import com.metamatch.app.domain.usecase.UpdateRoommateIntentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * RoommateMatchResultsViewModel
 * ===============================
 *
 * WHAT: backs the Roomie "Match Results" screen — runs
 * [FindRoommateMatchesUseCase] for the current user and resolves each
 * match's two participant IDs back into their full [RoommateIntent]s.
 * The Roomie vertical's twin of
 * [com.metamatch.app.ui.match.MatchResultsViewModel]/
 * [com.metamatch.app.ui.pizza.PizzaMatchResultsViewModel] — with one
 * addition neither of those needs: [onAdjustPrice].
 *
 * WHY [onAdjustPrice] exists only here: see
 * [com.metamatch.app.domain.repository.RoommateRepository.updateIntent]'s
 * own docs — Roomie is the one vertical whose product requirement is
 * "let the user renegotiate terms after seeing a real candidate," not
 * just accept-as-is or cancel-and-republish.
 */
@HiltViewModel
class RoommateMatchResultsViewModel @Inject constructor(
    private val repository: RoommateRepository,
    private val findRoommateMatchesUseCase: FindRoommateMatchesUseCase,
    private val updateRoommateIntentUseCase: UpdateRoommateIntentUseCase,
) : ViewModel() {

    private val currentUserId = MockRideShareRepository.CURRENT_DEMO_USER_ID

    private val _uiState = MutableStateFlow(RoommateMatchResultsUiState())
    val uiState: StateFlow<RoommateMatchResultsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val matches = findRoommateMatchesUseCase(currentUserId)
                val enriched = matches.mapNotNull { match -> enrich(match) }
                _uiState.update { it.copy(isLoading = false, matches = enriched) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = t.message ?: "Could not load matches.")
                }
            }
        }
    }

    /** Edits *my own* side of [match] in place — e.g. a landlord lowering
     * their asking price after seeing this candidate — then recomputes
     * matches so the updated price gap is immediately visible. */
    fun onAdjustPrice(match: RoommateMatchDisplayModel, newAmount: Double) {
        viewModelScope.launch {
            val updated = match.myIntent.copy(
                financialTerms = match.myIntent.financialTerms.copy(amount = newAmount),
            )
            updateRoommateIntentUseCase(updated)
            refresh()
        }
    }

    fun onAcceptMatch(match: RoommateMatchDisplayModel) {
        viewModelScope.launch {
            repository.saveMatchResult(match.raw)
            val contractRecord = repository.formalizeContract(
                match.raw,
                listOf(match.myIntent, match.counterpartIntent),
            )
            _uiState.update {
                it.copy(
                    confirmationMessage = "Match confirmed! Coordinate next steps once chat arrives in Module 3.",
                    lastContractRecord = contractRecord,
                )
            }
            refresh()
        }
    }

    fun dismissConfirmation() {
        _uiState.update { it.copy(confirmationMessage = null, lastContractRecord = null) }
    }

    private suspend fun enrich(match: RoommateMatchResult): RoommateMatchDisplayModel? {
        val myIntents = repository.getActiveIntentsForUser(currentUserId)
        val candidateIntents = repository.getCandidateIntents(excludingUserId = currentUserId)
        val allKnownIntents = (myIntents + candidateIntents).associateBy { it.id }

        val participants = match.participantIntentIds.mapNotNull { allKnownIntents[it] }
        val mine = participants.find { it.creatorUserId == currentUserId } ?: return null
        val counterpart = participants.find { it.creatorUserId != currentUserId } ?: return null
        return RoommateMatchDisplayModel(raw = match, myIntent = mine, counterpartIntent = counterpart)
    }
}

data class RoommateMatchResultsUiState(
    val isLoading: Boolean = false,
    val matches: List<RoommateMatchDisplayModel> = emptyList(),
    val errorMessage: String? = null,
    val confirmationMessage: String? = null,
    val lastContractRecord: ContractRecord? = null,
)

/**
 * A [RoommateMatchResult] paired with the full [RoommateIntent] of each
 * side, split into "mine" and "theirs" so the UI can label an
 * "Adjust My Price" action against the right one.
 */
data class RoommateMatchDisplayModel(
    val raw: RoommateMatchResult,
    val myIntent: RoommateIntent,
    val counterpartIntent: RoommateIntent,
)
