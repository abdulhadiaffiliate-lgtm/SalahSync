package com.salahsync.ui.duas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.core.duas.Dua
import com.salahsync.core.duas.DuaCategory
import com.salahsync.core.duas.Duas
import com.salahsync.core.quran.Quran
import com.salahsync.ui.components.ChoiceChips
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GoldDivider
import com.salahsync.ui.components.HadithReferenceCard
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah

/** Daily supplications by occasion; tap ⓘ on a card for its hadith source. */
@Composable
fun DuasScreen() {
    val c = Salah.colors
    var category by rememberSaveable { mutableStateOf(DuaCategory.entries.first()) }
    var info by remember { mutableStateOf<Dua?>(null) }

    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EyebrowText("${Duas.all.size} supplications from the Sunnah")
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Duas", style = MaterialTheme.typography.headlineLarge, color = c.text)
                Text("الأدعية", style = ArabicStyle.copy(fontSize = 20.sp), color = c.accent)
            }
        }
        ChoiceChips(
            DuaCategory.entries, category, { it.displayName }, { category = it },
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Every dua is from Sahih al-Bukhari, Sahih Muslim, or a sahih or hasan narration. Tap ⓘ for the source.",
                style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(horizontal = 4.dp),
            )
            Duas.inCategory(category).forEach { d -> DuaCard(d) { info = d } }
        }
    }

    info?.let { d -> DuaSourceDialog(d) { info = null } }
}

/** A dua with its Arabic, meaning and first reference; ⓘ calls [onInfo]. Also used on the Feelings screen. */
@Composable
fun DuaCard(dua: Dua, onInfo: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.hairline, shape)
            .padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                EyebrowText(dua.occasion, color = c.accent)
                Text(dua.title, style = MaterialTheme.typography.titleMedium, color = c.text)
            }
            IconButton(onClick = onInfo) {
                Icon(SalahIcons.Info, contentDescription = "Source of ${dua.title}", tint = c.accent, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(dua.arabic, style = ArabicStyle.copy(fontSize = 22.sp, lineHeight = 42.sp), color = c.text, modifier = Modifier.fillMaxWidth())
            Text(dua.meaning, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            val ref = dua.references.first()
            Text("${ref.book} ${ref.number}", style = MaterialTheme.typography.labelMedium, color = c.accent)
        }
    }
}

/** Every reference of a dua, with any matching ayah and note. */
@Composable
fun DuaSourceDialog(dua: Dua, onDismiss: () -> Unit) {
    val c = Salah.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(dua.title, style = MaterialTheme.typography.titleLarge, color = c.text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(dua.arabic, style = ArabicStyle.copy(fontSize = 20.sp, lineHeight = 38.sp), color = c.text, modifier = Modifier.fillMaxWidth())
                EyebrowText(dua.occasion)
                GoldDivider()
                EyebrowText(if (dua.references.size > 1) "References" else "Reference", color = c.accent)
                dua.references.forEach { HadithReferenceCard(it) }
                dua.ayah?.let { a ->
                    Text(
                        "The same words are in the Quran: ${Quran.surah(a.surah).name} ${a.surah}:${a.ayah}.",
                        style = MaterialTheme.typography.bodySmall, color = c.text,
                    )
                }
                dua.note?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = c.textMuted)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = c.accent) } },
    )
}
