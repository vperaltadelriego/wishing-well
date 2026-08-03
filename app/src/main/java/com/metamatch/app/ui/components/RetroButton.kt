package com.metamatch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metamatch.app.ui.theme.RetroBorder

/**
 * RetroButton
 * =============
 *
 * WHAT: a hand-built button that mimics the classic 8-bit "raised panel"
 * look — a solid color face, a thick black-and-white border, and a hard
 * drop shadow offset down-and-right that visually "lifts" while idle and
 * "presses flat" while the user's finger is down.
 *
 * WHY not just use Material 3's `Button` composable directly: Material's
 * default button has soft elevation shadows and rounded corners baked
 * into its internals, which fights the sharp/high-contrast retro
 * aesthetic no matter what shape/color arguments are passed in. Building
 * a small custom composable from `Box` + `Modifier.border` + a manual
 * shadow rectangle gives full control over exactly those two details.
 *
 * HOW the layered shadow works, step by step for anyone new to Compose:
 * a `Box` sizes itself to fit whichever children *don't* use
 * `Modifier.matchParentSize()`. So the visible, clickable "face" (with its
 * padding and text) is the child that decides how big the button is, and
 * the shadow rectangle behind it uses `Modifier.matchParentSize()` to
 * exactly copy that size — rather than needing its own width/height
 * calculation. Draw order follows child order, so the shadow is declared
 * first (bottom layer) and the face second (top layer).
 *
 * HOW the "pressed" effect works:
 * 1. [MutableInteractionSource] is a stream of "the user pressed / released
 *    this exact composable" events.
 * 2. [collectIsPressedAsState] turns that stream into a single `Boolean`
 *    Compose `State`, so `isPressed` automatically triggers a recomposition
 *    (Compose's term for "re-run this function to redraw with new values")
 *    the instant the finger goes down or up — no manual state management
 *    needed.
 * 3. While `isPressed` is true, the face is nudged down-and-right to meet
 *    the (fixed) shadow position, faking the button being pushed flush
 *    against the background — a cheap but very recognizable
 *    retro/skeuomorphic trick.
 *
 * @param label Upper-cased automatically to match retro arcade button
 *   text (e.g. "PUBLISH", "CANCEL").
 * @param backgroundColor Face color of the button; defaults to the theme's
 *   primary accent ([MaterialTheme.colorScheme.primary]).
 * @param contentColor Text color; defaults to the theme's matching
 *   `onPrimary` color for contrast.
 */
@Composable
fun RetroButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shadowDistance = 4.dp
    val faceAlpha = if (enabled) 1f else 0.4f

    Box(modifier = modifier) {
        // Bottom layer: a solid rectangle the same size as the face,
        // permanently offset down-and-right. This never moves — it is the
        // face that moves to meet it when pressed.
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowDistance, y = shadowDistance)
                .background(color = RetroBorder, shape = RectangleShape),
        )
        // Top layer: the clickable face. Determines the Box's overall size
        // via its own padding + content.
        Box(
            modifier = Modifier
                .offset(
                    x = if (isPressed) shadowDistance else 0.dp,
                    y = if (isPressed) shadowDistance else 0.dp,
                )
                .border(width = 2.dp, color = RetroBorder, shape = RectangleShape)
                .background(color = backgroundColor.copy(alpha = faceAlpha), shape = RectangleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}
