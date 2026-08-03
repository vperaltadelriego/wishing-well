package com.metamatch.app.ui.pizza

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metamatch.app.ui.components.EstablishmentSuggestionRow
import com.metamatch.app.ui.components.LegalNoticeCard
import com.metamatch.app.ui.components.PixelBadge
import com.metamatch.app.ui.components.RetroButton
import com.metamatch.app.ui.components.RetroCard
import com.metamatch.app.ui.theme.RetroGreen
import com.metamatch.app.ui.theme.RetroRed

/**
 * PublishPizzaIntentScreen
 * ===========================
 *
 * WHAT: the "Publish a shared purchase" screen for Meta-Match Pizza —
 * the Pizza vertical's twin of
 * [com.metamatch.app.ui.publish.PublishIntentScreen]. Same shape (a
 * scrolling column of [RetroCard]s, a live anti-spam badge, a
 * [LegalNoticeCard] gate before the Publish button), adapted to Pizza's
 * own fields: what's being ordered, how much of it, and for how much.
 *
 * WHY there's no date/time picker (unlike Ride): see
 * [PublishPizzaIntentViewModel]'s own docs — Pizza demand is "now," so
 * this screen has no SCHEDULE card at all.
 */
@Composable
fun PublishPizzaIntentScreen(
    modifier: Modifier = Modifier,
    viewModel: PublishPizzaIntentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("PUBLISH A SHARED PURCHASE", style = MaterialTheme.typography.headlineMedium)

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
            Text("HOME LOCATION", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Where you'll pick up your share of the order.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            CoordinateRow(
                lat = state.homeLat,
                lng = state.homeLng,
                onLatChanged = { viewModel.onHomeLocationChanged(it, state.homeLng) },
                onLngChanged = { viewModel.onHomeLocationChanged(state.homeLat, it) },
            )
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("ORDER", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.establishment,
                onValueChange = viewModel::onEstablishmentChanged,
                label = { Text("Establishment") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            EstablishmentSuggestionRow(
                onSuggestionSelected = viewModel::onEstablishmentChanged,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedTextField(
                value = state.productDescription,
                onValueChange = viewModel::onProductDescriptionChanged,
                label = { Text("What are you ordering?") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("SPLIT & PRICE", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = state.totalUnits,
                    onValueChange = viewModel::onTotalUnitsChanged,
                    label = { Text("Total units in the order") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.totalPriceForWholeOrder,
                    onValueChange = viewModel::onTotalPriceChanged,
                    label = { Text("Price of the whole order, MXN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = state.desiredUnits,
                    onValueChange = viewModel::onDesiredUnitsChanged,
                    label = { Text("Units you want") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.pricePortion,
                    onValueChange = viewModel::onPricePortionChanged,
                    label = { Text("Your price, MXN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("PROXIMITY", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.maxDistanceMeters,
                onValueChange = viewModel::onMaxDistanceChanged,
                label = { Text("Max distance to pick up (meters)") },
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
            extraBulletPoints = listOf(
                "El pago del producto ocurre entre las partes al recogerlo; verifica el pedido antes de aceptar.",
            ),
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
