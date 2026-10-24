package com.salahsync.ui.hadith

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahsync.core.hadith.HadithBook
import com.salahsync.core.hadith.HadithNumber
import com.salahsync.core.hadith.HadithQuery
import com.salahsync.core.hadith.HadithSource
import com.salahsync.hadith.Chapter
import com.salahsync.hadith.HadithDownload
import com.salahsync.hadith.HadithItem
import com.salahsync.hadith.HadithViewModel
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GeometricPattern
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.quran.Pill
import com.salahsync.ui.quran.StarBadge
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah
import java.time.LocalDate

/** The Hadith section: the hadith of the day, search, and the four books. */
@Composable
fun HadithScreen(vm: HadithViewModel, onOpenBook: (HadithBook) -> Unit, onOpenHadith: (HadithItem) -> Unit) {
    val c = Salah.colors
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val installed by vm.installed.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val daily by vm.daily.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val ofTheDay = daily.takeIf { it.isNotEmpty() }?.let { vm.dailyFor(today) }

    Box(Modifier.fillMaxSize().background(c.background)) {
        GeometricPattern(Modifier.fillMaxWidth().height(260.dp))
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        EyebrowText("Sayings of the Prophet ﷺ")
                        Text("Hadith", style = MaterialTheme.typography.headlineLarge, color = c.text)
                    }
                    Text("الحديث النبوي", style = ArabicStyle.copy(fontSize = 22.sp, lineHeight = 38.sp), color = c.accent)
                }
            }
            ofTheDay?.let { h ->
                item(key = "daily") {
                    Spacer(Modifier.height(4.dp))
                    DailyHadithCard(h, vm.topicOf(h)?.topic, prefs.language) { onOpenHadith(h) }
                }
            }
            item(key = "language") { LanguageControls(prefs, { vm.setLanguage(it) }, { vm.setShowArabic(it) }) }
            item(key = "search") { HadithSearchField(query, vm::setQuery) }

            val s = search
            if (query.isNotBlank()) {
                when {
                    installed.isEmpty() -> item { Note("Download a book below to search it. Search works offline once a book is downloaded.") }
                    s == null || s.text != query -> item { Loading() }
                    s.query == null -> item { Note("Type a hadith number, such as 13 or Muslim 2564c, or at least two letters of a word.") }
                    s.results.isEmpty() -> item { Note(noResults(s.query, installed)) }
                    else -> {
                        item { EyebrowText(resultsLabel(s.results.size, installed), modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
                        items(s.results, key = { "r${it.book}:${it.ref}" }) { h ->
                            HadithCard(h, prefs, showChapter = true, maxLines = 4) { onOpenHadith(h) }
                        }
                    }
                }
            } else {
                item { EyebrowText("Books", modifier = Modifier.padding(start = 4.dp, top = 8.dp)) }
                items(HadithBook.entries, key = { it.slug }) { b ->
                    BookRow(
                        book = b,
                        installed = b in installed,
                        download = downloads[b],
                        onOpen = { onOpenBook(b) },
                        onDownload = { vm.download(b) },
                        onCancel = { vm.cancelDownload(b) },
                    )
                }
                item {
                    Text(
                        "Each book downloads once in Arabic, English and Urdu, then works offline. The text comes unchanged from the open hadith-api dataset; see About for sources.",
                        style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private fun resultsLabel(count: Int, installed: Set<HadithBook>): String {
    val where = if (installed.size == HadithBook.entries.size) "all four books" else installed.sortedBy { it.ordinal }.joinToString { it.shortTitle }
    return if (count >= 100) "First 100 results in $where" else "$count found in $where"
}

private fun noResults(q: HadithQuery, installed: Set<HadithBook>): String = when (q) {
    is HadithQuery.Number -> {
        val book = q.book
        if (book != null && book !in installed) "Download ${book.title} to look up its hadith."
        else "No hadith numbered ${q.ref} in your downloaded books."
    }
    is HadithQuery.Words -> "No hadith in your downloaded books has all of those words."
}

@Composable
fun DailyHadithCard(item: HadithItem, topic: String?, language: com.salahsync.hadith.HadithLanguage, onClick: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(c.surface, c.surfaceDeep)))
            .border(1.dp, c.accentHairline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EyebrowText("Hadith of the Day", color = c.accent)
        if (topic != null) Text(topic, style = MaterialTheme.typography.titleLarge, color = c.text)
        TranslationText(item.shown(language), maxLines = 6, color = c.textMuted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            EyebrowText(item.label, modifier = Modifier.weight(1f))
            Text("Read", style = MaterialTheme.typography.labelLarge, color = c.accent)
            Icon(SalahIcons.ChevronRight, contentDescription = null, tint = c.accent, modifier = Modifier.padding(start = 4.dp).size(14.dp))
        }
    }
}

@Composable
private fun BookRow(
    book: HadithBook,
    installed: Boolean,
    download: HadithDownload?,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    val c = Salah.colors
    val shape = RoundedCornerShape(20.dp)
    val busy = download != null && !download.failed
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).clip(shape).border(1.dp, c.hairline, shape)
            .clickable(role = Role.Button, onClick = onOpen).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, color = c.text)
            val status = when {
                installed -> "Downloaded · works offline"
                download?.failed == true -> "Download failed. Tap the arrow to try again."
                download?.preparing == true -> "Preparing for offline reading…"
                download != null -> "Downloading ${(download.fraction * 100).toInt()}%"
                else -> "${book.compiler} · ${megabytes(HadithSource.bytes(book))} download"
            }
            Text(status, style = MaterialTheme.typography.bodySmall, color = if (download?.failed == true) c.accent else c.textMuted)
        }
        Text(book.arabicTitle, style = ArabicStyle.copy(fontSize = 17.sp, lineHeight = 30.sp), color = c.accent)
        when {
            installed -> Icon(SalahIcons.ChevronRight, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
            busy -> Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                if (download?.preparing == true) CircularProgressIndicator(Modifier.size(22.dp), color = c.accent, strokeWidth = 2.dp)
                else CircularProgressIndicator(progress = { download?.fraction ?: 0f }, modifier = Modifier.size(22.dp), color = c.accent, strokeWidth = 2.dp, trackColor = c.hairline)
                Icon(SalahIcons.Close, contentDescription = "Cancel download", tint = c.accent, modifier = Modifier.size(10.dp))
            }
            else -> Box(
                Modifier.size(40.dp).clip(CircleShape).border(1.dp, c.accentHairline, CircleShape).clickable(role = Role.Button, onClick = onDownload),
                contentAlignment = Alignment.Center,
            ) {
                Icon(SalahIcons.Download, contentDescription = "Download ${book.title}", tint = c.accent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** A book's chapters, or the offer to download it. */
@Composable
fun HadithBookScreen(vm: HadithViewModel, book: HadithBook, onBack: () -> Unit, onOpenChapter: (Int) -> Unit) {
    val c = Salah.colors
    val installed by vm.installed.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val isInstalled = book in installed
    val chapters by produceState<List<Chapter>?>(null, book, isInstalled) { value = if (isInstalled) vm.chapters(book) else emptyList() }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 72.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(book.arabicTitle, style = ArabicStyle.copy(fontSize = 30.sp, lineHeight = 52.sp), color = c.accent)
                    Text(book.compiler, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                }
            }
            val list = chapters
            when {
                !isInstalled -> item {
                    val d = downloads[book]
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).border(1.dp, c.accentHairline, RoundedCornerShape(24.dp)).padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Download to read offline", style = MaterialTheme.typography.titleLarge, color = c.text)
                        Text(
                            "${book.title} in Arabic, English and Urdu is about ${megabytes(HadithSource.bytes(book))} " +
                                "(less over the network, as it is compressed). After that it needs no connection.",
                            style = MaterialTheme.typography.bodyMedium, color = c.textMuted,
                        )
                        when {
                            d == null || d.failed -> {
                                if (d?.failed == true) Text("The download did not finish. Check your connection and try again.", style = MaterialTheme.typography.bodySmall, color = c.accent)
                                Pill("Download", SalahIcons.Download, filled = true) { vm.download(book) }
                            }
                            else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = c.accent, strokeWidth = 2.dp)
                                Text(
                                    if (d.preparing) "Preparing…" else "Downloading ${(d.fraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f),
                                )
                                Pill("Cancel", null) { vm.cancelDownload(book) }
                            }
                        }
                    }
                }
                list == null -> item { Loading() }
                else -> items(list, key = { it.number }) { ch -> ChapterRow(ch) { onOpenChapter(ch.number) } }
            }
        }
        HadithTopBar(book.title, if (isInstalled) chapters?.let { "${it.size} books · ${it.sumOf { ch -> ch.count }} hadith" } else null, onBack) {
            if (isInstalled) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(SalahIcons.Trash, contentDescription = "Remove download", tint = c.textMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = c.surface,
            title = { Text("Remove ${book.title}?", style = MaterialTheme.typography.titleLarge, color = c.text) },
            text = { Text("This frees about ${megabytes(HadithSource.bytes(book))}. You can download it again at any time.", color = c.textMuted) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(book); onBack() }) { Text("Remove", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep", color = c.textMuted) } },
        )
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, onClick: () -> Unit) {
    val c = Salah.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StarBadge(chapter.number)
        Column(Modifier.weight(1f)) {
            Text(chapter.title, style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val range = if (chapter.firstRef == chapter.lastRef) HadithNumber.label(chapter.firstRef)
            else "${HadithNumber.label(chapter.firstRef)} to ${HadithNumber.label(chapter.lastRef)}"
            Text(
                "${chapter.count} hadith · $range",
                style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Every hadith in one chapter. */
@Composable
fun HadithChapterScreen(vm: HadithViewModel, book: HadithBook, chapter: Int, onBack: () -> Unit) {
    val c = Salah.colors
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val title by produceState("", book, chapter) { value = vm.chapterTitle(book, chapter) }
    val hadith by produceState<List<HadithItem>?>(null, book, chapter) { value = vm.inChapter(book, chapter) }
    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 72.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val list = hadith
            if (list == null) item { Loading() } else items(list, key = { it.ref }) { HadithCard(it, prefs) }
        }
        HadithTopBar(title, book.title, onBack)
    }
}

/** One hadith on its own, opened from search, the daily card or Home. */
@Composable
fun HadithDetailScreen(vm: HadithViewModel, book: HadithBook, ref: String, onBack: () -> Unit, onOpenBook: (HadithBook) -> Unit) {
    val c = Salah.colors
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val installed by vm.installed.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    val loaded by produceState<Pair<Boolean, HadithItem?>>(false to null, book, ref, installed) { value = true to vm.hadith(book, ref) }
    val hadith = loaded.second
    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 72.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                !loaded.first -> item { Loading() }
                hadith == null -> item { Note("This hadith is not on your phone. Download ${book.title} to read it.") }
                else -> {
                    vm.topicOf(hadith)?.let { pick ->
                        item { Text(pick.topic, style = MaterialTheme.typography.headlineMedium, color = c.text, modifier = Modifier.padding(horizontal = 4.dp)) }
                    }
                    item { HadithCard(hadith, prefs, showChapter = true) }
                    item { LanguageControls(prefs, { vm.setLanguage(it) }, { vm.setShowArabic(it) }) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HadithNumber.sunnahUrl(book, ref)?.let { url -> Pill("View on sunnah.com", null) { uri.openUri(url) } }
                            if (book in installed) Pill("Browse ${book.shortTitle}", SalahIcons.Book) { onOpenBook(book) }
                        }
                    }
                }
            }
        }
        HadithTopBar(hadith?.label ?: book.title, hadith?.chapter?.ifBlank { null }, onBack)
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Salah.colors.accent) }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Salah.colors.textMuted, modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp))
}
