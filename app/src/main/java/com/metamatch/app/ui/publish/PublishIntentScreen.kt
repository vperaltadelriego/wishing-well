package com.metamatch.app.ui.publish

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metamatch.app.ui.components.LegalNoticeCard
import com.metamatch.app.ui.components.PixelBadge
import com.metamatch.app.ui.components.RetroButton
import com.metamatch.app.ui.components.RetroCard
import com.metamatch.app.ui.theme.RetroGreen
import com.metamatch.app.ui.theme.RetroRed
import java.util.Calendar

/**
 * PublishIntentScreen
 * ======================
 *
 * WHAT: the "Publish Intent" screen from Module 2's UI spec — lets the
 * user configure a ride share request (departure, destination, schedule,
 * walking tolerance, budget, community filter) and shows the live
 * anti-spam "X / 5 FREE" indicator before publishing it.
 *
 * WHY this Composable has almost no logic of its own: every field read
 * and every button action here only reads [PublishUiState] or calls a
 * function on [PublishIntentViewModel] — it deliberately contains no
 * business rules (no fee math, no validation logic beyond "is this text
 * parseable"). That keeps the screen trivially previewable and testable:
 * this file only has to answer "does it show what the state says to
 * show?", never "is the anti-spam rule correct?" (that question belongs
 * to `CheckAntiSpamUseCase`'s own unit tests).
 *
 * HOW state flows in from the ViewModel: `hiltViewModel()` asks Hilt for
 * (or reuses) this screen's `PublishIntentViewModel`, and
 * `collectAsState()` turns its `StateFlow<PublishUiState>` into Compose
 * `State` — meaning this whole function automatically re-runs
 * ("recomposes") every time the ViewModel publishes a new state, with no
 * manual observer wiring required.
 */
@Composable
fun PublishIntentScreen(
    modifier: Modifier = Modifier,
    viewModel: PublishIntentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("PUBLISH A RIDE INTENT", style = MaterialTheme.typography.headlineMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Active listings", style = MaterialTheme.typography.bodyMedium)
            val overFreeLimit = state.activeIntentCount >= state.freeIntentLimit
            PixelBadge(
                text = "${state.activeIntentCount} / ${state.freeIntentLimit} FREE",
                backgroundColor = if (overFreeLimit) RetroRed else RetroGreen,
            )
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("DEPARTURE", style = MaterialTheme.typography.titleLarge)
            CoordinateRow(
                lat = state.departureLat,
                lng = state.departureLng,
                onLatChanged = { viewModel.onDepartureChanged(it, state.departureLng) },
                onLngChanged = { viewModel.onDepartureChanged(state.departureLat, it) },
            )
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("DESTINATION", style = MaterialTheme.typography.titleLarge)
            CoordinateRow(
                lat = state.destinationLat,
                lng = state.destinationLng,
                onLatChanged = { viewModel.onDestinationChanged(it, state.destinationLng) },
                onLngChanged = { viewModel.onDestinationChanged(state.destinationLat, it) },
            )
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("SCHEDULE", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RetroButton(
                    label = "%04d-%02d-%02d".format(state.year, state.month, state.day),
                    onClick = {
                        val calendar = Calendar.getInstance().apply {
                            set(state.year, state.month - 1, state.day)
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth -> viewModel.onDateSelected(year, month + 1, dayOfMonth) },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                )
                RetroButton(
                    label = "%02d:%02d".format(state.hour, state.minute),
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> viewModel.onTimeSelected(hour, minute) },
                            state.hour,
                            state.minute,
                            true,
                        ).show()
                    },
                )
            }
            Text(
                text = "Future dates are fully supported — publish a ride for a flight " +
                    "landing years from now, exactly as MetaMatch's advance-scheduling design intends.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("TOLERANCE & BUDGET", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.maxWalkingDistanceMeters,
                onValueChange = viewModel::onMaxWalkingDistanceChanged,
                label = { Text("Max walking distance (meters)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = state.budgetContribution,
                onValueChange = viewModel::onBudgetChanged,
                label = { Text("Your contribution, MXN (0 = free ride)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("COMMUNITY FILTER", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.allowedDomainsRaw,
                onValueChange = viewModel::onAllowedDomainsChanged,
                label = { Text("Allowed email domains (comma-separated, blank = open to all)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }

        LegalNoticeCard(
            acknowledged = state.legalConsentAcknowledged,
            onAcknowledgedChanged = viewModel::onLegalConsentChanged,
            modifier = Modifier.fillMaxWidth(),
        )

        state.errorMessage?.let { message ->
            Text(text = message, color = RetroRed, style = MaterialTheme.typography.bodyMedium)
        }
        state.successMessage?.let { message ->
            Text(text = message, color = RetroGreen, style = MaterialTheme.typography.bodyMedium)
        }

        RetroButton(
            label = if (state.isPublishing) "PUBLISHING..." else "PUBLISH",
            enabled = !state.isPublishing && state.legalConsentAcknowledged,
            onClick = viewModel::onPublishClicked,
            modifier = Modifier.fillMaxWidth(),
        )

        // Bottom spacer so the last card never sits flush against the
        // screen edge on tall content.
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp))
    }

    state.pendingMicroFeeCents?.let { feeCents ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMicroFeeDialog,
            title = { Text("Free limit reached") },
            text = {
                Text(
                    "You already have ${state.activeIntentCount} active listings " +
                        "(free limit: ${state.freeIntentLimit}). Publishing another one costs " +
                        "$feeCents cents. Continue?",
                )
            },
            confirmButton = {
                RetroButton(label = "Pay & Publish", onClick = viewModel::onMicroFeeConfirmed)
            },
            dismissButton = {
                RetroButton(
                    label = "Cancel",
                    onClick = viewModel::dismissMicroFeeDialog,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
    }
}

@Composable
private fun CoordinateRow(
    lat: String,
    lng: String,
    onLatChanged: (String) -> Unit,
    onLngChanged: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = lat,
            onValueChange = onLatChanged,
            label = { Text("Latitude") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = lng,
            onValueChange = onLngChanged,
            label = { Text("Longitude") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
    }
}
