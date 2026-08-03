package com.metamatch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metamatch.app.ui.theme.RetroBorder

/**
 * PixelBadge
 * ============
 *
 * WHAT: a small, high-contrast rectangular tag used for short, important
 * status text — most visibly the anti-spam counter on the Publish screen
 * ("3 / 5 FREE"), but reusable anywhere a compact status label is needed
 * (e.g. a trust-level tag next to a user's name).
 *
 * WHY this exists separately from [RetroCard]: a card is a *container*
 * meant to hold arbitrary child content; a badge is a single, short,
 * self-contained fact. Giving it its own composable keeps call sites
 * expressive (`PixelBadge("3 / 5 FREE", RetroGreen)`) instead of every
 * screen re-building a mini bordered box with `padding(6.dp)` by hand.
 *
 * HOW color communicates meaning here: [backgroundColor] is a required,
 * explicit parameter (no silent default) precisely because a badge's
 * whole job is to communicate status at a glance — the Publish screen
 * passes [com.metamatch.app.ui.theme.RetroGreen] while under the free
 * limit and [com.metamatch.app.ui.theme.RetroRed] once a fee will be
 * charged, so forgetting to pick a color is a compile error, not a
 * silently-wrong UI.
 */
@Composable
fun PixelBadge(
    text: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.background,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        modifier = modifier
            .border(width = 2.dp, color = RetroBorder, shape = RectangleShape)
            .background(color = backgroundColor, shape = RectangleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
