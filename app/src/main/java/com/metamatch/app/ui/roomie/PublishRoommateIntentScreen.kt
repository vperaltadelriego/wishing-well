package com.metamatch.app.ui.roomie

import android.app.DatePickerDialog
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metamatch.app.domain.model.RoommateRole
import com.metamatch.app.ui.components.LegalNoticeCard
import com.metamatch.app.ui.components.PixelBadge
import com.metamatch.app.ui.components.RetroButton
import com.metamatch.app.ui.components.RetroCard
import com.metamatch.app.ui.theme.RetroGreen
import com.metamatch.app.ui.theme.RetroRed
import com.metamatch.app.ui.theme.RetroYellow
import java.util.Calendar

/**
 * PublishRoommateIntentScreen
 * =============================
 *
 * WHAT: the "Publish a Roomie listing" screen — the Roomie vertical's
 * twin of [com.metamatch.app.ui.publish.PublishIntentScreen]/
 * [com.metamatch.app.ui.pizza.PublishPizzaIntentScreen]. Same scrolling
 * [RetroCard] shape, adapted to Roomie's own fields: which side of the
 * match this is ([RoommateRole]), the move-in window, the legal-minimum
 * lease fields, and the explicitly-subjective/third-party fields the
 * matching algorithm never reads (see [PublishRoommateIntentViewModel]'s
 * own docs and `domain/model/RoommateIntent.kt`'s class doc).
 */
@Composable
fun PublishRoommateIntentScreen(
    modifier: Modifier = Modifier,
    viewModel: PublishRoommateIntentViewModel = hiltViewModel(),
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
        Text("PUBLISH A ROOMIE LISTING", style = MaterialTheme.typography.headlineMedium)

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
            Text("WHO ARE YOU?", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                RetroButton(
                    label = "Seeking a place",
                    onClick = { viewModel.onRoleChanged(RoommateRole.SEEKING) },
                    backgroundColor = if (state.role == RoommateRole.SEEKING) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (state.role == RoommateRole.SEEKING) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                RetroButton(
                    label = "Offering a place",
                    onClick = { viewModel.onRoleChanged(RoommateRole.OFFERING) },
                    backgroundColor = if (state.role == RoommateRole.OFFERING) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (state.role == RoommateRole.OFFERING) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("PLACE", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.zone,
                onValueChange = viewModel::onZoneChanged,
                label = { Text("Zone / neighborhood") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = state.propertyDescription,
                onValueChange = viewModel::onPropertyDescriptionChanged,
                label = { Text("Describe the place (or what you're looking for)") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("MOVE-IN WINDOW", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                RetroButton(
                    label = "From %04d-%02d-%02d".format(state.moveInStartYear, state.moveInStartMonth, state.moveInStartDay),
                    onClick = {
                        val calendar = Calendar.getInstance().apply {
                            set(state.moveInStartYear, state.moveInStartMonth - 1, state.moveInStartDay)
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, day -> viewModel.onMoveInStartSelected(year, month + 1, day) },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                )
                RetroButton(
                    label = "To %04d-%02d-%02d".format(state.moveInEndYear, state.moveInEndMonth, state.moveInEndDay),
                    onClick = {
                        val calendar = Calendar.getInstance().apply {
                            set(state.moveInEndYear, state.moveInEndMonth - 1, state.moveInEndDay)
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, day -> viewModel.onMoveInEndSelected(year, month + 1, day) },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                )
            }
            Text(
                text = "The earliest and latest dates you'd move in — matches are found on any overlap.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("LEASE TERMS", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = state.leaseDurationMonths,
                    onValueChange = viewModel::onLeaseDurationChanged,
                    label = { Text("Duration (months)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.priceAmount,
                    onValueChange = viewModel::onPriceChanged,
                    label = { Text(if (state.role == RoommateRole.OFFERING) "Asking price, MXN" else "Your budget, MXN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = state.depositAmount,
                onValueChange = viewModel::onDepositChanged,
                label = { Text("Security deposit, MXN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            OutlinedTextField(
                value = state.guarantorArrangement,
                onValueChange = viewModel::onGuarantorArrangementChanged,
                label = { Text("Guarantor / alternative arrangement") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("PREFERENCES", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Subjective — never used for matching, only shown to a candidate for their own judgment.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedTextField(
                value = state.preferenceNotes,
                onValueChange = viewModel::onPreferenceNotesChanged,
                label = { Text("e.g. \"Looking for someone quiet, non-smoker\"") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("THIRD-PARTY ARRANGEMENT", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.isThirdPartyArrangement,
                    onCheckedChange = viewModel::onThirdPartyArrangementChanged,
                    colors = CheckboxDefaults.colors(checkedColor = RetroYellow),
                )
                Text(
                    text = "I'm arranging this for someone else (e.g. a parent for a child).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.isThirdPartyArrangement) {
                OutlinedTextField(
                    value = state.occupantDescription,
                    onValueChange = viewModel::onOccupantDescriptionChanged,
                    label = { Text("Who will actually live there?") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
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
                "Esto no es un contrato de arrendamiento formal — verifica identidad, referencias y " +
                    "antecedentes antes de firmar cualquier acuerdo real.",
                "Considera formalizar cualquier acuerdo por escrito conforme a la legislación de tu estado.",
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
