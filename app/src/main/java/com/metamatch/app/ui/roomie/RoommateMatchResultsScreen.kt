package com.metamatch.app.ui.roomie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metamatch.app.ui.components.PixelBadge
import com.metamatch.app.ui.components.RetroButton
import com.metamatch.app.ui.components.RetroCard
import com.metamatch.app.ui.theme.RetroGreen
import com.metamatch.app.ui.theme.RetroRed
import com.metamatch.app.ui.theme.RetroTextMuted
import com.metamatch.app.ui.theme.RetroYellow
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * RoommateMatchResultsScreen
 * ============================
 *
 * WHAT: the Roomie vertical's "Match Results" screen — the twin of
 * [com.metamatch.app.ui.match.MatchResultsScreen]/
 * [com.metamatch.app.ui.pizza.PizzaMatchResultsScreen]. Lists every
 * compatible seeker/offerer pair [FindRoommateMatchesUseCase] found, with
 * the price gap shown honestly rather than filtered out (see that use
 * case's own docs), and an "Adjust My Price" action neither of the other
 * two verticals has.
 */
@Composable
fun RoommateMatchResultsScreen(
    modifier: Modifier = Modifier,
    viewModel: RoommateMatchResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var adjustingMatch by remember { mutableStateOf<RoommateMatchDisplayModel?>(null) }
    var adjustedPriceText by remember { mutableStateOf("") }

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
            state.isLoading -> Text("Looking for compatible listings...", style = MaterialTheme.typography.bodyLarge)

            state.errorMessage != null -> Text(
                text = state.errorMessage.orEmpty(),
                color = RetroRed,
                style = MaterialTheme.typography.bodyLarge,
            )

            state.matches.isEmpty() -> Text(
                text = "No matches yet. Publish a listing, then come back here — " +
                    "Wishing Well recomputes this list from the live pool every time you refresh.",
                style = MaterialTheme.typography.bodyLarge,
            )

            else -> state.matches.forEach { match ->
                RoommateMatchCard(
                    match = match,
                    onAccept = { viewModel.onAcceptMatch(match) },
                    onAdjustPriceClick = {
                        adjustingMatch = match
                        adjustedPriceText = match.myIntent.financialTerms.amount.roundToInt().toString()
                    },
                )
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp))
    }

    adjustingMatch?.let { match ->
        AlertDialog(
            onDismissRequest = { adjustingMatch = null },
            title = { Text("Adjust my price") },
            text = {
                OutlinedTextField(
                    value = adjustedPriceText,
                    onValueChange = { adjustedPriceText = it },
                    label = { Text("New amount, MXN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = {
                RetroButton(
                    label = "Save",
                    onClick = {
                        adjustedPriceText.toDoubleOrNull()?.let { newAmount ->
                            viewModel.onAdjustPrice(match, newAmount)
                        }
                        adjustingMatch = null
                    },
                )
            },
            dismissButton = {
                RetroButton(
                    label = "Cancel",
                    onClick = { adjustingMatch = null },
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
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
                                "%.2f %s  •  every party's legal-consent timestamp on file.".format(
                                    record.totalContribution, record.currency,
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Antes de firmar: agenden una entrevista, intercambien referencias, " +
                                "y verifiquen lo que necesiten — esto llega al chat en la iteración del Módulo 3.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RetroTextMuted,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = { RetroButton(label = "Got it", onClick = viewModel::dismissConfirmation) },
        )
    }
}

@Composable
private fun RoommateMatchCard(
    match: RoommateMatchDisplayModel,
    onAccept: () -> Unit,
    onAdjustPriceClick: () -> Unit,
) {
    val result = match.raw
    RetroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(result.zone, style = MaterialTheme.typography.titleLarge)
            PixelBadge(
                text = if (result.isPriceAligned) {
                    "PRICE ALIGNED"
                } else {
                    "GAP: ${abs(result.priceGapPercent).roundToInt()}%"
                },
                backgroundColor = if (result.isPriceAligned) RetroGreen else RetroYellow,
            )
        }

        Text(
            text = "Asking %.2f %s  —  Budget %.2f %s".format(
                result.askingPrice, result.currency, result.seekerBudget, result.currency,
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Lease: ${match.counterpartIntent.leaseDurationMonths} months  •  " +
                "Deposit %.2f %s  •  Guarantor: %s".format(
                    match.counterpartIntent.depositAmount, result.currency, match.counterpartIntent.guarantorArrangement,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = RetroTextMuted,
        )
        if (match.counterpartIntent.preferenceNotes.isNotBlank()) {
            Text(
                text = "\"${match.counterpartIntent.preferenceNotes}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = RetroTextMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            text = "• ${match.counterpartIntent.creatorEmail}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            RetroButton(
                label = "Adjust My Price",
                onClick = onAdjustPriceClick,
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            RetroButton(
                label = "Accept Match",
                onClick = onAccept,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
