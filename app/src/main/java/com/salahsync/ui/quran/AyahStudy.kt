package com.salahsync.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.quran.Quran
import com.salahsync.core.quran.QuranText
import com.salahsync.core.quran.QuranWord
import com.salahsync.quran.QuranViewModel
import com.salahsync.quran.Tafseer
import com.salahsync.quran.Translation
import com.salahsync.quran.TranslationMode
import com.salahsync.quran.WORD_BY_WORD_CREDIT
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah
import com.salahsync.ui.theme.UrduStyle

/**
 * One ayah to study, full screen: the Arabic, each word with its transliteration and
 * meaning, and the tafseer in the chosen language. The chip beside "Tafseer" switches
 * between English, Urdu and Arabic.
 */
@Composable
fun AyahStudy(vm: QuranViewModel, ref: AyahRef, tafseer: Tafseer, onTafseer: (Tafseer) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AyahStudyContent(vm, ref, tafseer, onTafseer, onDismiss)
    }
}

@Composable
private fun AyahStudyContent(vm: QuranViewModel, ref: AyahRef, tafseer: Tafseer, onTafseer: (Tafseer) -> Unit, onBack: () -> Unit) {
    val c = Salah.colors
    val arabic by produceState("", ref) { value = vm.verse(ref, TranslationMode.ARABIC).arabic }
    val words by produceState<List<QuranWord>?>(null, ref) { value = vm.words(ref) }
    // Loaded is Pair(tafseer, text) so a stale text never shows under a new language.
    val loaded by produceState<Pair<Tafseer, String?>?>(null, tafseer, ref) { value = tafseer to vm.tafseer(tafseer, ref) }
    val notes by produceState<String?>(null, ref) { value = vm.notes(Translation.ENGLISH, ref) }
    val cardShape = RoundedCornerShape(22.dp)

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(SalahIcons.ChevronLeft, contentDescription = "Back", tint = c.text, modifier = Modifier.size(20.dp)) }
            Text("${Quran.surah(ref.surah).name} $ref", style = MaterialTheme.typography.titleLarge, color = c.text)
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth().clip(cardShape).background(c.card).padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(
                    if (arabic.isEmpty()) "" else arabic + " " + QuranText.ayahEnd(ref.ayah),
                    style = ArabicStyle.copy(fontSize = 28.sp, lineHeight = 56.sp), color = c.onCard,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            words?.let { ws ->
                Text("Word by word", style = MaterialTheme.typography.headlineSmall, color = c.text, modifier = Modifier.padding(top = 12.dp))
                WordChips(ws)
                Text(WORD_BY_WORD_CREDIT, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }

            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Tafseer", style = MaterialTheme.typography.headlineSmall, color = c.text, modifier = Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(24.dp)).background(c.accent.copy(alpha = 0.14f))
                        .clickable(role = Role.Button, onClickLabel = "Show the ${tafseer.next.name.lowercase()} tafseer") { onTafseer(tafseer.next) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(tafseer.label, style = MaterialTheme.typography.titleMedium, color = c.accent)
                }
            }
            Text(
                tafseer.source, style = if (tafseer.rtl) ArabicStyle.copy(fontSize = 14.sp, lineHeight = 24.sp) else MaterialTheme.typography.bodyMedium,
                color = c.textMuted, modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.fillMaxWidth().clip(cardShape).background(c.surface).padding(20.dp)) {
                val shown = loaded?.first
                val body = loaded?.second?.takeIf { it.isNotBlank() }
                when {
                    shown != tafseer -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = c.accent)
                    }
                    body == null -> Text(
                        "Ahsan-ul-Bayan has no note on this ayah. Switch the language to read Al-Mukhtasar or Al-Muyassar.",
                        style = MaterialTheme.typography.bodyMedium, color = c.textMuted,
                    )
                    tafseer == Tafseer.URDU -> Text(body, style = UrduStyle.copy(fontSize = 17.sp, lineHeight = 40.sp), color = c.text, modifier = Modifier.fillMaxWidth())
                    tafseer.rtl -> Text(body, style = ArabicStyle.copy(fontSize = 19.sp, lineHeight = 38.sp), color = c.text, modifier = Modifier.fillMaxWidth())
                    else -> Text(body, style = MaterialTheme.typography.bodyLarge, color = c.text)
                }
            }
            Text(tafseer.credit, style = MaterialTheme.typography.bodySmall, color = c.textMuted)

            // Saheeh International's footnotes, the gold numbers in the English translation.
            notes?.let { text ->
                Text("Translator's notes", style = MaterialTheme.typography.headlineSmall, color = c.text, modifier = Modifier.padding(top = 16.dp))
                Text(Translation.ENGLISH.translator, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                Box(Modifier.fillMaxWidth().clip(cardShape).background(c.surface).padding(20.dp)) {
                    Text(text, style = MaterialTheme.typography.bodyLarge, color = c.text)
                }
            }
        }
    }
}

/** Each word as a chip: transliteration in gold, meaning underneath, in reading order. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordChips(words: List<QuranWord>) {
    val c = Salah.colors
    val shape = RoundedCornerShape(16.dp)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        words.forEach { w ->
            Column(
                Modifier.clip(shape).border(1.dp, c.accentHairline, shape).padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(w.transliteration.ifEmpty { w.arabic }, style = MaterialTheme.typography.titleMedium, color = c.accent, textAlign = TextAlign.Center)
                Text(w.gloss, style = MaterialTheme.typography.bodyMedium, color = c.text, textAlign = TextAlign.Center)
            }
        }
    }
}
