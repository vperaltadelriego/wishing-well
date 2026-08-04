package com.metamatch.app.ui.wish

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import com.metamatch.app.ui.theme.RetroBorder
import com.metamatch.app.ui.theme.RetroCyan
import com.metamatch.app.ui.theme.RetroSurface
import com.metamatch.app.ui.theme.RetroYellow
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * WishGlobeCanvas
 * =================
 *
 * WHAT: a hand-drawn, code-only rotating globe — the Wish feature's
 * visual centerpiece. Every wish cast (and, ambiently, the seeded pool
 * from [com.metamatch.app.data.mock.WishSeedData]) shows up as a brief
 * spark of light on the sphere.
 *
 * WHY this is drawn entirely with [Canvas] shapes instead of an image or
 * a 3D rendering library: same "zero setup, clone and run" reasoning as
 * `ui/intro/IntroScreen.kt`'s pixel-art well — no binary asset to go
 * missing, no new rendering dependency. The rotation illusion is the
 * classic 2D trick real 8/16-bit-era globe animations used: several
 * "meridian" ellipses whose horizontal radius is scaled by
 * `|cos(angle)|` as `angle` advances, which reads as vertical lines
 * sweeping around a sphere even though nothing is actually 3D.
 *
 * HOW sparks animate without a manual per-frame ticker: each [Spark]
 * owns its own [Animatable] alpha, driven by a `LaunchedEffect` that
 * fades it in then out and removes it from [sparks] when done. Compose's
 * snapshot system redraws the [Canvas] automatically whenever an
 * [Animatable.value] read inside its draw lambda changes — no manual
 * invalidation needed. [castSignal] is a plain counter the caller
 * increments to request one *explicit* spark (e.g. "I just tossed a
 * wish in"), on top of the continuous ambient sparks that keep the globe
 * feeling alive even with no interaction.
 *
 * @param castSignal Increment this from the caller to spawn one
 *   explicit, brighter spark — see class doc.
 */
@Composable
fun WishGlobeCanvas(modifier: Modifier = Modifier, castSignal: Int = 0) {
    val sparks = remember { mutableStateListOf<Spark>() }

    val infiniteTransition = rememberInfiniteTransition(label = "globe-rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "globe-angle",
    )

    // Ambient sparks: the globe keeps sparking gently even with no
    // interaction, so it reads as "wishes are constantly landing," not
    // just a reaction to the current user's own taps.
    LaunchedEffect(Unit) {
        while (true) {
            delay(900)
            sparks += Spark.atRandomPoint(bright = false)
        }
    }

    // One explicit, brighter spark per increment of castSignal — skips
    // the initial composition (castSignal starting at 0) via the key.
    LaunchedEffect(castSignal) {
        if (castSignal > 0) sparks += Spark.atRandomPoint(bright = true)
    }

    // Drives each spark's fade in/out and removes it once done — see
    // rememberSparkAnimator's own doc.
    sparks.toList().forEach { spark -> rememberSparkAnimator(sparks, spark) }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f * 0.9f

        // The sphere itself, shaded so it doesn't read as a flat circle.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(RetroCyan.copy(alpha = 0.35f), RetroSurface),
                center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                radius = radius * 1.6f,
            ),
            radius = radius,
            center = center,
        )
        drawCircle(color = RetroBorder, radius = radius, center = center, style = Stroke(width = 3f))

        // Meridians: vertical rings whose horizontal squash fakes rotation.
        val meridianPhases = listOf(0f, 60f, 120f, 180f, 240f, 300f)
        meridianPhases.forEach { phaseDegrees ->
            val phaseRadians = Math.toRadians(phaseDegrees.toDouble()).toFloat()
            val horizontalRadius = radius * kotlin.math.abs(cos(rotationAngle + phaseRadians))
            drawOval(
                color = RetroBorder.copy(alpha = 0.5f),
                topLeft = Offset(center.x - horizontalRadius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(horizontalRadius * 2f, radius * 2f),
                style = Stroke(width = 2f),
            )
        }

        // A couple of static latitude rings (the equator and one more).
        listOf(0f, 0.45f).forEach { verticalOffsetFraction ->
            val verticalOffset = radius * verticalOffsetFraction
            val ringRadius = radius * (1f - verticalOffsetFraction * 0.6f)
            drawOval(
                color = RetroBorder.copy(alpha = 0.35f),
                topLeft = Offset(center.x - radius, center.y - verticalOffset - ringRadius * 0.28f),
                size = androidx.compose.ui.geometry.Size(radius * 2f, ringRadius * 0.56f),
                style = Stroke(width = 1.5f),
            )
        }

        sparks.forEach { spark ->
            val sparkCenter = Offset(
                center.x + (spark.xFraction - 0.5f) * radius * 2f,
                center.y + (spark.yFraction - 0.5f) * radius * 2f,
            )
            drawCircle(
                color = RetroYellow,
                radius = if (spark.bright) 10f else 6f,
                center = sparkCenter,
                alpha = spark.alpha.value,
            )
        }
    }
}

/** One spark of light on the globe's surface — see [WishGlobeCanvas]'s
 * own docs for how its fade animation drives redraws. */
private class Spark(val xFraction: Float, val yFraction: Float, val bright: Boolean) {
    val alpha = Animatable(0f)

    companion object {
        /** A point within the visible disc (not the whole square canvas)
         * — polar coordinates constrained to an 80%-of-radius disc so
         * sparks never render outside the sphere's silhouette. */
        fun atRandomPoint(bright: Boolean): Spark {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val distance = Random.nextFloat() * 0.8f
            return Spark(
                xFraction = 0.5f + 0.5f * distance * cos(angle),
                yFraction = 0.5f + 0.5f * distance * sin(angle),
                bright = bright,
            )
        }
    }
}

@Composable
private fun rememberSparkAnimator(sparks: MutableList<Spark>, spark: Spark) {
    LaunchedEffect(spark) {
        spark.alpha.animateTo(1f, tween(200))
        spark.alpha.animateTo(0f, tween(1300))
        sparks.remove(spark)
    }
}
