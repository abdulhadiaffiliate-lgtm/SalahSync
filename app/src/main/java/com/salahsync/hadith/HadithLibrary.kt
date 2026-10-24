package com.salahsync.hadith

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import androidx.room.withTransaction
import com.salahsync.core.hadith.ChapterAssigner
import com.salahsync.core.hadith.DailyHadith
import com.salahsync.core.hadith.Grade
import com.salahsync.core.hadith.HadithBook
import com.salahsync.core.hadith.HadithEdition
import com.salahsync.core.hadith.HadithNumber
import com.salahsync.core.hadith.HadithPick
import com.salahsync.core.hadith.HadithQuery
import com.salahsync.core.hadith.HadithSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** A hadith ready to show, from a downloaded book or the bundled daily set. */
data class HadithItem(
    val book: HadithBook,
    val ref: String,
    val chapter: String,
    val arabic: String,
    val english: String,
    val urdu: String,
    val grades: List<Grade>,
) {
    val label: String get() = "${book.title} ${HadithNumber.label(ref)}"
}

data class Chapter(val number: Int, val title: String, val count: Int, val firstRef: String, val lastRef: String)

/** A book download: fetching the three editions, then building the local index. */
data class HadithDownload(val fraction: Float, val preparing: Boolean = false, val failed: Boolean = false)

/**
 * The hadith books: the bundled daily hadith, and the four books, downloaded one at a time
 * from the pinned dataset into [HadithDatabase] so they then work offline.
 */
class HadithLibrary(private val context: Context) {
    private val db = HadithDatabase.open(context)
    private val dao = db.dao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<HadithBook, Job>()

    val installed: Flow<Set<HadithBook>> = dao.installed().map { rows -> rows.mapNotNull(HadithBook::bySlug).toSet() }

    private val _downloads = MutableStateFlow<Map<HadithBook, HadithDownload>>(emptyMap())
    val downloads: StateFlow<Map<HadithBook, HadithDownload>> = _downloads.asStateFlow()

    // The daily set ---------------------------------------------------------------------

    private var dailyCache: List<HadithItem>? = null

    /** The 20 daily hadith, in [DailyHadith.picks] order. */
    suspend fun daily(): List<HadithItem> = dailyCache ?: withContext(Dispatchers.IO) {
        val json = JSONObject(context.assets.open("hadith/daily.json").bufferedReader().use { it.readText() })
        val list = json.getJSONArray("hadith")
        (0 until list.length()).map { i ->
            val o = list.getJSONObject(i)
            HadithItem(
                book = HadithBook.bySlug(o.getString("book")) ?: error("Unknown book in daily.json"),
                ref = o.getString("ref"),
                chapter = o.optString("chapter"),
                arabic = o.getString("arabic"),
                english = o.getString("english"),
                urdu = o.getString("urdu"),
                grades = emptyList(),
            )
        }.also { dailyCache = it }
    }

    fun topicOf(item: HadithItem): HadithPick? = DailyHadith.picks.firstOrNull { it.book == item.book && it.ref == item.ref }

    // Reading ---------------------------------------------------------------------------

    suspend fun chapters(book: HadithBook): List<Chapter> {
        val summaries = dao.chapters(book.slug)
        val refs = summaries.flatMap { listOf(it.firstSeq, it.lastSeq) }.distinct().chunked(500)
            .flatMap { dao.refsAt(book.slug, it) }.associate { it.seq to it.ref }
        return summaries.map { s ->
            Chapter(
                number = s.number,
                title = s.title.trim().ifEmpty { "Chapter ${s.number}" },
                count = s.count,
                firstRef = refs[s.firstSeq].orEmpty(),
                lastRef = refs[s.lastSeq].orEmpty(),
            )
        }
    }

    suspend fun chapterTitle(book: HadithBook, number: Int): String =
        dao.chapterTitle(book.slug, number)?.trim()?.ifEmpty { null } ?: "Chapter $number"

    suspend fun inChapter(book: HadithBook, chapter: Int): List<HadithItem> {
        val title = chapterTitle(book, chapter)
        return dao.inChapter(book.slug, chapter).map { it.toItem(title) }
    }

    /** From the downloaded book if there is one, otherwise from the daily set. */
    suspend fun hadith(book: HadithBook, ref: String): HadithItem? {
        dao.byRef(book.slug, ref)?.let { return it.toItem(chapterTitle(book, it.chapter)) }
        return daily().firstOrNull { it.book == book && it.ref == ref }
    }

    /** Searches the downloaded books, Bukhari first. */
    suspend fun search(query: HadithQuery): List<HadithItem> {
        val rows = when (query) {
            is HadithQuery.Number -> dao.byNumber(query.ref, SEARCH_LIMIT).filter { query.book == null || it.book == query.book.slug }
            is HadithQuery.Words -> dao.matching(query.match, SEARCH_LIMIT)
        }
        val titles = mutableMapOf<Pair<String, Int>, String>()
        return rows.sortedWith(compareBy({ HadithBook.bySlug(it.book)?.ordinal ?: 99 }, { it.seq })).mapNotNull { row ->
            val book = HadithBook.bySlug(row.book) ?: return@mapNotNull null
            val title = titles.getOrPut(row.book to row.chapter) { chapterTitle(book, row.chapter) }
            row.toItem(title)
        }
    }

    private fun HadithRow.toItem(chapterTitle: String) = HadithItem(
        book = HadithBook.bySlug(book) ?: HadithBook.BUKHARI,
        ref = ref,
        chapter = chapterTitle,
        arabic = arabic,
        english = english,
        urdu = urdu,
        grades = grades.lines().filter { '\t' in it }.map { Grade(it.substringBefore('\t'), it.substringAfter('\t')) },
    )

    // Downloading -----------------------------------------------------------------------

    fun download(book: HadithBook) {
        if (jobs[book]?.isActive == true) return
        _downloads.update { it + (book to HadithDownload(0f)) }
        jobs[book] = scope.launch {
            try {
                val total = HadithSource.bytes(book).toFloat()
                var before = 0L
                val files = HadithEdition.entries.associateWith { edition ->
                    fetch(book, edition) { done ->
                        _downloads.update { it + (book to HadithDownload(((before + done) / total).coerceAtMost(1f))) }
                    }.also { before += HadithSource.bytes(book, edition) }
                }
                _downloads.update { it + (book to HadithDownload(1f, preparing = true)) }
                import(book, files)
                files.values.forEach { it.delete() }
                _downloads.update { it - book }
            } catch (e: CancellationException) {
                _downloads.update { it - book }
                throw e
            } catch (e: Exception) {
                // IOException from the network, or a malformed file; either way nothing is kept.
                _downloads.update { it + (book to HadithDownload(0f, failed = true)) }
            }
        }
    }

    fun cancel(book: HadithBook) {
        jobs.remove(book)?.cancel()
        _downloads.update { it - book }
    }

    fun delete(book: HadithBook) {
        cancel(book)
        scope.launch { db.withTransaction { clear(book) } }
    }

    private suspend fun clear(book: HadithBook) {
        dao.deleteInstalled(book.slug)
        dao.deleteHadith(book.slug)
        dao.deleteChapters(book.slug)
    }

    private fun cacheDir() = File(context.cacheDir, "hadith").apply { mkdirs() }

    /** Downloads one edition, trying each mirror, and keeps it only if its SHA-256 matches. */
    private suspend fun fetch(book: HadithBook, edition: HadithEdition, progress: (Long) -> Unit): File {
        val target = File(cacheDir(), HadithSource.name(book, edition) + ".json")
        val expected = HadithSource.sha256(book, edition)
        if (target.exists() && sha256(target) == expected) return target
        var failure: IOException? = null
        for (url in HadithSource.urls(book, edition)) {
            coroutineContext.ensureActive()
            val tmp = File(target.path + ".part")
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val conn = URL(url).openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 15_000
                    conn.readTimeout = 30_000
                    if (conn.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${conn.responseCode} for $url")
                    conn.inputStream.use { input ->
                        tmp.outputStream().use { out ->
                            val buffer = ByteArray(64 * 1024)
                            var done = 0L
                            while (true) {
                                coroutineContext.ensureActive()
                                val n = input.read(buffer)
                                if (n < 0) break
                                out.write(buffer, 0, n)
                                digest.update(buffer, 0, n)
                                done += n
                                progress(done)
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }
                if (digest.digest().hex() != expected) throw IOException("$url does not match the pinned dataset")
                if (!tmp.renameTo(target)) throw IOException("Could not save ${target.name}")
                return target
            } catch (e: IOException) {
                failure = e
            } finally {
                tmp.delete()
            }
        }
        throw failure ?: IOException("No source for ${HadithSource.name(book, edition)}")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().hex()
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

    /**
     * Reads the three editions into the database in one transaction, so a book is either
     * there whole or not at all. English gives the numbering, chapters and grades; the
     * Arabic and Urdu texts are then joined to it by hadith number.
     */
    private suspend fun import(book: HadithBook, files: Map<HadithEdition, File>) = db.withTransaction {
        clear(book)
        val chapters = ChapterAssigner()
        val batch = ArrayList<HadithRow>(BATCH)
        var seq = 0
        readEdition(files.getValue(HadithEdition.ENGLISH), onChapters = { titles ->
            dao.insertChapters(titles.map { (n, t) -> ChapterRow(book.slug, n, t) })
        }) { h ->
            val number = HadithNumber.of(book, h.number, h.arabicNumber)
            batch += HadithRow(
                book = book.slug,
                seq = seq++,
                number = h.number,
                ref = number.ref,
                base = number.base,
                chapter = chapters.next(h.chapter),
                arabic = "",
                english = h.text,
                urdu = "",
                grades = h.grades.joinToString("\n") { "${it.scholar.clean()}\t${it.grade.clean()}" },
            )
            if (batch.size == BATCH) {
                dao.insertHadith(batch)
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) dao.insertHadith(batch)
        readEdition(files.getValue(HadithEdition.ARABIC)) { h -> if (h.text.isNotEmpty()) dao.setArabic(book.slug, h.number, h.text) }
        readEdition(files.getValue(HadithEdition.URDU)) { h -> if (h.text.isNotEmpty()) dao.setUrdu(book.slug, h.number, h.text) }
        dao.deleteEmpty(book.slug)
        dao.setInstalled(InstalledRow(book.slug, HadithSource.COMMIT, System.currentTimeMillis()))
    }

    private fun String.clean() = replace('\t', ' ').replace('\n', ' ').trim()

    private class RawHadith(val number: String, val arabicNumber: String?, val text: String, val chapter: Int, val grades: List<Grade>)

    /** Streams one edition file: {"metadata": {"sections": {...}}, "hadiths": [...]}. */
    private suspend fun readEdition(
        file: File,
        onChapters: suspend (Map<Int, String>) -> Unit = {},
        onHadith: suspend (RawHadith) -> Unit,
    ) {
        JsonReader(file.bufferedReader()).use { r ->
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "metadata" -> {
                        r.beginObject()
                        while (r.hasNext()) {
                            if (r.nextName() == "sections") {
                                val titles = mutableMapOf<Int, String>()
                                r.beginObject()
                                while (r.hasNext()) {
                                    val n = r.nextName().toIntOrNull()
                                    val t = r.stringOrNull().orEmpty()
                                    if (n != null) titles[n] = t
                                }
                                r.endObject()
                                onChapters(titles)
                            } else {
                                r.skipValue()
                            }
                        }
                        r.endObject()
                    }
                    "hadiths" -> {
                        r.beginArray()
                        while (r.hasNext()) {
                            coroutineContext.ensureActive()
                            onHadith(readHadith(r) ?: continue)
                        }
                        r.endArray()
                    }
                    else -> r.skipValue()
                }
            }
            r.endObject()
        }
    }

    private fun readHadith(r: JsonReader): RawHadith? {
        var number: String? = null
        var arabicNumber: String? = null
        var text = ""
        var chapter = 0
        val grades = mutableListOf<Grade>()
        r.beginObject()
        while (r.hasNext()) {
            when (r.nextName()) {
                "hadithnumber" -> number = r.stringOrNull()
                "arabicnumber" -> arabicNumber = r.stringOrNull()
                "text" -> text = r.stringOrNull().orEmpty().trim()
                "grades" -> {
                    r.beginArray()
                    while (r.hasNext()) {
                        var scholar = ""
                        var grade = ""
                        r.beginObject()
                        while (r.hasNext()) {
                            when (r.nextName()) {
                                "name" -> scholar = r.stringOrNull().orEmpty()
                                "grade" -> grade = r.stringOrNull().orEmpty()
                                else -> r.skipValue()
                            }
                        }
                        r.endObject()
                        if (grade.isNotBlank()) grades += Grade(scholar, grade)
                    }
                    r.endArray()
                }
                "reference" -> {
                    r.beginObject()
                    while (r.hasNext()) {
                        if (r.nextName() == "book" && r.peek() == JsonToken.NUMBER) chapter = r.nextInt() else r.skipValue()
                    }
                    r.endObject()
                }
                else -> r.skipValue()
            }
        }
        r.endObject()
        return number?.let { RawHadith(it, arabicNumber, text, chapter, grades) }
    }

    /** Numbers are read as their literal text, so "402.2" stays "402.2". */
    private fun JsonReader.stringOrNull(): String? = when (peek()) {
        JsonToken.NULL -> nextNull().let { null }
        JsonToken.STRING, JsonToken.NUMBER -> nextString()
        else -> skipValue().let { null }
    }

    private companion object {
        const val BATCH = 500
        const val SEARCH_LIMIT = 100
    }
}
