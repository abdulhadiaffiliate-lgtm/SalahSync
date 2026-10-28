package com.salahsync.ui.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.salahsync.core.quran.Quran
import com.salahsync.quran.Translation
import com.salahsync.quran.Verse
import com.salahsync.quran.share.CardShape
import com.salahsync.quran.share.CardStyle
import com.salahsync.quran.share.VerseArt
import com.salahsync.quran.share.VerseCard
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.Salah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shares a verse from the reader as an image or text. */
@Composable
fun ShareVerseDialog(verse: Verse, onDismiss: () -> Unit) {
    val surah = Quran.surah(verse.ref.surah)
    val translations = verse.translations.mapValues { (_, t) -> t.replace(Regex("""\[\d+]"""), "") }
    ShareCardDialog(
        arabic = verse.arabic,
        translations = translations,
        reference = "${surah.name} ${verse.ref}",
        onDismiss = onDismiss,
    )
}

@Composable
fun ShareCardDialog(arabic: String, translations: Map<Translation, String>, reference: String, onDismiss: () -> Unit) {
    ShareImageDialog(
        title = "Share verse",
        arabic = arabic,
        translations = translations.map { (t, text) -> CardText(t.label, text, t.rtl) },
        reference = reference,
        onDismiss = onDismiss,
    )
}

/** A translation offered on a share card. */
data class CardText(val label: String, val text: String, val rtl: Boolean)

/**
 * Turns Arabic, a translation and its reference into an image on one of four backgrounds,
 * as a post or a story, then shares or copies it. [note] goes under the reference, such as
 * a hadith's grading. With [arabicOptional] the Arabic can be left off, which long hadith need.
 */
@Composable
fun ShareImageDialog(
    title: String,
    arabic: String,
    translations: List<CardText>,
    reference: String,
    onDismiss: () -> Unit,
    note: String? = null,
    arabicOptional: Boolean = false,
    showArabicInitially: Boolean = true,
) {
    val c = Salah.colors
    val context = LocalContext.current
    val hasArabic = arabic.isNotBlank()
    var style by remember { mutableStateOf(CardStyle.EMERALD) }
    var shape by remember { mutableStateOf(CardShape.POST) }
    var withArabic by remember { mutableStateOf(hasArabic && (showArabicInitially || translations.isEmpty())) }
    var withTranslation by remember { mutableStateOf(translations.firstOrNull()) }
    val card = VerseCard(if (withArabic) arabic else "", withTranslation?.text, withTranslation?.rtl == true, reference, note)
    // Null while drawing; a null bitmap once drawn means the text is too long for one image.
    val image by produceState<Drawn?>(null, card, style, shape) {
        value = null
        value = Drawn(withContext(Dispatchers.Default) { VerseArt.render(context, card, style, shape) })
    }
    val bitmap = image?.bitmap

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val dialogShape = RoundedCornerShape(28.dp)
        Column(
            Modifier.padding(20.dp).fillMaxWidth().clip(dialogShape).background(c.surface).border(1.dp, c.accentHairline, dialogShape)
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = c.text)
            Box(
                Modifier.fillMaxWidth().heightIn(min = 200.dp).clip(RoundedCornerShape(16.dp)).background(c.surfaceDeep),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    image == null -> CircularProgressIndicator(color = c.accent, modifier = Modifier.padding(48.dp))
                    bitmap == null -> Text(
                        if (arabicOptional && withArabic && withTranslation != null) "This is too long for one image. Hide the Arabic, or copy the text instead."
                        else "This is too long for one image. Copy the text instead.",
                        style = MaterialTheme.typography.bodyMedium, color = c.textMuted, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                    else -> Image(bitmap.asImageBitmap(), contentDescription = "Preview of the image", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
                }
            }

            EyebrowText("Background")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardStyle.entries.forEach { s ->
                    val on = s == style
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(Brush.verticalGradient(listOf(Color(s.top), Color(s.bottom))))
                                .border(if (on) 2.dp else 1.dp, if (on) c.accent else c.hairline, CircleShape)
                                .selectable(selected = on, role = Role.RadioButton) { style = s },
                        )
                        Text(s.label, style = MaterialTheme.typography.labelSmall, color = if (on) c.accent else c.textMuted)
                    }
                }
            }

            EyebrowText("Size")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardShape.entries.forEach { s -> Pill(s.label, null, filled = shape == s) { shape = s } }
            }

            if (arabicOptional && hasArabic && translations.isNotEmpty()) {
                EyebrowText("Arabic")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Show", null, filled = withArabic) { withArabic = true }
                    Pill("Hide", null, filled = !withArabic) {
                        withArabic = false
                        if (withTranslation == null) withTranslation = translations.first()
                    }
                }
            }

            if (translations.isNotEmpty()) {
                EyebrowText("Translation")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    translations.forEach { t ->
                        Pill(t.label, null, filled = withTranslation == t) { withTranslation = t }
                    }
                    // Something has to be on the card, so "None" needs the Arabic.
                    if (withArabic) Pill("None", null, filled = withTranslation == null) { withTranslation = null }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Share image", SalahIcons.Share, filled = true, enabled = bitmap != null) {
                    bitmap?.let { VerseArt.share(context, it, "$reference\n\nShared from SalahSync") }
                }
                Pill("Copy text", SalahIcons.Copy) {
                    val text = buildString {
                        if (withArabic) append(arabic)
                        withTranslation?.let { if (isNotEmpty()) append("\n\n"); append(it.text) }
                        append("\n\n").append(reference)
                        note?.takeIf { it.isNotBlank() }?.let { append("\n").append(it) }
                    }
                    copy(context, text)
                }
            }
        }
    }
}

private class Drawn(val bitmap: Bitmap?)

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("SalahSync", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}
