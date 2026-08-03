package com.metamatch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metamatch.app.ui.theme.RetroBorder

/**
 * RetroCard
 * ===========
 *
 * WHAT: the app's one general-purpose "panel" container — a sharp-cornered
 * rectangle with a thick border and the theme's surface color, used to
 * group related content (a single match candidate, a form section, a
 * summary block).
 *
 * WHY a single shared container instead of styling every screen's boxes
 * individually: consistency. Every bordered panel in the app should have
 * the exact same border weight and color so the retro aesthetic reads as
 * one coherent design system rather than several slightly-different
 * rectangles. Any screen that needs a bordered panel reaches for this
 * composable instead of re-declaring `Modifier.border(...)` inline.
 *
 * HOW: this is a *layout* composable — it takes a `content` lambda (typed
 * as `@Composable ColumnScope.() -> Unit`) the same way `Column` or `Row`
 * do, so callers can put arbitrary Compose content inside it:
 * ```kotlin
 * RetroCard {
 *     Text("Match #A21")
 *     Text("Meeting point: 350 m away")
 * }
 * ```
 */
@Composable
fun RetroCard(
    modifier: Modifier = Modifier,
    borderWidth: androidx.compose.ui.unit.Dp = 2.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .border(width = borderWidth, color = RetroBorder, shape = RectangleShape)
            .background(color = MaterialTheme.colorScheme.surface, shape = RectangleShape)
            .padding(16.dp),
        content = content,
    )
}
