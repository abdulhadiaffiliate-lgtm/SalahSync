package com.salahsync.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.quran.Quran
import com.salahsync.core.quran.QuranText
import com.salahsync.core.quran.Surah
import com.salahsync.quran.Bookmark
import com.salahsync.quran.QuranViewModel
import com.salahsync.quran.audio.Offline
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GeometricPattern
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah

private enum class Index(val label: String) { SURAHS("Surahs"), JUZ("Juz"), BOOKMARKS("Bookmarks") }

/** The Quran tab: continue reading, the surah and juz index, bookmarks and the reciter. */
@Composable
fun QuranScreen(vm: QuranViewModel, onRead: (AyahRef) -> Unit) {
    val c = Salah.colors
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val downloaded by vm.downloaded.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Index.SURAHS) }
    var query by rememberSaveable { mutableStateOf("") }
    var pickReciter by remember { mutableStateOf(false) }
    val reciter = vm.reciter

    Box(Modifier.fillMaxSize().background(c.background)) {
        GeometricPattern(Modifier.fillMaxWidth().height(260.dp))
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        EyebrowText("The Noble Quran")
                        Text("Quran", style = MaterialTheme.typography.headlineLarge, color = c.text)
                    }
                    Text("القرآن الكريم", style = ArabicStyle.copy(fontSize = 24.sp, lineHeight = 40.sp), color = c.accent)
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                ContinueCard(prefs.lastRead, onRead, onListen = {
                    val ref = prefs.lastRead ?: AyahRef(1, 1)
                    vm.play(ref.surah, ref.ayah)
                })
            }
            item {
                val shape = RoundedCornerShape(18.dp)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape).border(1.dp, c.hairline, shape)
                        .clickable(role = Role.Button) { pickReciter = true }.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(SalahIcons.Headphones, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        EyebrowText("Reciter")
                        Text(reciter.name, style = MaterialTheme.typography.bodyLarge, color = c.text)
                    }
                    Icon(SalahIcons.ChevronRight, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                Segmented(Index.entries, tab, { it.label }) { tab = it }
            }
            when (tab) {
                Index.SURAHS -> {
                    item { SearchField(query) { query = it } }
                    val q = query.trim()
                    val list = if (q.isEmpty()) Quran.surahs else Quran.surahs.filter {
                        it.number.toString() == q || it.name.contains(q, true) || it.meaning.contains(q, true) || it.arabicName.contains(q)
                    }
                    items(list, key = { it.number }) { s ->
                        val key = vm.downloadKey(s.number)
                        val offline = if (key in downloaded) Offline.DOWNLOADED else vm.offline(s.number)
                        val isCurrent = playback.entry?.ref?.surah == s.number
                        SurahRow(
                            surah = s,
                            offline = offline,
                            downloading = key in downloads,
                            playing = isCurrent && playback.isPlaying,
                            onClick = { onRead(AyahRef(s.number, 1)) },
                            onPlay = {
                                if (isCurrent) vm.togglePlay() else vm.play(s.number)
                            },
                        )
                    }
                    if (list.isEmpty()) item { Empty("No surah matches “$q”.") }
                }
                Index.JUZ -> items((1..30).toList(), key = { "juz$it" }) { juz -> JuzRow(juz) { onRead(Quran.juzStarts[juz - 1]) } }
                Index.BOOKMARKS -> {
                    if (prefs.bookmarks.isEmpty()) {
                        item { Empty("Tap the bookmark on any ayah to keep it here.") }
                    }
                    items(prefs.bookmarks, key = { "b${it.ref}" }) { b ->
                        BookmarkRow(vm, b, onClick = { onRead(b.ref) }, onRemove = { vm.toggleBookmark(b.ref) })
                    }
                }
            }
        }
    }

    if (pickReciter) {
        ReciterDialog(reciter, onDismiss = { pickReciter = false }) {
            vm.setReciter(it)
            pickReciter = false
        }
    }
}

@Composable
private fun ContinueCard(lastRead: AyahRef?, onRead: (AyahRef) -> Unit, onListen: () -> Unit) {
    val c = Salah.colors
    val ref = lastRead ?: AyahRef(1, 1)
    val surah = Quran.surah(ref.surah)
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(c.surface, c.surfaceDeep)))
            .border(1.dp, c.accentHairline, shape)
            .clickable(role = Role.Button) { onRead(ref) }
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EyebrowText(if (lastRead != null) "Continue reading" else "Begin reading", color = c.accent)
                Text(surah.name, style = MaterialTheme.typography.displayMedium, color = c.text)
                Text(
                    if (lastRead != null) "Ayah ${ref.ayah} of ${surah.ayahCount} · Juz ${Quran.juzOf(ref)}" else "${surah.meaning} · ${surah.ayahCount} ayat",
                    style = MaterialTheme.typography.bodyMedium, color = c.textMuted,
                )
            }
            Text(surah.arabicName, style = ArabicStyle.copy(fontSize = 28.sp, lineHeight = 44.sp), color = c.accent)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill("Read", SalahIcons.Book, filled = true) { onRead(ref) }
            Pill("Listen", SalahIcons.Headphones, onClick = onListen)
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().height(48.dp).clip(shape).background(c.text.copy(alpha = 0.04f)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(SalahIcons.Search, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Search by name or number", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.text),
                cursorBrush = SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search surahs" },
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

@Composable
private fun SurahRow(
    surah: Surah,
    offline: Offline,
    downloading: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
) {
    val c = Salah.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StarBadge(surah.number)
        Column(Modifier.weight(1f)) {
            Text(surah.name, style = MaterialTheme.typography.titleMedium, color = if (playing) c.accent else c.text)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${surah.meaning} · ${surah.ayahCount} ayat",
                    style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                when {
                    downloading -> CircularProgressIndicator(Modifier.size(10.dp), color = c.accent, strokeWidth = 1.5.dp)
                    offline != Offline.NONE -> Icon(
                        SalahIcons.Download, contentDescription = "Available offline", tint = c.accent, modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        Text(surah.arabicName, style = ArabicStyle.copy(fontSize = 20.sp, lineHeight = 32.sp), color = c.accent)
        Box(
            Modifier.size(40.dp).clip(CircleShape).border(1.dp, c.accentHairline, CircleShape)
                .clickable(role = Role.Button, onClick = onPlay)
                .semantics { contentDescription = if (playing) "Pause ${surah.name}" else "Listen to ${surah.name}" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) SalahIcons.Pause else SalahIcons.Play, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun JuzRow(juz: Int, onClick: () -> Unit) {
    val c = Salah.colors
    val start = Quran.juzStarts[juz - 1]
    val surahs = Quran.surahsStartingIn(juz)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StarBadge(juz)
        Column(Modifier.weight(1f)) {
            Text("Juz $juz", style = MaterialTheme.typography.titleMedium, color = c.text)
            val more = if (surahs.isNotEmpty() && surahs.first().number != start.surah) " · then ${surahs.first().name}" else ""
            Text(
                "Starts at ${Quran.surah(start.surah).name} ${start.surah}:${start.ayah}$more",
                style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text("الجزء ${QuranText.arabicNumber(juz)}", style = ArabicStyle.copy(fontSize = 18.sp, lineHeight = 30.sp), color = c.accent)
    }
}

@Composable
private fun BookmarkRow(vm: QuranViewModel, bookmark: Bookmark, onClick: () -> Unit, onRemove: () -> Unit) {
    val c = Salah.colors
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val verse by produceState<com.salahsync.quran.Verse?>(null, bookmark.ref, prefs.mode) { value = vm.verse(bookmark.ref, prefs.mode) }
    val surah = Quran.surah(bookmark.ref.surah)
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).border(1.dp, c.hairline, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EyebrowText("${surah.name} · ${bookmark.ref}", color = c.accent, modifier = Modifier.weight(1f))
            Icon(
                SalahIcons.BookmarkFilled, contentDescription = "Remove bookmark", tint = c.accent,
                modifier = Modifier.size(20.dp).clickable(role = Role.Button, onClick = onRemove),
            )
        }
        verse?.let { v ->
            Text(
                v.arabic, style = ArabicStyle.copy(fontSize = 20.sp, lineHeight = 36.sp), color = c.text,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(),
            )
            v.translations.values.firstOrNull()?.let {
                Text(
                    translationText(it, c.accent), style = MaterialTheme.typography.bodyMedium, color = c.textMuted,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Empty(text: String) {
    val c = Salah.colors
    Text(text, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp))
}
