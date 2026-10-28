package com.salahsync.ui.hadith

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.hadith.HadithItem
import com.salahsync.hadith.HadithLanguage
import com.salahsync.hadith.HadithPrefs
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.quran.CardText
import com.salahsync.ui.quran.Segmented
import com.salahsync.ui.quran.ShareImageDialog
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah
import com.salahsync.ui.theme.UrduStyle

/** The translation to show, and whether it had to fall back to English. */
data class Shown(val text: String, val language: HadithLanguage, val fellBack: Boolean)

fun HadithItem.shown(language: HadithLanguage): Shown = when {
    language == HadithLanguage.URDU && urdu.isNotBlank() -> Shown(urdu, HadithLanguage.URDU, false)
    language == HadithLanguage.URDU -> Shown(english, HadithLanguage.ENGLISH, english.isNotBlank())
    english.isNotBlank() -> Shown(english, HadithLanguage.ENGLISH, false)
    else -> Shown(urdu, HadithLanguage.URDU, urdu.isNotBlank())
}

/** A translation in its own script: Nastaliq for Urdu, Manrope for English. */
@Composable
fun TranslationText(shown: Shown, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE, color: androidx.compose.ui.graphics.Color = Salah.colors.text) {
    val style = if (shown.language == HadithLanguage.URDU) UrduStyle.copy(fontSize = 17.sp, lineHeight = 40.sp)
    else MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp)
    Text(shown.text, style = style, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier.fillMaxWidth())
}

/** One hadith: its reference, Arabic, translation and any gradings. */
@Composable
fun HadithCard(
    item: HadithItem,
    prefs: HadithPrefs,
    modifier: Modifier = Modifier,
    showChapter: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    onClick: (() -> Unit)? = null,
) {
    val c = Salah.colors
    val context = LocalContext.current
    val shape = RoundedCornerShape(20.dp)
    val shown = item.shown(prefs.language)
    val preview = maxLines != Int.MAX_VALUE
    var sharing by remember { mutableStateOf(false) }
    if (sharing) ShareHadithDialog(item, prefs) { sharing = false }
    Column(
        modifier.fillMaxWidth().clip(shape).border(1.dp, c.hairline, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(top = 12.dp)) {
                EyebrowText(item.label, color = c.accent)
                if (showChapter && item.chapter.isNotBlank()) {
                    Text(item.chapter, style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (!preview) {
                IconButton(onClick = { copy(context, shareText(item, prefs)) }) {
                    Icon(SalahIcons.Copy, contentDescription = "Copy hadith", tint = c.textMuted, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { sharing = true }) {
                    Icon(SalahIcons.Share, contentDescription = "Share hadith", tint = c.textMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
        Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (prefs.showArabic && item.arabic.isNotBlank() && !preview) {
                Text(item.arabic, style = ArabicStyle.copy(fontSize = 21.sp, lineHeight = 40.sp), color = c.text, modifier = Modifier.fillMaxWidth())
            }
            if (shown.text.isNotBlank()) {
                TranslationText(shown, maxLines = maxLines, color = if (preview) c.textMuted else c.text)
            }
            if (shown.fellBack && !preview) {
                Text(
                    if (shown.language == HadithLanguage.ENGLISH) "The dataset has no Urdu translation of this hadith, so it is shown in English."
                    else "The dataset has no English translation of this hadith, so it is shown in Urdu.",
                    style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                )
            }
            if (!preview) Grades(item)
        }
    }
}

@Composable
private fun Grades(item: HadithItem) {
    val c = Salah.colors
    if (item.grades.isEmpty()) {
        if (item.book.isSahihCollection) EyebrowText("Sahih · in ${item.book.title}", color = c.textMuted)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        EyebrowText("Grading", color = c.textMuted)
        item.grades.forEach { g ->
            Text(
                if (g.scholar.isBlank()) g.grade else "${g.scholar}: ${g.grade}",
                style = MaterialTheme.typography.bodySmall, color = c.textMuted,
            )
        }
    }
}

private fun shareText(item: HadithItem, prefs: HadithPrefs): String = buildString {
    if (prefs.showArabic && item.arabic.isNotBlank()) append(item.arabic).append("\n\n")
    append(item.shown(prefs.language).text).append("\n\n").append(item.label)
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Hadith", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

/** Shares a hadith as an image: its Arabic, a translation, the reference and any gradings. */
@Composable
fun ShareHadithDialog(item: HadithItem, prefs: HadithPrefs, onDismiss: () -> Unit) {
    val english = CardText("English", item.english, rtl = false)
    val urdu = CardText("Urdu", item.urdu, rtl = true)
    val ordered = if (prefs.language == HadithLanguage.URDU) listOf(urdu, english) else listOf(english, urdu)
    ShareImageDialog(
        title = "Share hadith",
        arabic = item.arabic,
        translations = ordered.filter { it.text.isNotBlank() },
        reference = item.label,
        onDismiss = onDismiss,
        note = gradingNote(item),
        arabicOptional = true,
        showArabicInitially = prefs.showArabic,
    )
}

/** The gradings as the dataset gives them; Bukhari and Muslim need none, their titles say sahih. */
private fun gradingNote(item: HadithItem): String? =
    item.grades.takeIf { it.isNotEmpty() }?.joinToString(" · ") { g -> if (g.scholar.isBlank()) g.grade else "${g.scholar}: ${g.grade}" }

/** English or Urdu, and whether to show the Arabic. */
@Composable
fun LanguageControls(prefs: HadithPrefs, onLanguage: (HadithLanguage) -> Unit, onShowArabic: (Boolean) -> Unit) {
    val c = Salah.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Segmented(HadithLanguage.entries, prefs.language, { it.label }, onSelect = onLanguage)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Switch) { onShowArabic(!prefs.showArabic) }.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Show Arabic text", style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
            Switch(
                checked = prefs.showArabic, onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = c.onAccent, checkedTrackColor = c.accent,
                    uncheckedThumbColor = c.textMuted, uncheckedTrackColor = c.hairline, uncheckedBorderColor = c.hairline,
                ),
            )
        }
    }
}

@Composable
fun HadithSearchField(value: String, onChange: (String) -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().height(48.dp).clip(shape).background(c.text.copy(alpha = 0.04f)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(SalahIcons.Search, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Search by number or words", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.text),
                cursorBrush = SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search hadith" },
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                SalahIcons.Close, contentDescription = "Clear search", tint = c.textMuted,
                modifier = Modifier.size(18.dp).clickable(role = Role.Button) { onChange("") },
            )
        }
    }
}

/** A back button and title over a fade, as in the Quran reader. */
@Composable
fun HadithTopBar(title: String, subtitle: String?, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    val c = Salah.colors
    Row(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(c.background, c.background, c.background.copy(alpha = 0f))))
            .statusBarsPadding().height(64.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(SalahIcons.ChevronLeft, contentDescription = "Back", tint = c.text, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        action?.invoke()
    }
}

fun megabytes(bytes: Long): String = "${(bytes + 500_000) / 1_000_000} MB"
