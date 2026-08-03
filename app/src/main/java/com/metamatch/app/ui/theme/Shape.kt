package com.metamatch.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Retro Shapes
 * ==============
 *
 * WHAT: every corner radius in the app is zero. `RoundedCornerShape(0.dp)`
 * is a perfect rectangle — sharp, right-angled corners, matching the
 * brief's "sharp borders/corners" requirement.
 *
 * WHY every size still gets its own explicit entry instead of one shared
 * constant: Material 3's [Shapes] API requires an `extraSmall` through
 * `extraLarge` entry (used respectively by small chips, medium cards,
 * large dialogs, etc.). Declaring all five as `0.dp` — rather than relying
 * on a default — makes the "everything is sharp, on purpose" decision
 * explicit and impossible to silently regress if Material 3's defaults
 * ever change.
 */
val MetaMatchShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)
