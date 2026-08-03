package com.metamatch.app.ui.intro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.metamatch.app.ui.components.RetroButton
import com.metamatch.app.ui.theme.RetroBorder
import com.metamatch.app.ui.theme.RetroCyan
import com.metamatch.app.ui.theme.RetroSurface
import com.metamatch.app.ui.theme.RetroTextMuted
import com.metamatch.app.ui.theme.RetroYellow

/**
 * IntroScreen
 * =============
 *
 * WHAT: the app's very first screen — the "Wishing Well" brand splash.
 * Sets the product's visual metaphor before the user ever sees a form:
 * publishing an intent is framed as "tossing a coin in", and a computed
 * match is a "coincidence" the well surfaces back.
 *
 * WHY the well is hand-drawn with [Canvas] instead of an image asset: this
 * project's whole premise is "clone it, hit Run, zero setup" — no
 * drawable/PNG/SVG asset to go missing or need licensing. A few rectangles
 * drawn in the app's own retro palette ([RetroBorder]/[RetroYellow]/
 * [RetroCyan]) keep that promise while still giving the rebrand a real,
 * on-brand piece of art instead of just retitled text.
 *
 * HOW this fits the new navigation: this is the `intro` start destination
 * of the [androidx.navigation.compose.NavHost] built in `MainActivity.kt`.
 * Tapping "Toss a coin" pops it off the back stack (see the `popUpTo` call
 * at the call site) so the back button from the Hub exits the app rather
 * than returning to the splash — a splash screen is meant to be seen once
 * per app open, not re-visitable.
 */
@Composable
fun IntroScreen(onTossCoin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WishingWellArt(modifier = Modifier.size(220.dp))

        Text(
            text = "WISHING WELL",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "a meta-finder for wild coincidences",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "powered by the MetaMatch engine",
            style = MaterialTheme.typography.labelSmall,
            color = RetroTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )

        RetroButton(
            label = "Toss a coin →",
            onClick = onTossCoin,
            modifier = Modifier.padding(top = 32.dp),
        )
    }
}

/**
 * A pixel-art wishing well: two stone side walls, a base, an A-frame roof
 * with a crossbeam, a bucket hanging from a rope at the beam's center, a
 * coin mid-toss above the opening, and a scatter of stars. Every shape is
 * an axis-aligned rectangle sized as a fraction of the canvas — deliberately
 * blocky, matching the "8-bit" language the rest of the app already uses
 * (see `ui/theme/Shape.kt`: zero corner radius everywhere).
 */
@Composable
private fun WishingWellArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawStars(w, h)

        // Stone base + side walls (a blocky "U").
        val wellLeft = w * 0.20f
        val wellRight = w * 0.80f
        val wellTop = h * 0.55f
        val wellBottom = h * 0.85f
        val wallThickness = w * 0.10f

        drawRect(
            color = RetroSurface,
            topLeft = Offset(wellLeft, wellTop),
            size = Size(wellRight - wellLeft, wellBottom - wellTop),
        )
        drawRect(
            color = RetroCyan,
            topLeft = Offset(wellLeft, wellTop),
            size = Size(wallThickness, wellBottom - wellTop),
        )
        drawRect(
            color = RetroCyan,
            topLeft = Offset(wellRight - wallThickness, wellTop),
            size = Size(wallThickness, wellBottom - wellTop),
        )
        // The well's rim — a thin bright bar across the opening.
        drawRect(
            color = RetroBorder,
            topLeft = Offset(wellLeft, wellTop),
            size = Size(wellRight - wellLeft, h * 0.04f),
        )

        // A-frame roof posts + crossbeam.
        val postWidth = w * 0.06f
        val postTop = h * 0.10f
        drawRect(
            color = RetroBorder,
            topLeft = Offset(wellLeft - postWidth * 0.3f, postTop),
            size = Size(postWidth, wellTop - postTop),
        )
        drawRect(
            color = RetroBorder,
            topLeft = Offset(wellRight - postWidth * 0.7f, postTop),
            size = Size(postWidth, wellTop - postTop),
        )
        drawRect(
            color = RetroBorder,
            topLeft = Offset(wellLeft - postWidth * 0.3f, postTop),
            size = Size(wellRight - wellLeft + postWidth * 0.4f, h * 0.035f),
        )

        // Rope + bucket, hanging from the beam's center.
        val centerX = (wellLeft + wellRight) / 2f
        drawRect(
            color = RetroTextMuted,
            topLeft = Offset(centerX - w * 0.005f, postTop + h * 0.035f),
            size = Size(w * 0.01f, h * 0.22f),
        )
        drawRect(
            color = RetroYellow,
            topLeft = Offset(centerX - w * 0.08f, postTop + h * 0.25f),
            size = Size(w * 0.16f, h * 0.10f),
        )

        // A coin mid-toss above the opening — the "make a wish" moment.
        drawCircle(
            color = RetroYellow,
            radius = w * 0.035f,
            center = Offset(wellRight - w * 0.05f, wellTop - h * 0.10f),
        )
    }
}

private fun DrawScope.drawStars(w: Float, h: Float) {
    val starPositions = listOf(
        Offset(w * 0.10f, h * 0.08f),
        Offset(w * 0.90f, h * 0.05f),
        Offset(w * 0.05f, h * 0.30f),
        Offset(w * 0.95f, h * 0.28f),
        Offset(w * 0.75f, h * 0.02f),
    )
    starPositions.forEach { position ->
        drawCircle(color = RetroBorder, radius = w * 0.012f, center = position)
    }
}
