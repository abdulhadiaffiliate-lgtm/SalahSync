package com.salahsync.ui.quran

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.quran.Quran
import com.salahsync.core.quran.QuranText
import com.salahsync.quran.QuranViewModel
import com.salahsync.quran.Verse
import com.salahsync.quran.audio.Offline
import com.salahsync.quran.audio.Repeat
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GeometricPattern
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah
import com.salahsync.ui.theme.UrduStyle
import kotlinx.coroutines.delay
import java.util.Locale

private val speeds = listOf(0.75f, 1f, 1.25f, 1.5f)

/** The full "now playing" screen: artwork, the ayah being recited, and transport controls. */
@Composable
fun PlayerScreen(vm: QuranViewModel, onCollapse: () -> Unit, onOpenReader: (AyahRef) -> Unit) {
    val c = Salah.colors
    val state by vm.playback.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val downloaded by vm.downloaded.collectAsStateWithLifecycle()
    var pickReciter by remember { mutableStateOf(false) }
    val entry = state.entry

    LaunchedEffect(entry == null) { if (entry == null) onCollapse() }
    entry ?: return
    val surah = Quran.surah(entry.ref.surah)
    val verse by produceState<Verse?>(null, entry.ref, prefs.mode) { value = vm.verse(entry.ref, prefs.mode) }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.isPlaying, entry.ref) {
        do {
            val (p, d) = vm.progress()
            position = p; duration = d
            delay(250)
        } while (state.isPlaying)
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.surface, c.background, c.background)))) {
        GeometricPattern(Modifier.fillMaxWidth().height(360.dp), alpha = 0.05f)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCollapse) { Icon(SalahIcons.ChevronDown, contentDescription = "Close player", tint = c.text, modifier = Modifier.size(22.dp)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    EyebrowText("Now reciting")
                    Text("Surah ${surah.name}", style = MaterialTheme.typography.bodyMedium, color = c.text)
                }
                IconButton(onClick = { onOpenReader(entry.ref) }) {
                    Icon(SalahIcons.Book, contentDescription = "Open in reader", tint = c.accent, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Artwork(surah.arabicName, surah.number, Modifier.fillMaxWidth(0.78f))
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(surah.name, style = MaterialTheme.typography.headlineMedium, color = c.text)
                    Text(
                        entry.reciter.name, style = MaterialTheme.typography.bodyMedium, color = c.accent,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { pickReciter = true }.padding(vertical = 4.dp),
                    )
                }
                val key = vm.downloadKey(surah.number, entry.reciter)
                val offline = if (key in downloaded) Offline.DOWNLOADED else vm.offline(surah.number, entry.reciter)
                val progress = downloads[key]
                Box(
                    Modifier.size(44.dp).clip(CircleShape).border(1.dp, c.accentHairline, CircleShape)
                        .clickable(enabled = offline == Offline.NONE && entry.reciter == vm.reciter, role = Role.Button) {
                            if (progress != null && !progress.failed) vm.cancelDownload(surah.number) else vm.download(surah.number)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (progress != null && !progress.failed) {
                        CircularProgressIndicator(progress = { progress.fraction }, color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
                    } else {
                        Icon(
                            SalahIcons.Download,
                            contentDescription = if (offline == Offline.NONE) "Download surah" else "Available offline",
                            tint = if (offline == Offline.NONE) c.textMuted else c.accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // Seek across the surah by ayah; the thin bar below shows the current ayah's progress.
            var dragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableFloatStateOf(entry.ref.ayah.toFloat()) }
            Slider(
                value = if (dragging) dragValue else entry.ref.ayah.toFloat(),
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = { vm.seekToAyah(dragValue.toInt()); dragging = false },
                valueRange = 1f..surah.ayahCount.toFloat().coerceAtLeast(1.01f),
                colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.hairline),
            )
            AyahProgress(if (duration > 0) position.toFloat() / duration else 0f)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    "Ayah ${if (dragging) dragValue.toInt() else entry.ref.ayah} of ${surah.ayahCount}",
                    style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.weight(1f),
                )
                Text("${time(position)} / ${time(duration)}", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                SmallControl(
                    if (state.repeat == Repeat.AYAH) SalahIcons.RepeatOne else SalahIcons.Repeat,
                    state.repeat.label, state.repeat != Repeat.OFF, vm::cycleRepeat,
                )
                SmallControl(SalahIcons.Previous, "Previous ayah", false, vm::previous, size = 26.dp)
                Box(
                    Modifier.size(76.dp).clip(CircleShape).background(c.accent).clickable(role = Role.Button, onClick = vm::togglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isBuffering && !state.isPlaying) {
                        CircularProgressIndicator(color = c.onAccent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    } else {
                        Icon(
                            if (state.isPlaying) SalahIcons.Pause else SalahIcons.Play,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = c.onAccent, modifier = Modifier.size(30.dp),
                        )
                    }
                }
                SmallControl(SalahIcons.Next, "Next ayah", false, vm::next, size = 26.dp)
                Box(
                    Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) {
                        vm.setSpeed(speeds[(speeds.indexOf(state.speed).coerceAtLeast(0) + 1) % speeds.size])
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        String.format(Locale.US, "%sx", state.speed.toString().removeSuffix(".0")),
                        style = MaterialTheme.typography.labelLarge, color = if (state.speed != 1f) c.accent else c.textMuted,
                    )
                }
            }
            Text(state.repeat.label, style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(top = 4.dp))

            state.error?.let {
                Text(
                    it, style = MaterialTheme.typography.bodyMedium, color = c.accent, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp).clickable(role = Role.Button, onClick = vm::clearError),
                )
            }

            Spacer(Modifier.height(24.dp))
            // The ayah being recited, like synced lyrics.
            val shape = RoundedCornerShape(24.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(c.card).padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EyebrowText("${surah.name} ${entry.ref}", color = c.onCardMuted)
                AnimatedContent(verse, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) }, label = "ayah") { v ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (v != null) {
                            Text(
                                v.arabic + " " + QuranText.ayahEnd(v.ref.ayah),
                                style = ArabicStyle.copy(fontSize = 26.sp, lineHeight = 50.sp), color = c.onCard,
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                            )
                            v.translations.forEach { (t, text) ->
                                Text(
                                    translationText(text, c.onCardMuted),
                                    style = if (t.rtl) UrduStyle.copy(textAlign = TextAlign.Center) else MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                                    color = c.onCard, modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (pickReciter) {
        ReciterDialog(entry.reciter, onDismiss = { pickReciter = false }) {
            vm.setReciter(it)
            pickReciter = false
        }
    }
}

/** Album-style artwork: the surah's name inside a gold eight-pointed star. */
@Composable
private fun Artwork(arabicName: String, number: Int, modifier: Modifier = Modifier) {
    val c = Salah.colors
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier.aspectRatio(1f).clip(shape).background(Brush.linearGradient(listOf(c.surface, c.surfaceDeep)))
            .border(1.dp, c.accentHairline, shape),
        contentAlignment = Alignment.Center,
    ) {
        GeometricPattern(Modifier.fillMaxSize(), alpha = 0.06f)
        Canvas(Modifier.fillMaxSize(0.82f)) {
            val cx = size.width / 2
            val cy = size.height / 2
            val outer = size.minDimension / 2
            val inner = outer * 0.2666f
            val p = Path().apply {
                moveTo(cx, cy - outer)
                lineTo(cx + inner, cy - inner); lineTo(cx + outer, cy)
                lineTo(cx + inner, cy + inner); lineTo(cx, cy + outer)
                lineTo(cx - inner, cy + inner); lineTo(cx - outer, cy)
                lineTo(cx - inner, cy - inner); close()
            }
            drawPath(p, c.accent.copy(alpha = 0.55f), style = Stroke(1.2.dp.toPx()))
            drawCircle(c.accent.copy(alpha = 0.35f), radius = outer * 0.62f, center = Offset(cx, cy), style = Stroke(0.8.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(arabicName, style = ArabicStyle.copy(fontSize = 44.sp, lineHeight = 70.sp), color = c.text)
            EyebrowText(QuranText.arabicNumber(number), color = c.accent)
        }
    }
}

@Composable
private fun AyahProgress(fraction: Float) {
    val c = Salah.colors
    Canvas(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(3.dp)) {
        val y = size.height / 2
        drawLine(c.hairline, Offset(0f, y), Offset(size.width, y), strokeWidth = size.height, cap = StrokeCap.Round)
        drawLine(c.accent.copy(alpha = 0.6f), Offset(0f, y), Offset(size.width * fraction.coerceIn(0f, 1f), y), strokeWidth = size.height, cap = StrokeCap.Round)
    }
}

@Composable
private fun SmallControl(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit, size: Dp = 22.dp) {
    val c = Salah.colors
    Box(
        Modifier.size(52.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (active) c.accent else c.text, modifier = Modifier.size(size))
    }
}

private fun time(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

/** The bar above the tabs while a recitation is queued; tap it for the full player. */
@Composable
fun MiniPlayer(vm: QuranViewModel, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val c = Salah.colors
    val state by vm.playback.collectAsStateWithLifecycle()
    val entry = state.entry ?: return
    val surah = Quran.surah(entry.ref.surah)
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.accentHairline, shape)
            .clickable(role = Role.Button, onClick = onExpand),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StarBadge(surah.number, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${surah.name} · Ayah ${entry.ref.ayah}", style = MaterialTheme.typography.bodyLarge, color = c.text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.error ?: entry.reciter.name, style = MaterialTheme.typography.bodySmall,
                    color = if (state.error != null) c.accent else c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = vm::togglePlay) {
                if (state.isBuffering && !state.isPlaying) {
                    CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(
                        if (state.isPlaying) SalahIcons.Pause else SalahIcons.Play,
                        contentDescription = if (state.isPlaying) "Pause" else "Play", tint = c.accent, modifier = Modifier.size(20.dp),
                    )
                }
            }
            IconButton(onClick = vm::next) { Icon(SalahIcons.Next, contentDescription = "Next ayah", tint = c.text, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = vm::stop) { Icon(SalahIcons.Close, contentDescription = "Stop", tint = c.textMuted, modifier = Modifier.size(16.dp)) }
        }
        val fraction = entry.ref.ayah.toFloat() / surah.ayahCount
        Box(Modifier.fillMaxWidth().height(2.dp).background(c.hairline)) {
            Box(Modifier.fillMaxWidth(fraction).height(2.dp).background(c.accent))
        }
    }
}
