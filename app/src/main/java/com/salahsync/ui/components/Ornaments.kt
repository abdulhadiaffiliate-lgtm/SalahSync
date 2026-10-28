package com.salahsync.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salahsync.ui.theme.Salah

/** The eight-pointed star tiling, drawn at low opacity behind headers. */
@Composable
fun GeometricPattern(modifier: Modifier = Modifier, color: Color = Salah.colors.accent, alpha: Float = 0.07f) {
    Canvas(modifier) {
        val cell = 60.dp.toPx()
        val stroke = Stroke(width = 0.8.dp.toPx())
        val tint = color.copy(alpha = alpha)
        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                val cx = x + cell / 2
                val cy = y + cell / 2
                val star = Path().apply {
                    val outer = cell / 2
                    val inner = cell * 0.1333f
                    moveTo(cx, cy - outer)
                    lineTo(cx + inner, cy - inner); lineTo(cx + outer, cy)
                    lineTo(cx + inner, cy + inner); lineTo(cx, cy + outer)
                    lineTo(cx - inner, cy + inner); lineTo(cx - outer, cy)
                    lineTo(cx - inner, cy - inner); close()
                }
                drawPath(star, tint, style = stroke)
                drawCircle(tint, radius = cell * 0.15f, center = Offset(cx, cy), style = stroke)
                x += cell
            }
            y += cell
        }
    }
}

/** A gold hairline that fades out at both ends. */
@Composable
fun GoldDivider(modifier: Modifier = Modifier, color: Color = Salah.colors.accent) {
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, color, Color.Transparent)))
    }
}

/** Countdown ring: [progress] is the share of the time between prayers already passed. */
@Composable
fun CountdownRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 112.dp,
    content: @Composable () -> Unit,
) {
    val c = Salah.colors
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val w = 3.dp.toPx()
            val inset = w / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
            drawArc(c.text.copy(alpha = 0.08f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            rotate(-90f) {
                drawArc(
                    c.accent, 0f, 360f * progress.coerceIn(0f, 1f), false,
                    Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

@Composable
fun EyebrowText(text: String, color: Color = Salah.colors.textMuted, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = com.salahsync.ui.theme.Eyebrow, color = color, modifier = modifier)
}

/** An icon on a soft tinted disc, used to give entry points a clear, friendly mark. */
@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 44.dp, tint: Color = Salah.colors.accent) {
    Box(modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size / 2))
    }
}
