package com.metamatch.app.ui.pizza

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
import com.metamatch.app.domain.model.PizzaShareIntent
import com.metamatch.app.ui.components.PixelBadge
import com.metamatch.app.ui.components.RetroButton
import com.metamatch.app.ui.components.RetroCard
import com.metamatch.app.ui.theme.RetroGreen
import com.metamatch.app.ui.theme.RetroRed
import com.metamatch.app.ui.theme.RetroTextMuted

/**
 * PizzaMatchResultsScreen
 * =========================
 *
 * WHAT: the Pizza vertical's "Match Results" screen — the twin of
 * [com.metamatch.app.ui.match.MatchResultsScreen]. Lists every compatible
 * order-splitting group [FindPizzaMatchesUseCase] found, and shows
 * whether the group has claimed the entire order.
 *
 * WHY units-claimed is shown as an explicit badge instead of hiding
 * partial matches: same reasoning as Ride's `meetsMinimumFare` badge — a
 * group that hasn't fully claimed the order yet is still a real,
 * actionable match (fewer slices unclaimed than ordering alone), not a
 * failure state to hide.
 */
@Composable
fun PizzaMatchResultsScreen(
    modifier: Modifier = Modifier,
    viewModel: PizzaMatchResultsViewModel = hiltViewModel(),
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
            state.isLoading -> Text("Looking for people splitting the same order...", style = MaterialTheme.typography.bodyLarge)

            state.errorMessage != null -> Text(
                text = state.errorMessage.orEmpty(),
                color = RetroRed,
                style = MaterialTheme.typography.bodyLarge,
            )

            state.matches.isEmpty() -> Text(
                text = "No matches yet. Publish a shared purchase, then come back here — " +
                    "Wishing Well recomputes this list from the live pool every time you refresh.",
                style = MaterialTheme.typography.bodyLarge,
            )

            else -> state.matches.forEach { match ->
                PizzaMatchCard(match = match, onAccept = { viewModel.onAcceptMatch(match) })
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
                    }
                }
            },
            confirmButton = { RetroButton(label = "Got it", onClick = viewModel::dismissConfirmation) },
        )
    }
}

@Composable
private fun PizzaMatchCard(match: PizzaMatchDisplayModel, onAccept: () -> Unit) {
    val result = match.raw
    RetroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${match.participants.size} SHARING", style = MaterialTheme.typography.titleLarge)
            PixelBadge(
                text = if (result.isFullyClaimed) {
                    "ORDER FULLY CLAIMED"
                } else {
                    "${result.unitsRemaining} UNITS LEFT"
                },
                backgroundColor = if (result.isFullyClaimed) RetroGreen else RetroRed,
            )
        }

        Text(
            text = "${result.establishment} — ${result.productDescription}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Pickup point: %.4f, %.4f".format(result.pickupPoint.latitude, result.pickupPoint.longitude),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Units claimed: ${result.totalUnitsClaimed} / ${result.totalUnitsAvailable}   -   " +
                "Pooled: %.2f %s".format(result.totalContribution, result.currency),
            style = MaterialTheme.typography.bodyMedium,
            color = RetroTextMuted,
        )

        Column(modifier = Modifier.padding(top = 8.dp)) {
            match.participants.forEach { participant -> ParticipantRow(participant) }
        }

        RetroButton(
            label = "Accept Match",
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}


@Composable
private fun ParticipantRow(intent: PizzaShareIntent) {
    Text(
        text = "• ${intent.creatorEmail}  —  wants ${intent.desiredUnits} units, contributes %.2f %s".format(
            intent.financialTerms.amount, intent.financialTerms.currency,
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
}
