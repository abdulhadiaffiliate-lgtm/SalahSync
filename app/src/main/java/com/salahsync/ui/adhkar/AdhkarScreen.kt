package com.salahsync.ui.adhkar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahsync.adhkar.AdhkarViewModel
import com.salahsync.adhkar.PassageText
import com.salahsync.core.adhkar.Adhkar
import com.salahsync.core.adhkar.AdhkarSession
import com.salahsync.core.adhkar.Dhikr
import com.salahsync.core.quran.QuranText
import com.salahsync.ui.components.CountdownRing
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GoldDivider
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Cormorant
import com.salahsync.ui.theme.Salah

/** Morning and evening adhkar: tap a card to count, hold a Quran card for its full text, tap ⓘ for the source. */
@Composable
fun AdhkarScreen(vm: AdhkarViewModel) {
    val c = Salah.colors
    val haptics = LocalHapticFeedback.current
    val session by vm.session.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val passages by vm.passages.collectAsStateWithLifecycle()
    val basmala by vm.basmala.collectAsStateWithLifecycle()
    val items = Adhkar.forSession(session)
    var info by remember { mutableStateOf<Dhikr?>(null) }
    var fullText by remember { mutableStateOf<Dhikr?>(null) }
    val done = items.count { (progress[it.id] ?: 0) >= it.count }

    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EyebrowText("$done of ${items.size} complete")
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Adhkar", style = MaterialTheme.typography.headlineLarge, color = c.text)
                Text(session.arabicName, style = ArabicStyle.copy(fontSize = 20.sp), color = c.accent)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdhkarSession.entries.forEach { s ->
                val selected = s == session
                Text(
                    s.displayName, style = MaterialTheme.typography.labelLarge,
                    color = if (selected) c.onAccent else c.textMuted,
                    modifier = Modifier.clip(RoundedCornerShape(18.dp))
                        .then(if (selected) Modifier.background(c.accent) else Modifier.border(1.dp, c.hairline, RoundedCornerShape(18.dp)))
                        .clickable(role = Role.Tab) { vm.setSession(s) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
        Text(
            "Tap a card to count. Hold Ayat al-Kursi or the three Quls to read them in full. Tap ⓘ for the hadith source.",
            style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(horizontal = 4.dp),
        )

        items.forEach { d ->
            DhikrCard(
                dhikr = d,
                count = progress[d.id] ?: 0,
                preview = passages[d.id]?.firstOrNull()?.ayat?.firstOrNull()?.second,
                onTap = {
                    val finished = vm.tap(d)
                    haptics.performHapticFeedback(if (finished) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
                },
                onHold = if (d.isQuran) ({ fullText = d }) else null,
                onInfo = { info = d },
                onReset = { vm.reset(d) },
            )
        }
    }

    info?.let { d -> ReferenceDialog(d, passages[d.id].orEmpty()) { info = null } }
    fullText?.let { d -> FullTextDialog(d, passages[d.id].orEmpty(), basmala) { fullText = null } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DhikrCard(
    dhikr: Dhikr,
    count: Int,
    preview: String?,
    onTap: () -> Unit,
    onHold: (() -> Unit)?,
    onInfo: () -> Unit,
    onReset: () -> Unit,
) {
    val c = Salah.colors
    val complete = count >= dhikr.count
    val shape = RoundedCornerShape(22.dp)
    val progress by animateFloatAsState(count / dhikr.count.toFloat(), tween(200), label = "dhikr")
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (complete) c.surfaceDeep else c.surface)
            .border(1.dp, if (complete) c.accentHairline else c.hairline, shape)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Count ${dhikr.title}",
                onLongClickLabel = if (onHold != null) "Read full text" else null,
                onLongClick = onHold,
                onClick = onTap,
            )
            .padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dhikr.title, style = MaterialTheme.typography.titleMedium, color = if (complete) c.accent else c.text, modifier = Modifier.weight(1f))
            IconButton(onClick = onInfo) {
                Icon(SalahIcons.Info, contentDescription = "Source of ${dhikr.title}", tint = c.accent, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (dhikr.isQuran) {
                if (preview != null) {
                    Text(
                        preview, style = ArabicStyle.copy(fontSize = 22.sp, lineHeight = 42.sp), color = c.text,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text("Hold to read the full text", style = MaterialTheme.typography.labelMedium, color = c.accent)
            } else {
                Text(dhikr.arabic, style = ArabicStyle.copy(fontSize = 22.sp, lineHeight = 42.sp), color = c.text, modifier = Modifier.fillMaxWidth())
            }
            Text(dhikr.meaning, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CountdownRing(progress, diameter = 40.dp) {
                    if (complete) Icon(SalahIcons.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                }
                Text(
                    if (complete) "Done" else "$count of ${dhikr.count}",
                    style = MaterialTheme.typography.labelLarge, color = if (complete) c.accent else c.text,
                    modifier = Modifier.weight(1f),
                )
                if (count > 0) {
                    IconButton(onClick = onReset) {
                        Icon(SalahIcons.Reset, contentDescription = "Reset ${dhikr.title}", tint = c.textMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceDialog(dhikr: Dhikr, passages: List<PassageText>, onDismiss: () -> Unit) {
    val c = Salah.colors
    val uri = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(dhikr.title, style = MaterialTheme.typography.titleLarge, color = c.text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (dhikr.isQuran) {
                    passages.forEach { p ->
                        EyebrowText(p.reference, color = c.accent)
                        Text(
                            p.ayat.joinToString(" ") { (n, t) -> "$t ${QuranText.ayahEnd(n)}" },
                            style = ArabicStyle.copy(fontSize = 20.sp, lineHeight = 38.sp), color = c.text, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Text(dhikr.arabic, style = ArabicStyle.copy(fontSize = 20.sp, lineHeight = 38.sp), color = c.text, modifier = Modifier.fillMaxWidth())
                }
                EyebrowText("Say it ${times(dhikr.count)}")
                GoldDivider()
                EyebrowText("Virtue", color = c.accent)
                Text(dhikr.virtue, style = MaterialTheme.typography.bodyMedium, color = c.text)
                EyebrowText(if (dhikr.references.size > 1) "References" else "Reference", color = c.accent)
                dhikr.references.forEach { r ->
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        Modifier.fillMaxWidth().clip(shape).border(1.dp, c.hairline, shape).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("${r.book} ${r.number}", style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text("Narrated by ${r.narrator}", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                        Text("Grade: ${r.grade}", style = MaterialTheme.typography.bodySmall, color = c.text)
                        Text(
                            "View on sunnah.com", style = MaterialTheme.typography.labelLarge, color = c.accent,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) { runCatching { uri.openUri(r.url) } }
                                .padding(vertical = 8.dp),
                        )
                    }
                }
                dhikr.note?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = c.textMuted)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = c.accent) } },
    )
}

private fun times(n: Int) = when (n) {
    1 -> "once"
    2 -> "twice"
    else -> "$n times"
}

@Composable
private fun FullTextDialog(dhikr: Dhikr, passages: List<PassageText>, basmala: String, onDismiss: () -> Unit) {
    val c = Salah.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier.padding(16.dp).fillMaxWidth().clip(shape).background(c.background).border(1.dp, c.accentHairline, shape)
                .verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    EyebrowText("Say it ${times(dhikr.count)}")
                    Text(dhikr.title, style = MaterialTheme.typography.titleLarge, color = c.text)
                }
                IconButton(onClick = onDismiss) {
                    Icon(SalahIcons.Close, contentDescription = "Close", tint = c.textMuted, modifier = Modifier.size(20.dp))
                }
            }
            passages.forEach { p ->
                GoldDivider()
                Text(p.surahArabicName, style = ArabicStyle.copy(fontSize = 22.sp), color = c.accent)
                EyebrowText(p.reference)
                if (p.withBasmala && basmala.isNotBlank()) {
                    Text(basmala, style = ArabicStyle.copy(fontSize = 22.sp, lineHeight = 40.sp), color = c.text, textAlign = TextAlign.Center)
                }
                Text(
                    p.ayat.joinToString(" ") { (n, t) -> "$t ${QuranText.ayahEnd(n)}" },
                    style = ArabicStyle.copy(fontSize = 26.sp, lineHeight = 52.sp), color = c.text,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                p.english.forEachIndexed { i, e ->
                    Text(
                        "(${p.ayat[i].first}) ${QuranText.displayTranslation(e).replace(Regex("""\[\d+]"""), "")}",
                        fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 17.sp, lineHeight = 23.sp,
                        color = c.textMuted, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Box(Modifier.height(4.dp))
            Text(
                "Arabic: Tanzil Uthmani text. English: Saheeh International.",
                style = MaterialTheme.typography.bodySmall, color = c.textMuted, textAlign = TextAlign.Center,
            )
        }
    }
}
