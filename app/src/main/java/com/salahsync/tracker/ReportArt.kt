package com.salahsync.tracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import com.salahsync.R
import com.salahsync.core.Prayer
import com.salahsync.core.tracker.PrayerStatus
import com.salahsync.core.tracker.WeeklyReport
import com.salahsync.quran.share.CardStyle
import com.salahsync.quran.share.VerseArt
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Draws the weekly report as an image to share, in the app's emerald and gold. It is
 * made on the phone and only leaves it if the user shares it.
 */
object ReportArt {
    private const val WIDTH = 1080
    private const val HEIGHT = 1800

    private val dayShort = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)

    fun rangeLabel(report: WeeklyReport): String {
        val s = report.start
        val e = report.end
        val end = e.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))
        val start = when {
            s.year != e.year -> s.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))
            s.month != e.month -> s.format(DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH))
            else -> s.dayOfMonth.toString()
        }
        return "$start – $end"
    }

    fun render(context: Context, report: WeeklyReport): Bitmap {
        val style = CardStyle.EMERALD
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        VerseArt.background(canvas, style, WIDTH.toFloat(), HEIGHT.toFloat())
        val serif = VerseArt.font(context, R.font.cormorant_garamond, 500)
        val sans = VerseArt.font(context, R.font.manrope)

        fun paint(size: Float, color: Int, bold: Boolean = false, serifFace: Boolean = false, spacing: Float = 0f) =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = if (serifFace) serif else sans
                textSize = size
                this.color = color
                letterSpacing = spacing
                isFakeBoldText = bold
            }

        fun centered(text: String, y: Float, p: Paint, cx: Float = WIDTH / 2f) =
            canvas.drawText(text, cx - p.measureText(text) / 2f, y, p)

        val eyebrow = paint(28f, style.accent, spacing = 0.2f)
        val muted = paint(30f, style.muted)
        centered("WEEKLY PRAYER REPORT", 180f, eyebrow)
        centered(rangeLabel(report), 260f, paint(54f, style.text, serifFace = true))

        val t = report.totals
        centered("${t.prayed} of ${t.tracked}", 470f, paint(170f, style.accent, serifFace = true))
        centered(if (report.inProgress) "prayers prayed so far this week" else "prayers prayed this week", 535f, muted)
        VerseArt.hairline(canvas, style, 610f, WIDTH.toFloat())

        val stats = listOf("On time" to t.onTime, "Late" to t.late, "Missed" to t.missed)
        val colW = (WIDTH - 220) / 3f
        stats.forEachIndexed { i, (label, n) ->
            val cx = 110 + colW * i + colW / 2
            centered("$n", 720f, paint(96f, style.text, serifFace = true), cx)
            centered(label.uppercase(), 772f, paint(24f, style.muted, spacing = 0.18f), cx)
        }

        // The week at a glance: one row per day, one dot per prayer.
        val gridLeft = 300f
        val step = 120f
        val header = paint(24f, style.muted, spacing = 0.12f)
        Prayer.fard.forEachIndexed { i, p -> centered(p.displayName.uppercase(), 870f, header, gridLeft + step * i) }
        val dayPaint = paint(30f, style.text)
        val filled = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.accent }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.accent; this.style = Paint.Style.STROKE; strokeWidth = 4f }
        val faint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.muted; this.style = Paint.Style.STROKE; strokeWidth = 2.5f; alpha = 110
        }
        val cross = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.muted; strokeWidth = 3f; alpha = 160 }
        report.days.forEachIndexed { row, day ->
            val y = 940f + row * 66f
            canvas.drawText(day.date.format(dayShort), 130f, y + 10f, dayPaint)
            Prayer.fard.forEachIndexed { i, p ->
                val cx = gridLeft + step * i
                when (day.statuses[p]) {
                    PrayerStatus.ON_TIME -> canvas.drawCircle(cx, y, 17f, filled)
                    PrayerStatus.LATE -> canvas.drawCircle(cx, y, 15f, ring)
                    PrayerStatus.MISSED -> {
                        canvas.drawLine(cx - 9f, y - 9f, cx + 9f, y + 9f, cross)
                        canvas.drawLine(cx + 9f, y - 9f, cx - 9f, y + 9f, cross)
                    }
                    else -> canvas.drawCircle(cx, y, 4f, faint)
                }
            }
        }
        val legendY = 940f + 7 * 66f + 20f
        val legend = paint(24f, style.muted)
        var x = 345f
        canvas.drawCircle(x, legendY - 8f, 10f, filled); x += 22f
        canvas.drawText("On time", x, legendY, legend); x += legend.measureText("On time") + 50f
        canvas.drawCircle(x, legendY - 8f, 9f, ring); x += 22f
        canvas.drawText("Late", x, legendY, legend); x += legend.measureText("Late") + 50f
        canvas.drawLine(x - 7f, legendY - 15f, x + 7f, legendY - 1f, cross)
        canvas.drawLine(x + 7f, legendY - 15f, x - 7f, legendY - 1f, cross); x += 22f
        canvas.drawText("Missed", x, legendY, legend)

        VerseArt.hairline(canvas, style, legendY + 50f, WIDTH.toFloat())
        val footer = listOf(
            "Qada owed" to "${report.qadaOwed}",
            "Streak" to days(report.currentStreak),
            "Best" to days(report.bestStreak),
        )
        footer.forEachIndexed { i, (label, value) ->
            val cx = 110 + colW * i + colW / 2
            centered(value, legendY + 135f, paint(52f, style.text, serifFace = true), cx)
            centered(label.uppercase(), legendY + 178f, paint(22f, style.muted, spacing = 0.18f), cx)
        }
        centered("SALAHSYNC", HEIGHT - 80f, paint(26f, style.muted, spacing = 0.18f))
        return bitmap
    }

    private fun days(n: Int) = if (n == 1) "1 day" else "$n days"
}
