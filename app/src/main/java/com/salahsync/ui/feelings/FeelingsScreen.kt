package com.salahsync.ui.feelings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahsync.core.adhkar.QuranPassage
import com.salahsync.core.duas.Dua
import com.salahsync.core.feelings.Feeling
import com.salahsync.core.feelings.Feelings
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.quran.Quran
import com.salahsync.core.quran.QuranText
import com.salahsync.hadith.HadithItem
import com.salahsync.hadith.HadithViewModel
import com.salahsync.quran.QuranViewModel
import com.salahsync.quran.Translation
import com.salahsync.quran.TranslationMode
import com.salahsync.quran.Verse
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GoldDivider
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.duas.DuaCard
import com.salahsync.ui.duas.DuaSourceDialog
import com.salahsync.ui.hadith.HadithCard
import com.salahsync.ui.hadith.HadithTopBar
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Cormorant
import com.salahsync.ui.theme.Salah
import com.salahsync.ui.theme.UrduStyle

/** "How are you feeling?": pick a feeling to open what the Quran and Sunnah say to it. */
@Composable
fun FeelingsScreen(onOpen: (Feeling) -> Unit) {
    val c = Salah.colors
    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EyebrowText("Comfort from the Quran and Sunnah")
            Text("How are you feeling?", style = MaterialTheme.typography.headlineLarge, color = c.text)
            Text(
                "Choose what is on your heart. Each feeling has ayat, duas and an authentic hadith, with their sources.",
                style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(top = 4.dp),
            )
        }
        Feelings.all.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { f -> FeelingTile(f, Modifier.weight(1f)) { onOpen(f) } }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FeelingTile(feeling: Feeling, modifier: Modifier, onClick: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.heightIn(min = 124.dp).clip(shape).background(c.surface).border(1.dp, c.hairline, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(SalahIcons.Heart, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        Text(feeling.name, style = MaterialTheme.typography.titleLarge, color = c.text)
        Text(feeling.comfort, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
}

/** The ayat, duas and hadith for one feeling. */
@Composable
fun FeelingScreen(
    feeling: Feeling,
    quran: QuranViewModel,
    hadith: HadithViewModel,
    onBack: () -> Unit,
    onOpenAyah: (AyahRef) -> Unit,
    onOpenHadith: (HadithItem) -> Unit,
) {
    val c = Salah.colors
    val quranPrefs by quran.prefs.collectAsStateWithLifecycle()
    val hadithPrefs by hadith.prefs.collectAsStateWithLifecycle()
    // Show the reader's translation; "Arabic only" still gets English here, as the meaning is the point.
    val mode = quranPrefs.mode.takeIf { it.translations.isNotEmpty() } ?: TranslationMode.ENGLISH
    val passages by produceState<List<List<Verse>>?>(null, feeling, mode) {
        value = feeling.ayat.map { p -> p.refs.map { quran.verse(it, mode) } }
    }
    val item by produceState<HadithItem?>(null, feeling) { value = hadith.hadith(feeling.hadith.book, feeling.hadith.ref) }
    var info by remember { mutableStateOf<Dua?>(null) }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(horizontal = 16.dp).padding(top = 72.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EyebrowText("When you feel", color = c.accent)
                Text(feeling.name, style = MaterialTheme.typography.headlineLarge, color = c.text)
                Text(
                    feeling.comfort, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 20.sp, lineHeight = 25.sp,
                    color = c.textMuted,
                )
            }

            Section("From the Quran")
            passages?.forEachIndexed { i, verses -> AyahCard(feeling.ayat[i], verses) { onOpenAyah(feeling.ayat[i].refs.first()) } }

            Section("Duas to say")
            feeling.duas.forEach { d -> DuaCard(d) { info = d } }

            Section("From the Sunnah")
            item?.let { h ->
                Text(feeling.hadith.topic, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.padding(horizontal = 4.dp))
                HadithCard(h, hadithPrefs, onClick = { onOpenHadith(h) })
            }

            Text(
                "Quran: Tanzil Uthmani text with ${mode.translations.joinToString(" and ") { it.translator }}. " +
                    "Duas and hadith are from Sahih al-Bukhari, Sahih Muslim, or sahih and hasan narrations, as graded on sunnah.com.",
                style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
        HadithTopBar("How are you feeling?", null, onBack)
    }

    info?.let { d -> DuaSourceDialog(d) { info = null } }
}

@Composable
private fun Section(title: String) {
    EyebrowText(title, color = Salah.colors.accent, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
}

/** One passage: its Arabic, translation and reference. Tap to open it in the reader. */
@Composable
private fun AyahCard(passage: QuranPassage, verses: List<Verse>, onClick: () -> Unit) {
    val c = Salah.colors
    val surah = Quran.surah(passage.surah)
    val ayahs = if (passage.ayahs.first == passage.ayahs.last) "${passage.ayahs.first}" else "${passage.ayahs.first}-${passage.ayahs.last}"
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.card)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            verses.joinToString(" ") { "${it.arabic} ${QuranText.ayahEnd(it.ref.ayah)}" },
            style = ArabicStyle.copy(fontSize = 24.sp, lineHeight = 46.sp), color = c.onCard, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        GoldDivider(color = c.onCardMuted)
        val translations = verses.firstOrNull()?.translations?.keys.orEmpty()
        translations.forEach { t ->
            val text = verses.joinToString(" ") { it.translations[t].orEmpty().replace(footnote, "").trim() }
            if (t == Translation.URDU) {
                Text(text, style = UrduStyle.copy(fontSize = 17.sp, lineHeight = 40.sp), color = c.onCard, modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    "“$text”", fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 19.sp, lineHeight = 25.sp,
                    color = c.onCard, textAlign = TextAlign.Center,
                )
            }
        }
        EyebrowText("${surah.name} ${passage.surah}:$ayahs", color = c.onCardMuted)
    }
}

private val footnote = Regex("""\[\d+]""")
