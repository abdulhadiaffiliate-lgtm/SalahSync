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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.salahsync.core.quran.Quran
import com.salahsync.quran.Translation
import com.salahsync.quran.Verse
import com.salahsync.quran.share.CardStyle
import com.salahsync.quran.share.VerseArt
import com.salahsync.quran.share.VerseCard
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.Salah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Turns a verse into an image on one of four backgrounds, then shares or copies it. */
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
    val c = Salah.colors
    val context = LocalContext.current
    var style by remember { mutableStateOf(CardStyle.EMERALD) }
    var withTranslation by remember { mutableStateOf(translations.keys.firstOrNull()) }
    val card = VerseCard(arabic, withTranslation?.let { translations[it] }, withTranslation?.rtl == true, reference)
    val image by produceState<Bitmap?>(null, card, style) {
        value = null
        value = withContext(Dispatchers.Default) { VerseArt.render(context, card, style) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier.padding(20.dp).fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.accentHairline, shape)
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Share verse", style = MaterialTheme.typography.titleLarge, color = c.text)
            Box(
                Modifier.fillMaxWidth().heightIn(min = 200.dp).clip(RoundedCornerShape(16.dp)).background(c.surfaceDeep),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = image
                if (bmp == null) {
                    CircularProgressIndicator(color = c.accent, modifier = Modifier.padding(48.dp))
                } else {
                    Image(bmp.asImageBitmap(), contentDescription = "Preview of the verse image", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
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

            if (translations.isNotEmpty()) {
                EyebrowText("Translation")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    translations.keys.forEach { t ->
                        Pill(t.label, null, filled = withTranslation == t) { withTranslation = t }
                    }
                    Pill("None", null, filled = withTranslation == null) { withTranslation = null }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Share image", SalahIcons.Share, filled = true, enabled = image != null) {
                    image?.let { VerseArt.share(context, it, "$reference\n\nShared from SalahSync") }
                }
                Pill("Copy text", SalahIcons.Copy) {
                    val text = buildString {
                        append(arabic)
                        withTranslation?.let { append("\n\n").append(translations[it]) }
                        append("\n\n").append(reference)
                    }
                    copy(context, text)
                }
            }
        }
    }
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Quran", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}
