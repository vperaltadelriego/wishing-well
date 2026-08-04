package com.metamatch.app.ui.hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metamatch.app.ui.components.PixelBadge
import com.metamatch.app.ui.components.RetroCard
import com.metamatch.app.ui.theme.RetroTextMuted
import com.metamatch.app.ui.theme.RetroYellow

/**
 * HubScreen
 * ===========
 *
 * WHAT: the "which kind of match am I looking for?" screen, shown right
 * after the intro splash. Presents every matching vertical the Wishing
 * Well engine offers as its own plaque-styled [RetroCard] — **Ride**,
 * **Pizza**, **Roomie**, and **Wish** (a wish with no structure at all,
 * plus global statistics) are all live.
 *
 * WHY building the 3-card layout now, before Pizza/Roomie exist: adding a
 * new vertical later only means adding one route + one card here — no
 * further hub-layout work. Standing up the whole shape up front (rather
 * than a "Ride"-only hub that gets rebuilt twice) is exactly the kind of
 * scaffolding worth doing once real requirements (three verticals,
 * confirmed by the product brief) already exist — not speculative,
 * un-asked-for future-proofing.
 *
 * HOW navigation works: this is a mid-stack destination in the
 * [androidx.navigation.compose.NavHost] built in `MainActivity.kt`.
 * Tapping the Ride card calls [onOpenRide], which the host wires to the
 * existing Publish/Matches tab flow — that flow's own composables
 * ([com.metamatch.app.ui.publish.PublishIntentScreen],
 * [com.metamatch.app.ui.match.MatchResultsScreen]) are unchanged; only
 * what hosts them changed.
 */
@Composable
fun HubScreen(
    onOpenRide: () -> Unit,
    onOpenPizza: () -> Unit,
    onOpenRoomie: () -> Unit,
    onOpenWish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("MAKE A WISH", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Pick what you're trying to find a coincidence for.",
            style = MaterialTheme.typography.bodyMedium,
            color = RetroTextMuted,
        )

        VerticalPlaque(
            title = "RIDE",
            description = "Share a ride, split the fare, meet at a fair midpoint.",
            isAvailable = true,
            onClick = onOpenRide,
        )
        VerticalPlaque(
            title = "PIZZA",
            description = "Pool an order with people nearby, right now.",
            isAvailable = true,
            onClick = onOpenPizza,
        )
        VerticalPlaque(
            title = "ROOMIE",
            description = "Find someone to share a place with, for a stay of any length.",
            isAvailable = true,
            onClick = onOpenRoomie,
        )
        VerticalPlaque(
            title = "WISH",
            description = "Toss any wish into the well — structured or not — and see what the world is wishing for.",
            isAvailable = true,
            onClick = onOpenWish,
        )
    }
}

@Composable
private fun VerticalPlaque(
    title: String,
    description: String,
    isAvailable: Boolean,
    onClick: () -> Unit,
) {
    RetroCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isAvailable) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (!isAvailable) {
                PixelBadge(text = "COMING SOON", backgroundColor = RetroYellow)
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = RetroTextMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
