package com.salahsync.ui.prayers

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.salahsync.core.DayTimes
import com.salahsync.core.HijriDate
import com.salahsync.core.Prayer
import com.salahsync.data.AlertStyle
import com.salahsync.data.UserPrefs
import com.salahsync.ui.components.DualCalendarDialog
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah
import com.salahsync.ui.theme.TabularTime
import com.salahsync.ui.util.formatTime
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val headerDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)

@Composable
fun PrayerTimesScreen(
    prefs: UserPrefs,
    now: ZonedDateTime,
    loadTimes: suspend (LocalDate) -> DayTimes?,
    hijriFor: (LocalDate) -> HijriDate,
    isPrayed: (LocalDate, Prayer) -> Boolean,
    onTogglePrayed: (LocalDate, Prayer) -> Unit,
    qadaOwed: Int,
    onOpenTracker: () -> Unit,
    onAlertChange: (Prayer, AlertStyle) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = Salah.colors
    var date by remember { mutableStateOf(now.toLocalDate()) }
    var times by remember { mutableStateOf<DayTimes?>(null) }
    LaunchedEffect(date, prefs.prayerSettings, prefs.location, prefs.useOnlineTimes, now.toLocalDate()) {
        times = loadTimes(date)
    }
    val isToday = date == now.toLocalDate()
    val next = if (isToday) times?.next(now.toInstant()) else null
    var calendarOpen by remember { mutableStateOf(false) }
    if (calendarOpen) {
        DualCalendarDialog(
            initial = date,
            today = now.toLocalDate(),
            hijriFor = hijriFor,
            onDismiss = { calendarOpen = false },
            pickLabel = "Show prayer times",
            onPick = { date = it; calendarOpen = false },
        )
    }

    Column(Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState())) {
        Box(
            Modifier.fillMaxWidth().height(250.dp)
                .background(Brush.verticalGradient(listOf(c.surface.copy(alpha = 1f), c.surfaceDeep, c.background))),
        ) {
            SunArc(times, now, isToday, Modifier.fillMaxSize())
            Column(Modifier.statusBarsPadding().padding(horizontal = 24.dp).padding(top = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onOpenSettings)
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(SalahIcons.Pin, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                        Text(prefs.location?.placeName ?: "Set location", style = MaterialTheme.typography.labelLarge, color = c.text)
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.border(1.dp, c.accentHairline, RoundedCornerShape(22.dp))) {
                        Icon(SalahIcons.Sliders, contentDescription = "Prayer settings", tint = c.text, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { date = date.minusDays(1) }) {
                        Icon(SalahIcons.ChevronLeft, contentDescription = "Previous day", tint = c.textMuted, modifier = Modifier.size(18.dp))
                    }
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button, onClickLabel = "Open calendar") { calendarOpen = true },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(date.format(headerDate), style = MaterialTheme.typography.headlineMedium, color = c.text)
                        EyebrowText(hijriFor(date).toString(), color = c.accent)
                        if (!isToday) {
                            EyebrowText(
                                "Back to today",
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .clickable(role = Role.Button) { date = now.toLocalDate() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                    IconButton(onClick = { date = date.plusDays(1) }) {
                        Icon(SalahIcons.ChevronRight, contentDescription = "Next day", tint = c.textMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp).padding(top = 4.dp)) {
            val t = times
            if (t == null) {
                Text(
                    "Set your location in Settings to see prayer times.",
                    style = MaterialTheme.typography.bodyLarge, color = c.textMuted, modifier = Modifier.padding(16.dp),
                )
            } else {
                Prayer.entries.forEach { p ->
                    val passed = isToday && !t[p].toInstant().isAfter(now.toInstant())
                    PrayerRow(
                        prayer = p,
                        label = p.labelOn(t.isFriday),
                        arabic = p.arabicLabelOn(t.isFriday),
                        time = formatTime(t[p]),
                        isNext = p == next,
                        dimmed = (passed || p == Prayer.SUNRISE) && p != next,
                        prayed = isPrayed(date, p),
                        canTick = !date.isAfter(now.toLocalDate()),
                        alert = prefs.alertFor(p),
                        onTogglePrayed = { onTogglePrayed(date, p) },
                        onCycleAlert = {
                            val current = prefs.alertFor(p)
                            // Sunrise has no adhan: it only toggles between a silent note and off.
                            val nextStyle = if (p == Prayer.SUNRISE) {
                                if (current == AlertStyle.OFF) AlertStyle.SILENT else AlertStyle.OFF
                            } else current.next()
                            onAlertChange(p, nextStyle)
                        },
                    )
                }
            }
        }

        Row(
            Modifier.padding(horizontal = 20.dp).padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .border(1.dp, c.hairline, RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClick = onOpenTracker)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(SalahIcons.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text("Prayer tracker", style = MaterialTheme.typography.titleMedium, color = c.text)
                Text(
                    if (qadaOwed == 0) "No Qada owed" else "$qadaOwed Qada ${if (qadaOwed == 1) "prayer" else "prayers"} to make up",
                    style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                )
            }
            Icon(SalahIcons.ChevronRight, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp).padding(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Chip(prefs.method.displayName.substringBefore(',').substringBefore(" (").let {
                if (it.length > 22) it.take(20) + "…" else it
            })
            Chip("${prefs.asrSchool.displayName.substringBefore(',')} Asr")
            if (prefs.useOnlineTimes) Chip("AlAdhan")
        }
    }
}

@Composable
private fun Chip(text: String) {
    val c = Salah.colors
    Text(
        text, style = MaterialTheme.typography.bodySmall, color = c.textMuted,
        modifier = Modifier.border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun PrayerRow(
    prayer: Prayer,
    label: String,
    arabic: String,
    time: String,
    isNext: Boolean,
    dimmed: Boolean,
    prayed: Boolean,
    canTick: Boolean,
    alert: AlertStyle,
    onTogglePrayed: () -> Unit,
    onCycleAlert: () -> Unit,
) {
    val c = Salah.colors
    val shape = RoundedCornerShape(20.dp)
    var mod = Modifier.fillMaxWidth().height(64.dp).clip(shape)
    if (isNext) {
        mod = mod.background(Brush.linearGradient(listOf(c.surface, c.surfaceDeep))).border(1.dp, c.accentHairline, shape)
    }
    Row(mod.alpha(if (dimmed) 0.6f else 1f).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (prayer.isFard) {
            IconButton(onClick = onTogglePrayed, enabled = canTick) {
                Icon(
                    if (prayed) SalahIcons.Check else SalahIcons.Circle,
                    contentDescription = if (prayed) "$label prayed, tap to undo" else "Mark $label as prayed",
                    tint = if (prayed) c.accent else c.text.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(label, style = MaterialTheme.typography.titleLarge, color = if (isNext) c.accent else c.text)
            Text(arabic, style = ArabicStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize), color = c.textMuted)
        }
        Text(time, style = TabularTime, color = if (isNext) c.accent else c.text)
        IconButton(onClick = onCycleAlert) {
            Icon(
                when (alert) {
                    AlertStyle.ADHAN -> SalahIcons.Speaker
                    AlertStyle.NOTIFICATION -> SalahIcons.Bell
                    AlertStyle.SILENT -> SalahIcons.Muted
                    AlertStyle.OFF -> SalahIcons.BellOff
                },
                contentDescription = "$label alert: ${alert.displayName}. Tap to change.",
                tint = if (isNext) c.accent else c.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** The sun's path from sunrise to maghrib, with its position now. */
@Composable
private fun SunArc(times: DayTimes?, now: ZonedDateTime, isToday: Boolean, modifier: Modifier) {
    val c = Salah.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val start = Offset(-10.dp.toPx(), h * 0.84f)
        val end = Offset(w + 10.dp.toPx(), h * 0.84f)
        val control = Offset(w / 2, h * 0.16f)
        val arc = Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(control.x, control.y, end.x, end.y)
        }
        drawPath(
            arc, c.accent.copy(alpha = 0.28f),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))),
        )
        if (times == null || !isToday) return@Canvas
        val rise = times[Prayer.SUNRISE]
        val set = times[Prayer.MAGHRIB]
        val span = Duration.between(rise, set).seconds.toFloat()
        val f = Duration.between(rise, now).seconds / span
        val sunUp = f in 0f..1f
        val t = f.coerceIn(0f, 1f)
        val x = (1 - t) * (1 - t) * start.x + 2 * (1 - t) * t * control.x + t * t * end.x
        val y = (1 - t) * (1 - t) * start.y + 2 * (1 - t) * t * control.y + t * t * end.y
        val color = if (sunUp) c.accent else Color(0xFFCFD6E6)
        drawCircle(color, radius = 9.dp.toPx(), center = Offset(x, y))
        drawCircle(color.copy(alpha = 0.3f), radius = 20.dp.toPx(), center = Offset(x, y), style = Stroke(1.dp.toPx()))
    }
}
