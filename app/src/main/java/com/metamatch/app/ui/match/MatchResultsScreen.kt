package com.metamatch.app.ui.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metamatch.app.domain.model.RideShareIntent
import com.metamatch.app.ui.components.PixelBadge
import com.metamatch.app.ui.components.RetroButton
import com.metamatch.app.ui.components.RetroCard
import com.metamatch.app.ui.theme.RetroCyan
import com.metamatch.app.ui.theme.RetroGreen
import com.metamatch.app.ui.theme.RetroOnCyan
import com.metamatch.app.ui.theme.RetroRed
import com.metamatch.app.ui.theme.RetroTextMuted
import kotlin.math.roundToInt

/**
 * MatchResultsScreen
 * =====================
 *
 * WHAT: the "Match Results / Active Offers" screen from Module 2's UI
 * spec — lists every candidate ride-share group
 * [com.metamatch.app.domain.usecase.FindMatchesUseCase] found for the
 * current user's active intent(s), and lets them accept one.
 *
 * WHY each entry shows [MatchResult.meetsMinimumFare] as an explicit
 * badge instead of hiding infeasible matches entirely: the brief's
 * requirement is to *verify* the pooled budget against the estimated
 * fare, not to silently pretend a shortfall doesn't exist. Showing "SHORT
 * $12 MXN" honestly is more useful to a real user deciding whether to bump
 * their own contribution than a match that quietly vanishes from the list.
 *
 * HOW this fits the "run standalone" requirement: because
 * [MatchResultsViewModel] talks only to the [com.metamatch.app.domain
 * .repository.RideShareRepository] interface, this entire screen already
 * works end-to-end against the seeded [com.metamatch.app.data.mock
 * .MockRideShareRepository] data with no backend at all — publish an
 * intent on the previous screen, then open this one to see it matched.
 */
@Composable
fun MatchResultsScreen(
    modifier: Modifier = Modifier,
    viewModel: MatchResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("MATCH RESULTS", style = MaterialTheme.typography.headlineMedium)
            RetroButton(label = "Refresh", onClick = viewModel::refresh)
        }

        when {
            state.isLoading -> Text("Searching for compatible rides...", style = MaterialTheme.typography.bodyLarge)

            state.errorMessage != null -> Text(
                text = state.errorMessage.orEmpty(),
                color = RetroRed,
                style = MaterialTheme.typography.bodyLarge,
            )

            state.matches.isEmpty() -> Text(
                text = "No matches yet. Publish an intent, then come back here — MetaMatch " +
                    "recomputes this list from the live intent pool every time you refresh.",
                style = MaterialTheme.typography.bodyLarge,
            )

            else -> state.matches.forEach { match ->
                MatchCard(match = match, onAccept = { viewModel.onAcceptMatch(match) })
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp))
    }

    state.confirmationMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirmation,
            title = { Text("Match confirmed") },
            text = {
                Column {
                    Text(message)
                    state.lastContractRecord?.let { record ->
                        PixelBadge(
                            text = "CONTRACT RECORD SAVED",
                            backgroundColor = RetroGreen,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                        )
                        Text(
                            text = "${record.participants.size} parties  •  " +
                                "%.2f %s pooled  •  every party's legal-consent timestamp on file.".format(
                                    record.totalContribution, record.currency,
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "This is the data that would formalize a written agreement, " +
                                "should one ever be needed — no document is generated in this MVP.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RetroTextMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            },
            confirmButton = { RetroButton(label = "Got it", onClick = viewModel::dismissConfirmation) },
        )
    }
}

@Composable
private fun MatchCard(match: MatchDisplayModel, onAccept: () -> Unit) {
    val result = match.raw
    RetroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${match.participants.size} RIDERS", style = MaterialTheme.typography.titleLarge)
            PixelBadge(
                text = if (result.meetsMinimumFare) "FARE COVERED" else "SHORT ${'$'}${(-result.budgetSurplus).roundToInt()}",
                backgroundColor = if (result.meetsMinimumFare) RetroGreen else RetroRed,
            )
        }

        Text(
            text = "Meeting point: %.4f, %.4f".format(result.meetingPoint.latitude, result.meetingPoint.longitude),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Pooled budget: %.2f %s   -   Estimated fare: %.2f %s".format(
                result.totalContribution, result.currency, result.estimatedFare, result.currency,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )

        Column(modifier = Modifier.padding(top = 8.dp)) {
            match.participants.forEach { participant -> ParticipantRow(participant) }
        }

        PixelBadge(
            text = "MEETING POINT SET",
            backgroundColor = RetroCyan,
            contentColor = RetroOnCyan,
            modifier = Modifier.padding(top = 8.dp),
        )

        RetroButton(
            label = "Accept Match",
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}

@Composable
private fun ParticipantRow(intent: RideShareIntent) {
    Text(
        text = "• ${intent.creatorEmail}  —  contributes %.2f %s".format(
            intent.financialTerms.amount, intent.financialTerms.currency,
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
}
