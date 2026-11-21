package io.github.ianlyoo.latebris.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.ianlyoo.latebris.R

/** Atkinson Hyperlegible: a face drawn for legibility at a distance, like signage lettering. */
val SignFace = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

/** Measured values (coordinates, frequencies, ids) keep tabular figures so live numbers do not jitter. */
val Figures = TextStyle(
    fontFamily = FontFamily.Default,
    fontFeatureSettings = "tnum",
)

private fun sign(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = SignFace,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

val LatebrisTypography = Typography(
    displaySmall = sign(36, 40, FontWeight.Bold, -0.5),
    headlineMedium = sign(28, 34, FontWeight.Bold, -0.3),
    headlineSmall = sign(24, 30, FontWeight.Bold, -0.2),
    titleLarge = sign(22, 28, FontWeight.Bold),
    titleMedium = sign(17, 24, FontWeight.Bold),
    titleSmall = sign(15, 20, FontWeight.Bold),
    bodyLarge = sign(16, 24, FontWeight.Normal),
    bodyMedium = sign(14, 20, FontWeight.Normal),
    bodySmall = sign(13, 18, FontWeight.Normal),
    labelLarge = sign(15, 20, FontWeight.Bold, 0.1),
    labelMedium = sign(13, 16, FontWeight.Bold, 0.2),
    labelSmall = sign(12, 16, FontWeight.Bold, 0.2),
)
