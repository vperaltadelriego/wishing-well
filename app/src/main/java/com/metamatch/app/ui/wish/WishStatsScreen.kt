package com.metamatch.app.ui.wish

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metamatch.app.domain.model.WishStatistics
import com.metamatch.app.ui.components.PixelBadge
import com.metamatch.app.ui.components.RetroButton
import com.metamatch.app.ui.components.RetroCard
import com.metamatch.app.ui.theme.RetroCyan
import com.metamatch.app.ui.theme.RetroGreen
import com.metamatch.app.ui.theme.RetroOnCyan
import com.metamatch.app.ui.theme.RetroTextMuted
import kotlin.math.roundToInt

/**
 * WishStatsScreen
 * ==================
 *
 * WHAT: "what has everyone been wishing for?" — the statistics screen
 * the product brief specifically asked for: most common wish per scope,
 * % physically impossible, % wishing to see a deceased loved one again.
 * All numbers come from [com.metamatch.app.data.mock.WishSeedData] plus
 * whatever this device has cast this session — see the "DEMO DATA"
 * banner and `SupabaseWishRepository.kt`'s own docs for why these
 * aren't real global numbers yet.
 */
@Composable
fun WishStatsScreen(
    modifier: Modifier = Modifier,
    viewModel: WishStatsViewModel = hiltViewModel(),
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

        Text("WHAT THE WORLD WISHES FOR", style = MaterialTheme.typography.headlineMedium)

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WishScopeOption.entries.forEach { option ->
                RetroButton(
                    label = option.label,
                    onClick = { viewModel.onScopeSelected(option) },
                    backgroundColor = if (state.selectedScope == option) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (state.selectedScope == option) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }

        when {
            state.isLoading -> Text("Counting wishes...", style = MaterialTheme.typography.bodyLarge)
            state.statistics != null -> StatisticsCards(state.statistics!!)
        }

        if (state.recentWishes.isNotEmpty()) {
            RetroCard(modifier = Modifier.fillMaxWidth()) {
                Text("RECENT WISHES", style = MaterialTheme.typography.titleLarge)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    state.recentWishes.forEach { wish ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "\"${wish.text}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            PixelBadge(
                                text = wish.category.name,
                                backgroundColor = RetroGreen,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun StatisticsCards(statistics: WishStatistics) {
    RetroCard(modifier = Modifier.fillMaxWidth()) {
        Text(statistics.scopeLabel.uppercase(), style = MaterialTheme.typography.titleLarge)
        Text(
            text = "${statistics.totalWishes} wishes cast",
            style = MaterialTheme.typography.bodyMedium,
            color = RetroTextMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (statistics.mostCommonCategory != null) {
            Text(
                text = "Most common: ${statistics.mostCommonCategory.name} " +
                    "(${statistics.mostCommonCategoryCount})",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            text = "%d%% physically impossible".format(statistics.impossibleWishPercent.roundToInt()),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "%d%% wish to see a deceased loved one again".format(
                statistics.deceasedLovedOnePercent.roundToInt(),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "%d%% were actually looking for something specific".format(
                statistics.structuredWishPercent.roundToInt(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = RetroTextMuted,
        )
    }
}
