package com.metamatch.app.ui.wish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metamatch.app.ui.components.PixelBadge
import com.metamatch.app.ui.components.RetroButton
import com.metamatch.app.ui.components.RetroCard
import com.metamatch.app.ui.theme.RetroCyan
import com.metamatch.app.ui.theme.RetroOnCyan
import com.metamatch.app.ui.theme.RetroTextMuted
import com.metamatch.app.ui.theme.RetroYellow

/**
 * CastWishScreen
 * =================
 *
 * WHAT: "toss any wish into the well — structured or not." The rotating
 * globe ([WishGlobeCanvas]) sparks once ambiently every ~900ms and once
 * brighter each time this screen's own [CastWishViewModel] casts a wish.
 *
 * WHY the detected category is shown, not hidden: [WishCategory]
 * assignment is a simple keyword heuristic (see
 * [com.metamatch.app.domain.usecase.CategorizeWishTextUseCase]'s own
 * docs) — showing "DETECTED: LOVE" right after casting is honest about
 * *how* the statistics on the Stats tab get built, rather than
 * presenting categorization as something more sophisticated than it is.
 */
@Composable
fun CastWishScreen(
    modifier: Modifier = Modifier,
    viewModel: CastWishViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PixelBadge(
            text = "DEMO DATA — shared backend planned, not connected yet",
            backgroundColor = RetroCyan,
            contentColor = RetroOnCyan,
        )

        Text("MAKE A WISH", style = MaterialTheme.typography.headlineMedium)

        WishGlobeCanvas(
            castSignal = state.castSignal,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )

        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text("YOUR WISH", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.text,
                onValueChange = viewModel::onTextChanged,
                label = { Text("What do you wish for? Anything at all.") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                OutlinedTextField(
                    value = state.country,
                    onValueChange = viewModel::onCountryChanged,
                    label = { Text("Country") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.city,
                    onValueChange = viewModel::onCityChanged,
                    label = { Text("City") },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "Tu deseo se guarda de forma anónima para las estadísticas — " +
                    "no se comparte de forma identificable con otras personas.",
                style = MaterialTheme.typography.bodyMedium,
                color = RetroTextMuted,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        state.lastCastCategory?.let { category ->
            PixelBadge(text = "DETECTED: ${category.name}", backgroundColor = RetroYellow)
        }

        RetroButton(
            label = if (state.isCasting) "TOSSING..." else "TOSS IT IN",
            enabled = !state.isCasting && state.text.isNotBlank(),
            onClick = viewModel::onTossClicked,
            modifier = Modifier.fillMaxWidth(),
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp))
    }
}
