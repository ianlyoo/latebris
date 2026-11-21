package io.github.ianlyoo.latebris.ui

import io.github.ianlyoo.latebris.service.MovementPhase
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun formatCoordinates(latitude: Double, longitude: Double): String =
    "${latitude.fixed(5)}, ${longitude.fixed(5)}"

internal fun formatPosition(latitude: Double?, longitude: Double?, altitude: Double?): String {
    if (latitude == null || longitude == null) return "Waiting for live coordinates…"
    val altitudeSegment = altitude?.let { " · ${it.fixed(1)} m" } ?: ""
    return formatCoordinates(latitude, longitude) + altitudeSegment
}

internal fun formatAccuracy(meters: Float): String = "±${meters.toDouble().fixed(1)} m"

internal fun formatSpeed(speedMetersPerSecond: Double): String = "${(speedMetersPerSecond * 3.6).roundToInt()} km/h"

internal fun formatEta(phase: MovementPhase, etaEpochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String = when {
    phase == MovementPhase.Arrived -> "Arrived"
    etaEpochMillis <= 0L -> if (phase == MovementPhase.Routing) "Calculating route…" else "Working out the arrival time…"
    else -> {
        val totalSeconds = (etaEpochMillis - nowMillis).coerceAtLeast(0L) / 1_000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        if (minutes > 0L) "$minutes min $seconds s left" else "$seconds s left"
    }
}

internal fun countLabel(count: Int, one: String, many: String, zero: String): String = when (count) {
    0 -> zero
    1 -> "1 $one"
    else -> "$count $many"
}

internal val MovementPhase.label: String
    get() = when (this) {
        MovementPhase.Routing -> "Routing"
        MovementPhase.Moving -> "Moving"
        MovementPhase.Arrived -> "Arrived"
        MovementPhase.Canceled -> "Canceled"
        MovementPhase.None -> "Idle"
    }

private fun Double.fixed(digits: Int): String {
    // Avoid "-0.0" for values that round to zero.
    val value = if (abs(this) < Math.pow(10.0, -digits.toDouble()) / 2) 0.0 else this
    return String.format(Locale.US, "%.${digits}f", value)
}
