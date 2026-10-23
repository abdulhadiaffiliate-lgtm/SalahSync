package com.salahsync.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * One thin-line icon set in a single stroke weight, drawn on a 24-unit grid.
 * Tint them with Icon(tint = ...).
 */
object SalahIcons {
    private fun icon(name: String, vararg paths: String, stroke: Float = 1.5f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = stroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

    val Home = icon("home", "M4,11 L12,5 L20,11 V19 a1,1 0 0 1 -1,1 H15 V15 H9 V20 H5 a1,1 0 0 1 -1,-1 Z")
    val Clock = icon("clock", circle(12f, 12f, 8f), "M12,8 V12 L15,14")
    val Book = icon("book", "M12,6 C10,4.5 7,4 4,4.5 V17.5 C7,17 10,17.5 12,19 C14,17.5 17,17 20,17.5 V4.5 C17,4 14,4.5 12,6 Z", "M12,6 V19")
    val Compass = icon("compass", circle(12f, 12f, 9f), "M15.5,8.5 L13.5,13.5 L8.5,15.5 L10.5,10.5 Z")
    val Beads = icon(
        "beads",
        circle(12f, 4.5f, 1.6f), circle(17.3f, 6.7f, 1.6f), circle(19.5f, 12f, 1.6f), circle(17.3f, 17.3f, 1.6f),
        circle(12f, 19.5f, 1.6f), circle(6.7f, 17.3f, 1.6f), circle(4.5f, 12f, 1.6f), circle(6.7f, 6.7f, 1.6f),
        stroke = 1.4f,
    )
    val Heart = icon("heart", "M12,20 C12,20 5,15.6 5,10 A4,4 0 0 1 12,7.4 A4,4 0 0 1 19,10 C19,15.6 12,20 12,20 Z")
    val More = icon("more", circle(5f, 12f, 1.3f), circle(12f, 12f, 1.3f), circle(19f, 12f, 1.3f))
    val Flame = icon("flame", "M12,3 C14,6.5 17,8.5 17,13 A5,5 0 0 1 7,13 C7,11 8,9.5 9,8.5 C9,10.5 10,11.5 11,11.5 C11,8.5 10,6.5 12,3 Z")
    val Speaker = icon("speaker", "M4,10 V14 H8 L13,18 V6 L8,10 Z", "M16.5,9 A4,4 0 0 1 16.5,15", "M19,6.5 A7.5,7.5 0 0 1 19,17.5")
    val Bell = icon("bell", "M6,16 V11 A6,6 0 0 1 18,11 V16 L19.5,18 H4.5 Z", "M10,20.5 A2.2,2.2 0 0 0 14,20.5")
    val Muted = icon("muted", "M4,10 V14 H8 L13,18 V6 L8,10 Z", "M17,10 L21,14", "M21,10 L17,14")
    val BellOff = icon("bell_off", "M6,16 V11 A6,6 0 0 1 15.5,6.2", "M18,10.5 V16 L19.5,18 H8", "M4,4 L20,20")
    val Check = icon("check", circle(12f, 12f, 9f), "M8,12.5 L10.6,15 L16,9.5", stroke = 1.6f)
    val Circle = icon("circle", circle(12f, 12f, 9f), stroke = 1.4f)
    val Pin = icon("pin", "M12,21 C12,21 6,15.5 6,10 A6,6 0 0 1 18,10 C18,15.5 12,21 12,21 Z", circle(12f, 10f, 2.2f))
    val Sliders = icon("sliders", "M4,7 H14", "M18,7 H20", "M4,17 H8", "M12,17 H20", circle(16f, 7f, 2f), circle(10f, 17f, 2f))
    val ChevronLeft = icon("chevron_left", "M15,5 L8,12 L15,19")
    val ChevronRight = icon("chevron_right", "M9,5 L16,12 L9,19")
    val Star = icon("star", "M12,3 L14.5,8.5 L20,9.5 L16,13.5 L17,19 L12,16.2 L7,19 L8,13.5 L4,9.5 L9.5,8.5 Z")
    val Share = icon("share", "M12,4 V15", "M8,8 L12,4 L16,8", "M5,13 V19 A1,1 0 0 0 6,20 H18 A1,1 0 0 0 19,19 V13")
    val Shield = icon("shield", "M12,3 L19,6 V11 C19,15.5 16,19 12,21 C8,19 5,15.5 5,11 V6 Z", "M9,12 L11,14 L15,10")
    val Reset = icon("reset", "M5,12 A7,7 0 1 0 7.1,7", "M5,4 V8 H9")
}
