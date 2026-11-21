package io.github.ianlyoo.latebris.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wayfinding pictograms, drawn on a 24-unit grid with one stroke weight so they sit
 * beside Material's filled icons. Material's core set has no Wi-Fi, cell or route glyph.
 */
enum class Pictogram {
    Gps,
    Wifi,
    Cell,
    Arrow,
    ArrowDown,
    Stop,
}

@Composable
fun PictogramIcon(
    pictogram: Pictogram,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(size)) {
        val u = this.size.minDimension / 24f
        val stroke = Stroke(width = 2.4f * u, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
        when (pictogram) {
            Pictogram.Gps -> drawGps(u, tint, stroke)
            Pictogram.Wifi -> drawWifi(u, tint)
            Pictogram.Cell -> drawCell(u, tint, stroke)
            Pictogram.Arrow -> drawArrow(u, tint, rotate = false)
            Pictogram.ArrowDown -> drawArrow(u, tint, rotate = true)
            Pictogram.Stop -> drawRect(tint, topLeft = Offset(6f * u, 6f * u), size = Size(12f * u, 12f * u))
        }
    }
}

private fun DrawScope.drawGps(u: Float, tint: Color, stroke: Stroke) {
    val c = Offset(12f * u, 12f * u)
    drawCircle(tint, radius = 6.5f * u, center = c, style = stroke)
    drawCircle(tint, radius = 2.6f * u, center = c)
    val w = stroke.width
    // Four ticks reaching past the ring: the crosshair of a position fix.
    drawLine(tint, Offset(12f * u, 1.5f * u), Offset(12f * u, 5.5f * u), w)
    drawLine(tint, Offset(12f * u, 18.5f * u), Offset(12f * u, 22.5f * u), w)
    drawLine(tint, Offset(1.5f * u, 12f * u), Offset(5.5f * u, 12f * u), w)
    drawLine(tint, Offset(18.5f * u, 12f * u), Offset(22.5f * u, 12f * u), w)
}

private fun DrawScope.drawWifi(u: Float, tint: Color) {
    val origin = Offset(12f * u, 19.5f * u)
    val radii = listOf(16f, 11f, 6f)
    radii.forEach { r ->
        val rr = r * u
        drawArc(
            color = tint,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(origin.x - rr, origin.y - rr),
            size = Size(rr * 2, rr * 2),
            style = Stroke(width = 2.4f * u, cap = StrokeCap.Butt),
        )
    }
    drawCircle(tint, radius = 2.2f * u, center = origin)
}

private fun DrawScope.drawCell(u: Float, tint: Color, stroke: Stroke) {
    val w = stroke.width
    // Mast with splayed legs.
    val mast = Path().apply {
        moveTo(12f * u, 9f * u)
        lineTo(7.5f * u, 22.5f * u)
        moveTo(12f * u, 9f * u)
        lineTo(16.5f * u, 22.5f * u)
        moveTo(9.2f * u, 17.5f * u)
        lineTo(14.8f * u, 17.5f * u)
    }
    drawPath(mast, tint, style = stroke)
    drawCircle(tint, radius = 2.2f * u, center = Offset(12f * u, 7.5f * u))
    // Radiating arcs on both sides of the antenna head.
    listOf(4.5f, 8.5f).forEach { r ->
        val rr = r * u
        val topLeft = Offset(12f * u - rr, 7.5f * u - rr)
        val arcSize = Size(rr * 2, rr * 2)
        drawArc(tint, 140f, 80f, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Butt))
        drawArc(tint, -40f, 80f, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Butt))
    }
}

/** The direction-sign arrow: a thick shaft with a 90° head. */
private fun DrawScope.drawArrow(u: Float, tint: Color, rotate: Boolean) {
    fun p(x: Float, y: Float): Offset = if (rotate) Offset((24f - y) * u, x * u) else Offset(x * u, y * u)
    val points = listOf(
        p(2.5f, 9.6f), p(12.5f, 9.6f), p(12.5f, 3.5f), p(21.5f, 12f),
        p(12.5f, 20.5f), p(12.5f, 14.4f), p(2.5f, 14.4f),
    )
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, tint)
}
