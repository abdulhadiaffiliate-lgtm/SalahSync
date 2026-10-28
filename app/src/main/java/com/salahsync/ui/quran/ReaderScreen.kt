package com.salahsync.ui.quran

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.quran.Quran
import com.salahsync.core.quran.QuranText
import com.salahsync.core.quran.QuranWord
import com.salahsync.quran.QuranViewModel
import com.salahsync.quran.Verse
import com.salahsync.quran.WORD_BY_WORD_CREDIT
import com.salahsync.quran.audio.Offline
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GeometricPattern
import com.salahsync.ui.components.GoldDivider
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah
import com.salahsync.ui.theme.UrduStyle
import com.salahsync.ui.theme.quranStyle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** One surah, ayah by ayah, with translations, audio, bookmarks and sharing. */
@OptIn(FlowPreview::class)
@Composable
fun ReaderScreen(
    vm: QuranViewModel,
    surahNumber: Int,
    startAyah: Int,
    onBack: () -> Unit,
    onOpenSurah: (AyahRef) -> Unit,
) {
    val c = Salah.colors
    val surah = Quran.surah(surahNumber)
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val downloaded by vm.downloaded.collectAsStateWithLifecycle()
    val verses by produceState<List<Verse>?>(null, surahNumber, prefs.mode, prefs.wordByWord) {
        value = vm.surah(surahNumber, prefs.mode, prefs.wordByWord)
    }
    val basmala by produceState("") { value = vm.basmala() }
    val list = rememberLazyListState()
    var showDisplay by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf<Verse?>(null) }
    var explaining by remember { mutableStateOf<AyahRef?>(null) }
    var positioned by rememberSaveable(surahNumber) { mutableStateOf(false) }

    val playingHere = playback.entry?.ref?.surah == surahNumber
    val playingAyah = if (playingHere) playback.entry?.ref?.ayah else null

    // Open at the requested ayah once the text is in.
    LaunchedEffect(verses != null) {
        if (verses != null && !positioned) {
            if (startAyah > 1) list.scrollToItem(startAyah)
            positioned = true
        }
    }
    // Remember the ayah at the top of the screen as the place to continue from.
    LaunchedEffect(surahNumber, verses != null) {
        if (verses == null) return@LaunchedEffect
        snapshotFlow { list.firstVisibleItemIndex }
            .filter { positioned }
            .distinctUntilChanged()
            .debounce(800)
            .collect { index -> vm.setLastRead(AyahRef(surahNumber, index.coerceIn(1, surah.ayahCount))) }
    }
    // Keep the ayah being recited in view.
    LaunchedEffect(playingAyah, playback.isPlaying) {
        val ayah = playingAyah ?: return@LaunchedEffect
        if (!playback.isPlaying || verses == null) return@LaunchedEffect
        val visible = list.layoutInfo.visibleItemsInfo.map { it.index }
        if (ayah !in visible.dropLast(1)) list.animateScrollToItem(ayah)
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(top = 64.dp, bottom = 140.dp),
        ) {
            item(key = "header") {
                val key = vm.downloadKey(surahNumber)
                val offline = if (key in downloaded) Offline.DOWNLOADED else vm.offline(surahNumber)
                SurahHeader(
                    basmala = basmala,
                    playing = playingHere && playback.isPlaying,
                    offline = offline,
                    progress = downloads[key]?.let { if (it.failed) -1f else it.fraction },
                    onListen = { if (playingHere) vm.togglePlay() else vm.play(surahNumber) },
                    onDownload = {
                        when {
                            key in downloads && downloads[key]?.failed != true -> vm.cancelDownload(surahNumber)
                            offline == Offline.NONE -> vm.download(surahNumber)
                        }
                    },
                    surahNumber = surahNumber,
                )
            }
            val vs = verses
            if (vs == null) {
                item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.accent) } }
            } else {
                itemsIndexed(vs, key = { _, v -> v.ref.ayah }) { _, v ->
                    AyahItem(
                        vm = vm,
                        verse = v,
                        arabicSize = prefs.arabicSize,
                        recited = v.ref.ayah == playingAyah,
                        reciting = v.ref.ayah == playingAyah && playback.isPlaying,
                        bookmarked = prefs.isBookmarked(v.ref),
                        onPlay = { if (v.ref.ayah == playingAyah) vm.togglePlay() else vm.playAyah(v.ref) },
                        onBookmark = { vm.toggleBookmark(v.ref) },
                        onShare = { sharing = v },
                        onTafseer = { explaining = v.ref },
                    )
                }
                item(key = "footer") {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        GoldDivider(Modifier.padding(vertical = 16.dp))
                        if (prefs.wordByWord) {
                            Text(
                                WORD_BY_WORD_CREDIT, style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 16.dp),
                            )
                        }
                        if (surahNumber < Quran.surahs.size) {
                            val next = Quran.surah(surahNumber + 1)
                            Pill("Next: ${next.name}", SalahIcons.ChevronRight) { onOpenSurah(AyahRef(next.number, 1)) }
                        } else {
                            Text("صَدَقَ ٱللَّهُ ٱلْعَظِيمُ", style = ArabicStyle, color = c.accent)
                        }
                    }
                }
            }
        }

        // Top bar over a fade, so text scrolls softly underneath.
        Row(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(c.background, c.background, c.background.copy(alpha = 0f))))
                .statusBarsPadding().height(64.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(SalahIcons.ChevronLeft, contentDescription = "Back", tint = c.text, modifier = Modifier.size(20.dp)) }
            Column(Modifier.weight(1f)) {
                Text(surah.name, style = MaterialTheme.typography.titleMedium, color = c.text)
                Text("${surah.meaning} · ${surah.revelation}", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
            IconButton(onClick = { showDisplay = true }) {
                Icon(SalahIcons.TextSize, contentDescription = "Translation and text size", tint = c.accent, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showDisplay) DisplayDialog(prefs, vm::setMode, vm::setArabicSize, vm::setWordByWord) { showDisplay = false }
    explaining?.let { ref -> AyahStudy(vm, ref, prefs.tafseer, vm::setTafseer) { explaining = null } }
    sharing?.let { v -> ShareVerseDialog(v, onDismiss = { sharing = null }) }
}

@Composable
private fun SurahHeader(
    surahNumber: Int,
    basmala: String,
    playing: Boolean,
    offline: Offline,
    progress: Float?,
    onListen: () -> Unit,
    onDownload: () -> Unit,
) {
    val c = Salah.colors
    val surah = Quran.surah(surahNumber)
    val shape = RoundedCornerShape(28.dp)
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)) {
        Box(
            Modifier.fillMaxWidth().clip(shape).background(Brush.linearGradient(listOf(c.surface, c.surfaceDeep)))
                .border(1.dp, c.accentHairline, shape),
        ) {
            GeometricPattern(Modifier.matchParentSize(), alpha = 0.06f)
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                EyebrowText("Surah ${surah.number} · ${surah.ayahCount} ayat · Juz ${Quran.juzOf(AyahRef(surah.number, 1))}")
                Text(surah.arabicName, style = ArabicStyle.copy(fontSize = 40.sp, lineHeight = 64.sp), color = c.accent)
                Text(surah.meaning, style = MaterialTheme.typography.titleMedium, color = c.text)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill(if (playing) "Pause" else "Listen", if (playing) SalahIcons.Pause else SalahIcons.Play, filled = true, onClick = onListen)
                    val label = when {
                        progress != null && progress < 0f -> "Retry download"
                        progress != null -> "Downloading ${(progress * 100).toInt()}%"
                        offline == Offline.BUNDLED -> "Included offline"
                        offline == Offline.DOWNLOADED -> "Downloaded"
                        else -> "Download"
                    }
                    Pill(label, SalahIcons.Download, enabled = offline == Offline.NONE || progress != null, onClick = onDownload)
                }
            }
        }
        if (Quran.opensWithBasmala(surahNumber) && basmala.isNotEmpty()) {
            Text(
                basmala, style = ArabicStyle.copy(fontSize = 26.sp, lineHeight = 52.sp), color = c.text,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
        }
    }
}

@Composable
private fun AyahItem(
    vm: QuranViewModel,
    verse: Verse,
    arabicSize: Int,
    recited: Boolean,
    reciting: Boolean,
    bookmarked: Boolean,
    onPlay: () -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onTafseer: () -> Unit,
) {
    val c = Salah.colors
    val bg by animateColorAsState(if (recited) c.accent.copy(alpha = 0.09f) else Color.Transparent, tween(350), label = "recited")
    val shape = RoundedCornerShape(22.dp)

    Column(
        Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth().clip(shape).background(bg)
            .then(if (recited) Modifier.border(1.dp, c.accentHairline, shape) else Modifier)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarBadge(verse.ref.ayah, size = 34.dp)
            if (Quran.isSajdah(verse.ref)) {
                EyebrowText("Sajdah", color = c.accent, modifier = Modifier.padding(start = 10.dp))
            }
            Spacer(Modifier.weight(1f))
            TafseerButton(verse.ref, onTafseer)
            ActionIcon(SalahIcons.Share, "Share ayah ${verse.ref}", false, onShare)
            ActionIcon(if (bookmarked) SalahIcons.BookmarkFilled else SalahIcons.Bookmark, if (bookmarked) "Remove bookmark" else "Bookmark", bookmarked, onBookmark)
            ActionIcon(if (reciting) SalahIcons.Pause else SalahIcons.PlayLine, "Play from ayah ${verse.ref.ayah}", recited, onPlay)
        }
        val words = verse.words
        if (words != null) {
            WordByWordText(words, verse.ref.ayah, arabicSize)
        } else {
            Text(
                verse.arabic + " " + QuranText.ayahEnd(verse.ref.ayah),
                style = quranStyle(arabicSize),
                color = c.text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        verse.translations.forEach { (t, text) ->
            if (t.rtl) {
                Text(translationText(text, c.accent), style = UrduStyle, color = c.textMuted, modifier = Modifier.fillMaxWidth())
            } else {
                Text(translationText(text, c.accent), style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            }
        }
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    val c = Salah.colors
    // A soft gold disc, filled solid when active (bookmarked or reciting), so each action stands out.
    Box(
        Modifier.size(44.dp).padding(3.dp).clip(RoundedCornerShape(19.dp))
            .background(if (active) c.accent else c.accent.copy(alpha = 0.13f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (active) c.onAccent else c.accent, modifier = Modifier.size(19.dp))
    }
}

/** Tafseer is the ayah's main way in, so it gets a label as well as an icon. */
@Composable
private fun TafseerButton(ref: AyahRef, onClick: () -> Unit) {
    val c = Salah.colors
    Row(
        Modifier.padding(end = 3.dp).height(38.dp).clip(RoundedCornerShape(19.dp)).background(c.accent.copy(alpha = 0.13f))
            .clickable(role = Role.Button, onClickLabel = "Open the tafseer of ayah $ref", onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(SalahIcons.Book, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
        Text("Tafseer", style = MaterialTheme.typography.labelLarge, color = c.accent)
    }
}

/** The ayah as a right-to-left flow of words, each with its English gloss underneath. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordByWordText(words: List<QuranWord>, ayah: Int, arabicSize: Int) {
    val c = Salah.colors
    val arabic = quranStyle(arabicSize).copy(lineHeight = (arabicSize * 1.6f).sp)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            words.forEach { w ->
                Column(Modifier.widthIn(max = 160.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(w.arabic, style = arabic, color = c.text)
                    Text(w.gloss, style = MaterialTheme.typography.bodySmall, color = c.textMuted, textAlign = TextAlign.Center)
                }
            }
            Text(QuranText.ayahEnd(ayah), style = arabic, color = c.accent)
        }
    }
}
