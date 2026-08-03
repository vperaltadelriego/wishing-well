package com.metamatch.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Retro Typography
 * ==================
 *
 * WHAT: a Material 3 [Typography] built entirely on
 * [FontFamily.Monospace] — every character the exact same width, the way
 * text renders on an old terminal or an 8-bit game's dialogue box.
 *
 * WHY [FontFamily.Monospace] specifically, instead of bundling a custom
 * pixel-art `.ttf` file: monospace is a *built-in* Android font family
 * available on every device back to API 1, so the retro look works
 * immediately with zero extra assets, zero licensing questions, and zero
 * risk of a missing-font-resource build error — an important
 * consideration for a project whose explicit goal is to compile cleanly
 * the moment it is cloned. Swapping in a dedicated pixel font later (e.g.
 * "Press Start 2P") is a drop-in change to the single `FontFamily.Monospace`
 * reference below, once such a font file is added under `res/font/`.
 *
 * HOW weights are used to carry meaning: since a monospace face has less
 * visual variety than a typical UI font, [FontWeight.Bold] is reserved for
 * headlines and anything the user must not miss (fees, warnings), while
 * body/label text stays [FontWeight.Normal] — a deliberate, consistent
 * signal rather than a purely aesthetic choice.
 */
private val RetroFontFamily = FontFamily.Monospace

val MetaMatchTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = RetroFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 0.5.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = RetroFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.5.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = RetroFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = RetroFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = RetroFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = RetroFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = RetroFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
    ),
)
