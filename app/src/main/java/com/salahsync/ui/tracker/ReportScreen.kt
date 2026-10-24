package com.salahsync.ui.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.core.Prayer
import com.salahsync.core.tracker.PrayerStatus
import com.salahsync.core.tracker.StatusCounts
import com.salahsync.core.tracker.WeeklyReport
import com.salahsync.core.tracker.WeeklyReports
import com.salahsync.quran.share.VerseArt
import com.salahsync.tracker.ReportArt
import com.salahsync.tracker.TrackerState
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.Cormorant
import com.salahsync.ui.theme.Salah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekday = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH)

/**
 * The Friday-to-Thursday prayer report, worked out on the phone from the tracker. It
 * opens on the last full week; the arrows move between weeks since tracking began.
 */
@Composable
fun ReportScreen(state: TrackerState?, onOpenTracker: () -> Unit) {
    val c = Salah.colors
    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EyebrowText("Made on this phone only", color = c.accent)
            Text("Weekly report", style = MaterialTheme.typography.headlineLarge, color = c.text)
        }
        if (state == null) return@Column

        val thisWeek = WeeklyReports.weekStart(state.today)
        val firstWeek = WeeklyReports.weekStart(state.since)
        var weekEpochDay by rememberSaveable {
            mutableStateOf(maxOf(WeeklyReports.lastCompleteWeekStart(state.today), firstWeek).toEpochDay())
        }
        val week = LocalDate.ofEpochDay(weekEpochDay)
        val report = remember(state, week) { state.report(week) }

        WeekSwitcher(
            report = report,
            isThisWeek = week == thisWeek,
            canGoBack = week.isAfter(firstWeek),
            canGoForward = week.isBefore(thisWeek),
            onBack = { weekEpochDay = week.minusWeeks(1).toEpochDay() },
            onForward = { weekEpochDay = week.plusWeeks(1).toEpochDay() },
        )

        if (!report.hasData) {
            Card {
                Text("Nothing tracked this week", style = MaterialTheme.typography.titleLarge, color = c.text)
                Text(
                    "Tick your prayers in the tracker and they will show up here, with how many were on time.",
                    style = MaterialTheme.typography.bodyMedium, color = c.textMuted,
                )
                Text(
                    "Open the tracker", style = MaterialTheme.typography.labelLarge, color = c.accent,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onOpenTracker)
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                )
            }
            return@Column
        }

        SummaryCard(report)
        WeekGridCard(report)
        ByPrayerCard(report)
        QadaStreakCard(report)
        ShareButton(report)

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp, c.hairline, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(SalahIcons.Shield, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
            Text(
                "This report is worked out from your tracker on this phone. Nothing is uploaded; the image is only " +
                    "shared if you choose to share it. A prayer ticked after its time ended counts as late.",
                style = MaterialTheme.typography.bodySmall, color = c.textMuted,
            )
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.accentHairline, shape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun WeekSwitcher(
    report: WeeklyReport,
    isThisWeek: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    val c = Salah.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, enabled = canGoBack) {
            Icon(SalahIcons.ChevronLeft, "Earlier week", tint = if (canGoBack) c.accent else c.textMuted.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(ReportArt.rangeLabel(report), style = MaterialTheme.typography.titleMedium, color = c.text, textAlign = TextAlign.Center)
            EyebrowText(if (isThisWeek) "This week so far" else "Friday to Thursday")
        }
        IconButton(onClick = onForward, enabled = canGoForward) {
            Icon(SalahIcons.ChevronRight, "Later week", tint = if (canGoForward) c.accent else c.textMuted.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SummaryCard(report: WeeklyReport) {
    val c = Salah.colors
    val t = report.totals
    Card {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${t.prayed}", fontFamily = Cormorant, fontSize = 64.sp, lineHeight = 66.sp, color = c.accent)
            Text(
                "of ${t.tracked} prayed", style = MaterialTheme.typography.bodyLarge, color = c.textMuted,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }
        ProportionBar(t, Modifier.fillMaxWidth().height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("On time", t.onTime, c.accent, Modifier.weight(1f))
            Stat("Late", t.late, c.accent.copy(alpha = 0.45f), Modifier.weight(1f))
            Stat("Missed", t.missed, c.textMuted.copy(alpha = 0.35f), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, swatch: Color, modifier: Modifier = Modifier) {
    val c = Salah.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$value", fontFamily = Cormorant, fontSize = 34.sp, lineHeight = 38.sp, color = c.text)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(swatch))
            EyebrowText(label)
        }
    }
}

/** On time, late and missed as one bar, so the week reads at a glance. */
@Composable
private fun ProportionBar(counts: StatusCounts, modifier: Modifier = Modifier) {
    val c = Salah.colors
    val total = counts.tracked.coerceAtLeast(1).toFloat()
    Row(modifier.clip(RoundedCornerShape(4.dp)).background(c.textMuted.copy(alpha = 0.18f))) {
        if (counts.onTime > 0) Box(Modifier.weight(counts.onTime / total).fillMaxHeight().background(c.accent))
        if (counts.late > 0) Box(Modifier.weight(counts.late / total).fillMaxHeight().background(c.accent.copy(alpha = 0.45f)))
        val rest = (total - counts.onTime - counts.late) / total
        if (rest > 0f) Spacer(Modifier.weight(rest))
    }
}

@Composable
private fun WeekGridCard(report: WeeklyReport) {
    val c = Salah.colors
    Card {
        Text("The week", style = MaterialTheme.typography.titleLarge, color = c.text)
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Prayer.fard.forEach { p ->
                Text(
                    p.displayName.take(1), style = MaterialTheme.typography.labelMedium, color = c.textMuted,
                    modifier = Modifier.width(40.dp), textAlign = TextAlign.Center,
                )
            }
        }
        report.days.forEach { day ->
            Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(day.date.format(weekday), style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
                Prayer.fard.forEach { p ->
                    val status = day.statuses.getValue(p)
                    Box(
                        Modifier.width(40.dp).semantics { contentDescription = "${p.displayName}, ${day.date.format(weekday)}: ${status.spoken()}" },
                        contentAlignment = Alignment.Center,
                    ) { StatusMark(status) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(PrayerStatus.ON_TIME, "On time")
            Legend(PrayerStatus.LATE, "Late")
            Legend(PrayerStatus.MISSED, "Missed")
        }
    }
}

@Composable
private fun Legend(status: PrayerStatus, label: String) {
    val c = Salah.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusMark(status, size = 14)
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
}

@Composable
private fun StatusMark(status: PrayerStatus, size: Int = 20) {
    val c = Salah.colors
    when (status) {
        PrayerStatus.ON_TIME -> Icon(SalahIcons.Check, null, tint = c.accent, modifier = Modifier.size(size.dp))
        PrayerStatus.LATE -> Icon(SalahIcons.Clock, null, tint = c.accent.copy(alpha = 0.7f), modifier = Modifier.size(size.dp))
        PrayerStatus.MISSED -> Icon(SalahIcons.Close, null, tint = c.textMuted.copy(alpha = 0.6f), modifier = Modifier.size((size * 0.8f).dp))
        PrayerStatus.PENDING -> Icon(SalahIcons.Circle, null, tint = c.text.copy(alpha = 0.3f), modifier = Modifier.size(size.dp))
        PrayerStatus.UNTRACKED -> Box(Modifier.size(4.dp).clip(CircleShape).background(c.textMuted.copy(alpha = 0.3f)))
    }
}

private fun PrayerStatus.spoken(): String = when (this) {
    PrayerStatus.ON_TIME -> "on time"
    PrayerStatus.LATE -> "prayed late"
    PrayerStatus.MISSED -> "missed"
    PrayerStatus.PENDING -> "not prayed yet"
    PrayerStatus.UNTRACKED -> "not tracked"
}

@Composable
private fun ByPrayerCard(report: WeeklyReport) {
    val c = Salah.colors
    Card {
        Text("By prayer", style = MaterialTheme.typography.titleLarge, color = c.text)
        Prayer.fard.forEach { p ->
            val counts = report.byPrayer.getValue(p)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(p.displayName, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                    Text(
                        "${counts.onTime} on time · ${counts.late} late · ${counts.missed} missed",
                        style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                    )
                }
                ProportionBar(counts, Modifier.fillMaxWidth().height(5.dp))
            }
        }
    }
}

@Composable
private fun QadaStreakCard(report: WeeklyReport) {
    val c = Salah.colors
    Card {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                EyebrowText("Qada owed")
                Text("${report.qadaOwed}", fontFamily = Cormorant, fontSize = 44.sp, lineHeight = 48.sp, color = if (report.qadaOwed > 0) c.accent else c.text)
                Text("in total, not just this week", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
            Column(Modifier.weight(1f)) {
                EyebrowText("Streak")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(SalahIcons.Flame, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
                    Text("${report.currentStreak}", fontFamily = Cormorant, fontSize = 44.sp, lineHeight = 48.sp, color = c.text)
                }
                Text(
                    if (report.currentStreak == 1) "day with all five" else "days with all five",
                    style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Best streak: ${days(report.bestStreak)}", style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
            Text("All on time: ${days(report.onTimeStreak)}", style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
        }
    }
}

private fun days(n: Int) = if (n == 1) "1 day" else "$n days"

@Composable
private fun ShareButton(report: WeeklyReport) {
    val c = Salah.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(c.accent)
            .clickable(enabled = !busy, role = Role.Button) {
                busy = true
                scope.launch {
                    val image = withContext(Dispatchers.Default) { ReportArt.render(context, report) }
                    val t = report.totals
                    VerseArt.share(
                        context, image,
                        "My week of prayer, ${ReportArt.rangeLabel(report)}: ${t.prayed} of ${t.tracked} prayed, ${t.onTime} on time.\n\nShared from SalahSync",
                        fileName = "weekly-report.png",
                    )
                    busy = false
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(SalahIcons.Share, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (busy) "Preparing image…" else "Share as image", style = MaterialTheme.typography.labelLarge, color = c.onAccent)
    }
}
