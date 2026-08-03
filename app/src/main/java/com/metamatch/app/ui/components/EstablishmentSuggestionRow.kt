package com.metamatch.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metamatch.app.ui.theme.RetroTextMuted

/**
 * EstablishmentSuggestionRow
 * ============================
 *
 * WHAT: two horizontally-scrollable rows of tappable suggestion chips for
 * the Pizza Publish screen's establishment field — one row of common
 * options, one of deliberately less-common ones, per the product brief's
 * explicit ask for both kinds of suggestions.
 *
 * WHY suggestions instead of a closed dropdown: the establishment field
 * itself stays free text (see [PublishPizzaIntentScreen]) — these chips
 * are a convenience for the common case, never a restriction. Typing an
 * establishment that isn't listed here works exactly the same as tapping
 * one that is.
 *
 * HOW "common" vs. "less usual" is communicated: purely through grouping
 * and a label above each row (`"Comunes"` / `"Menos usuales"`) — both
 * rows use the same [RetroButton] chip styling, since the distinction is
 * about *discoverability* for the user, not a signal the UI needs to
 * treat differently.
 */
@Composable
fun EstablishmentSuggestionRow(
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    commonSuggestions: List<String> = DEFAULT_COMMON_SUGGESTIONS,
    uncommonSuggestions: List<String> = DEFAULT_UNCOMMON_SUGGESTIONS,
) {
    Column(modifier = modifier) {
        Text(
            text = "Comunes",
            style = MaterialTheme.typography.labelSmall,
            color = RetroTextMuted,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        SuggestionChipRow(commonSuggestions, onSuggestionSelected)

        Text(
            text = "Menos usuales",
            style = MaterialTheme.typography.labelSmall,
            color = RetroTextMuted,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        SuggestionChipRow(uncommonSuggestions, onSuggestionSelected)
    }
}

@Composable
private fun SuggestionChipRow(suggestions: List<String>, onSuggestionSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.forEach { suggestion ->
            RetroButton(
                label = suggestion,
                onClick = { onSuggestionSelected(suggestion) },
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val DEFAULT_COMMON_SUGGESTIONS = listOf(
    "Domino's Pizza", "Pizza Hut", "Little Caesars", "Papa John's",
)

private val DEFAULT_UNCOMMON_SUGGESTIONS = listOf(
    "La Doña Pizzería", "Rústica Wood-Fired", "Trattoria Della Nonna",
)
