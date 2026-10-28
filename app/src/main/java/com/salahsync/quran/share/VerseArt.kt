package com.salahsync.quran.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.salahsync.R
import com.salahsync.core.quran.QuranText
import com.salahsync.core.quran.Surah
import java.io.File

/** The four backgrounds offered when sharing a verse. */
enum class CardStyle(
    val label: String,
    val top: Int,
    val bottom: Int,
    val text: Int,
    val muted: Int,
    val accent: Int,
    val pattern: Boolean,
) {
    EMERALD("Emerald", 0xFF14362E.toInt(), 0xFF0B231E.toInt(), 0xFFF6F1E7.toInt(), 0xFFB9B4A8.toInt(), 0xFFD9BC84.toInt(), true),
    IVORY("Ivory", 0xFFFBF8F1.toInt(), 0xFFEFE8DA.toInt(), 0xFF0E2A24.toInt(), 0xFF5E665F.toInt(), 0xFF7A5C26.toInt(), true),
    NIGHT("Night", 0xFF101614.toInt(), 0xFF050807.toInt(), 0xFFF6F1E7.toInt(), 0xFF9C988F.toInt(), 0xFFD9BC84.toInt(), false),
    DAWN("Dawn", 0xFF2E4A52.toInt(), 0xFF8C6A4E.toInt(), 0xFFFBF5EA.toInt(), 0xFFE9DCC6.toInt(), 0xFFF1D7A3.toInt(), false),
}

/** The image size: a 4:5 post, or a 9:16 story for WhatsApp status and Instagram. */
enum class CardShape(val label: String, val height: Int) {
    POST("Post", 1350),
    STORY("Story", 1920),
}

/**
 * Text to draw: Arabic (may be blank), an optional translation, its reference, and an
 * optional [note] under the reference, such as a hadith's grading.
 */
data class VerseCard(
    val arabic: String,
    val translation: String?,
    val translationRtl: Boolean,
    val reference: String,
    val note: String? = null,
)

/** Draws verse and hadith cards, and surah artwork, with the app's Mushaf, Urdu and Latin fonts. */
object VerseArt {
    private const val WIDTH = 1080

    /** The tallest image we make; longer text is offered as text instead. */
    private const val MAX_HEIGHT = 3240

    /** Loads a bundled font; variable fonts are pinned to [weight] where Android allows it. */
    internal fun font(context: Context, id: Int, weight: Int = 500, italic: Boolean = false): Typeface {
        val tf = ResourcesCompat.getFont(context, id) ?: Typeface.DEFAULT
        return if (Build.VERSION.SDK_INT >= 28) Typeface.create(tf, weight, italic) else tf
    }

    private fun layout(text: CharSequence, paint: TextPaint, width: Int, rtl: Boolean, spacing: Float): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
            .setLineSpacing(0f, spacing)
            .setIncludePad(true)
            .build()

    /** Lays out text at the largest size from [max] down to [min] that fits in [maxHeight]. */
    private fun fit(text: String, paint: TextPaint, width: Int, rtl: Boolean, spacing: Float, max: Float, min: Float, maxHeight: Int): StaticLayout {
        var size = max
        while (true) {
            paint.textSize = size
            val l = layout(text, paint, width, rtl, spacing)
            if (l.height <= maxHeight || size <= min) return l
            size -= 2f
        }
    }

    /**
     * Draws [card] on [style] at [shape]. Returns null when the text is too long for one
     * readable image, so the caller can suggest less text or sharing it as text.
     */
    fun render(context: Context, card: VerseCard, style: CardStyle, shape: CardShape = CardShape.POST): Bitmap? {
        val margin = 110
        val textWidth = WIDTH - margin * 2
        val arabicPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font(context, R.font.amiri_quran, 400)
            color = style.text
        }
        val transPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = if (card.translationRtl) font(context, R.font.noto_nastaliq_urdu, 400) else font(context, R.font.cormorant_garamond_italic, italic = true)
            color = style.text
        }
        val refPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font(context, R.font.manrope)
            color = style.accent
            textSize = 30f
            letterSpacing = 0.18f
        }
        val notePaint = TextPaint(refPaint).apply { color = style.muted; textSize = 26f; letterSpacing = 0.04f }
        val brandPaint = TextPaint(refPaint).apply { color = style.muted; textSize = 26f }

        // Text budgets grow with the shape, so a story gets larger type than a post.
        val translationText = card.translation?.takeIf { it.isNotBlank() }
        val alone = card.arabic.isBlank() || translationText == null
        val arabicBudget = (shape.height * if (alone) 0.8f else 0.66f).toInt()
        val transBudget = (shape.height * if (alone) 0.8f else 0.46f).toInt()
        val arabic = card.arabic.takeIf { it.isNotBlank() }?.let { fit(it, arabicPaint, textWidth, true, 1.55f, 76f, 34f, arabicBudget) }
        val translation = translationText?.let {
            if (card.translationRtl) fit(it, transPaint, textWidth, true, 1.35f, 44f, 26f, transBudget)
            else fit("“$it”", transPaint, textWidth, false, 1.2f, 50f, 28f, transBudget)
        }
        val note = card.note?.takeIf { it.isNotBlank() }?.let { layout(it, notePaint, textWidth, false, 1.2f) }

        val gap = 64
        val blocks = listOfNotNull(arabic, translation)
        val content = blocks.sumOf { it.height } + (blocks.size - 1).coerceAtLeast(0) * gap * 2 + gap + 40 +
            (note?.let { 16 + it.height } ?: 0)
        val height = maxOf(shape.height, content + 2 * 190)
        if (height > MAX_HEIGHT) return null
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background(canvas, style, WIDTH.toFloat(), height.toFloat())

        var y = ((height - content) / 2f).coerceAtLeast(150f)
        blocks.forEachIndexed { i, block ->
            if (i > 0) {
                hairline(canvas, style, y, WIDTH.toFloat())
                y += gap
            }
            canvas.save(); canvas.translate(margin.toFloat(), y); block.draw(canvas); canvas.restore()
            y += block.height + gap
        }
        val ref = card.reference.uppercase()
        canvas.drawText(ref, (WIDTH - refPaint.measureText(ref)) / 2f, y + 30f, refPaint)
        if (note != null) {
            canvas.save(); canvas.translate(margin.toFloat(), y + 56f); note.draw(canvas); canvas.restore()
        }
        val brand = "SALAHSYNC"
        canvas.drawText(brand, (WIDTH - brandPaint.measureText(brand)) / 2f, height - 80f, brandPaint)
        return bitmap
    }

    /** Square artwork for the lock screen: the surah's Arabic name in a gold frame. */
    fun surahArtwork(context: Context, surah: Surah, size: Int): Bitmap {
        val style = CardStyle.EMERALD
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background(canvas, style, size.toFloat(), size.toFloat())
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.accent; this.style = Paint.Style.STROKE; strokeWidth = size / 170f; alpha = 150
        }
        star(canvas, size / 2f, size / 2f, size * 0.36f, frame)
        val name = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font(context, R.font.amiri_quran, 400); color = style.text; textSize = size * 0.15f
        }
        val l = layout(surah.arabicName, name, size, true, 1f)
        canvas.save(); canvas.translate(0f, size / 2f - l.height / 2f); l.draw(canvas); canvas.restore()
        val num = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font(context, R.font.manrope); color = style.accent; textSize = size * 0.05f; letterSpacing = 0.18f
        }
        val label = QuranText.arabicNumber(surah.number)
        canvas.drawText(label, (size - num.measureText(label)) / 2f, size * 0.86f, num)
        return bitmap
    }

    internal fun background(canvas: Canvas, style: CardStyle, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, h, style.top, style.bottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = RadialGradient(w / 2, h * 0.35f, w * 0.8f, (style.accent and 0x00FFFFFF) or 0x1A000000, 0, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        if (style.pattern) {
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = style.accent; this.style = Paint.Style.STROKE; strokeWidth = w / 900f; alpha = 26
            }
            val cell = w / 6f
            var y = 0f
            while (y < h) {
                var x = 0f
                while (x < w) {
                    star(canvas, x + cell / 2, y + cell / 2, cell / 2, line)
                    canvas.drawCircle(x + cell / 2, y + cell / 2, cell * 0.15f, line)
                    x += cell
                }
                y += cell
            }
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.accent; this.style = Paint.Style.STROKE; strokeWidth = w / 540f; alpha = 110
        }
        val inset = w / 22f
        canvas.drawRoundRect(inset, inset, w - inset, h - inset, w / 30f, w / 30f, border)
    }

    /** The eight-pointed star used across the app. */
    internal fun star(canvas: Canvas, cx: Float, cy: Float, outer: Float, paint: Paint) {
        val inner = outer * 0.2666f
        val p = Path().apply {
            moveTo(cx, cy - outer)
            lineTo(cx + inner, cy - inner); lineTo(cx + outer, cy)
            lineTo(cx + inner, cy + inner); lineTo(cx, cy + outer)
            lineTo(cx - inner, cy + inner); lineTo(cx - outer, cy)
            lineTo(cx - inner, cy - inner); close()
        }
        canvas.drawPath(p, paint)
    }

    internal fun hairline(canvas: Canvas, style: CardStyle, y: Float, w: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(w * 0.3f, y, w * 0.7f, y, intArrayOf(0, style.accent, 0), null, Shader.TileMode.CLAMP)
            strokeWidth = 2f
        }
        canvas.drawLine(w * 0.3f, y, w * 0.7f, y, paint)
    }

    /** Saves [bitmap] to the share cache and opens the system share sheet. */
    fun share(context: Context, bitmap: Bitmap, text: String, fileName: String = "verse.png") {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, uri)
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
}
