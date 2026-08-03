package com.metamatch.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * MetaMatchTheme
 * ================
 *
 * WHAT: the single Composable every screen in the app wraps itself in.
 * Combines the [Color.kt] palette, [MetaMatchTypography], and
 * [MetaMatchShapes] into one Material 3 `ColorScheme` + `MaterialTheme`.
 *
 * WHY the theme is ALWAYS dark, ignoring the system's light/dark setting:
 * the retro 8-bit aesthetic this brief asks for is built around a
 * near-black background with glowing, saturated accent colors — the
 * visual grammar of an old CRT screen or arcade cabinet. A light variant
 * of that same palette would not read as "retro," it would just look like
 * a broken dark theme. `isSystemInDarkTheme()` is intentionally unused
 * here (see the parameter below) — a deliberate design decision, not an
 * oversight.
 *
 * HOW every screen should use this: wrap the top-level Composable of each
 * screen (or the whole app, once in `MainActivity`) in:
 * ```kotlin
 * MetaMatchTheme {
 *     // your screen content; MaterialTheme.colorScheme / .typography /
 *     // .shapes are now all retro-themed for everything inside this block.
 * }
 * ```
 */
@Composable
fun MetaMatchTheme(
    // Kept as a parameter (rather than hard-coded) so a future settings
    // screen could still offer a light/dark toggle without touching this
    // function's signature — it simply is not wired to anything yet,
    // because the retro identity is dark-only for now.
    useSystemDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val retroColorScheme = darkColorScheme(
        primary = RetroYellow,
        onPrimary = RetroOnYellow,
        secondary = RetroMagenta,
        onSecondary = RetroOnMagenta,
        tertiary = RetroCyan,
        onTertiary = RetroOnCyan,
        background = RetroBackground,
        onBackground = RetroTextPrimary,
        surface = RetroSurface,
        onSurface = RetroTextPrimary,
        surfaceVariant = RetroSurface,
        onSurfaceVariant = RetroTextMuted,
        error = RetroRed,
        onError = RetroOnRed,
        outline = RetroBorder,
    )

    MaterialTheme(
        colorScheme = retroColorScheme,
        typography = MetaMatchTypography,
        shapes = MetaMatchShapes,
        content = content,
    )
}
