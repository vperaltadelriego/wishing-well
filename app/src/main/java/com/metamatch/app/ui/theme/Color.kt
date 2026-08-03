package com.metamatch.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Retro 8-Bit Palette
 * =====================
 *
 * WHAT: every color MetaMatch's UI is allowed to use. Deliberately a
 * SMALL, high-contrast set — the visual language called for in the brief
 * is "retro 8-bit," which historically meant real hardware limits (early
 * consoles could only show a handful of colors on screen at once). We
 * keep that constraint on purpose even though modern phones have no such
 * limit: a small fixed palette is what makes an interface *read* as
 * retro, rather than merely "flat design with square corners."
 *
 * WHY these specific colors:
 * - [RetroBackground]/[RetroSurface] are near-black, evoking an old CRT
 *   monitor with everything else glowing on top of it.
 * - [RetroYellow], [RetroMagenta], and [RetroCyan] are saturated,
 *   maximum-contrast accents in the style of 8-bit arcade cabinets —
 *   used for primary actions, the anti-spam badge, and links/highlights
 *   respectively, so each accent color also carries a consistent meaning.
 * - [RetroGreen] and [RetroRed] map directly onto "good" and "bad"
 *   application states (a free listing vs. a fee required; a fulfilled
 *   match vs. a cancellation) — color as information, not just decoration.
 *
 * HOW this plugs into the rest of the design system: [Theme.kt] builds a
 * Material 3 `darkColorScheme` FROM these constants, so ordinary Material
 * components (`Button`, `Card`, `Text`) already pick up the retro palette
 * automatically, while the hand-built `ui/components/Retro*.kt` widgets
 * reference these constants directly for the parts Material 3 does not
 * control (borders, shadows).
 */
val RetroBackground = Color(0xFF0D0D12)
val RetroSurface = Color(0xFF17171F)
val RetroBorder = Color(0xFFF5F5F0)
val RetroTextPrimary = Color(0xFFF5F5F0)
val RetroTextMuted = Color(0xFF9A9AA5)

val RetroYellow = Color(0xFFF5C542)
val RetroMagenta = Color(0xFFE5399A)
val RetroCyan = Color(0xFF3FD6D6)
val RetroGreen = Color(0xFF4CD97B)
val RetroRed = Color(0xFFE5484D)

/** Fixed "on-color" text used on top of the bright accent colors above,
 * chosen per-color for contrast rather than always defaulting to black. */
val RetroOnYellow = Color(0xFF0D0D12)
val RetroOnMagenta = Color(0xFFF5F5F0)
val RetroOnCyan = Color(0xFF0D0D12)
val RetroOnGreen = Color(0xFF0D0D12)
val RetroOnRed = Color(0xFF0D0D12)
