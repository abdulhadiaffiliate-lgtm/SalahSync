package com.salahsync.ui.goals

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.salahsync.core.goals.DayMark
import com.salahsync.core.goals.GoalKind
import com.salahsync.core.goals.GoalProgress
import com.salahsync.core.goals.GoalSettings
import com.salahsync.core.goals.Goals
import com.salahsync.core.goals.GoalsDay
import com.salahsync.ui.components.CountdownRing
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.IconBadge
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.Cormorant
import com.salahsync.ui.theme.Salah
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale

class GoalsActions(
    val openTracker: () -> Unit,
    val openQuran: () -> Unit,
    val openAdhkar: () -> Unit,
    val setEnabled: (GoalKind, Boolean) -> Unit,
    val setQuranMinutes: (Int) -> Unit,
    val setGoodDays: (Int) -> Unit,
    val setQadaPerWeek: (Int) -> Unit,
    val adjustReading: (Int) -> Unit,
)

private fun GoalKind.icon(): ImageVector = when (this) {
    GoalKind.PRAYERS -> SalahIcons.Check
    GoalKind.ON_TIME -> SalahIcons.Clock
    GoalKind.QURAN -> SalahIcons.Book
    GoalKind.MORNING_ADHKAR, GoalKind.EVENING_ADHKAR -> SalahIcons.Heart
    GoalKind.GOOD_DAYS -> SalahIcons.Flame
    GoalKind.QADA -> SalahIcons.Reset
}

private fun GoalKind.label(s: GoalSettings): String = when (this) {
    GoalKind.QURAN -> "Read Quran for ${s.quranMinutes} min"
    GoalKind.GOOD_DAYS -> "Meet your daily goals on ${days(s.goodDays)}"
    GoalKind.QADA -> "Make up ${s.qadaPerWeek} Qada prayer${if (s.qadaPerWeek == 1) "" else "s"}"
    else -> title
}

private fun GoalKind.hint(): String = when (this) {
    GoalKind.PRAYERS -> "Tick all five in the prayer tracker"
    GoalKind.ON_TIME -> "All five ticked before their time ends"
    GoalKind.QURAN -> "Time in the Quran reader counts by itself"
    GoalKind.MORNING_ADHKAR -> "Count every dhikr on the morning adhkar"
    GoalKind.EVENING_ADHKAR -> "Count every dhikr on the evening adhkar"
    GoalKind.GOOD_DAYS -> "Days this week with every daily goal met"
    GoalKind.QADA -> "Log each one under Qada in the prayer tracker"
}

private fun GoalProgress.status(): String = when (kind) {
    GoalKind.PRAYERS -> "$done of 5 prayed"
    GoalKind.ON_TIME -> "$done of 5 on time"
    GoalKind.QURAN -> "$done of $target min"
    GoalKind.MORNING_ADHKAR, GoalKind.EVENING_ADHKAR -> if (met) "Done" else "Not yet"
    GoalKind.GOOD_DAYS -> "$done of ${days(target)}"
    GoalKind.QADA -> "$done of $target made up"
}

private fun days(n: Int) = if (n == 1) "1 day" else "$n days"

/** Today's goals at a glance, for Home. */
@Composable
fun GoalsCard(state: GoalsDay, onClick: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.accentHairline, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CountdownRing(state.fraction, diameter = 56.dp) {
            when {
                state.allMet -> Icon(SalahIcons.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
                state.daily.isEmpty() -> Icon(SalahIcons.Plus, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                else -> Text("${state.dailyMet}/${state.daily.size}", fontFamily = Cormorant, fontSize = 19.sp, color = c.text)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            EyebrowText("Today's goals", color = c.accent)
            val title = when {
                state.daily.isEmpty() -> "Set a daily goal"
                state.allMet -> "All done today, alhamdulillah"
                else -> "${state.dailyMet} of ${state.daily.size} done"
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            val next = state.daily.firstOrNull { !it.met }
            when {
                state.streak > 0 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(SalahIcons.Flame, contentDescription = null, tint = c.accent, modifier = Modifier.size(14.dp))
                    Text("${days(state.streak)} streak", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
                next != null -> Text("Next: ${next.kind.label(state.settings)}", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                else -> Text("Small daily targets, kept on this phone", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
        }
        Icon(SalahIcons.ChevronRight, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
    }
}

/**
 * Counts time on screen towards the Quran goal while the screen holding it is in front.
 * Time is added every few seconds, so little is lost if the app is closed.
 */
@Composable
fun ReadingTimer(onSeconds: (Int) -> Unit) {
    val add by rememberUpdatedState(onSeconds)
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(Unit) {
        var last = System.nanoTime()
        fun flush() {
            val now = System.nanoTime()
            val seconds = ((now - last) / 1_000_000_000L).toInt()
            if (seconds > 0) {
                add(seconds)
                last += seconds * 1_000_000_000L
            }
        }
        val job = scope.launch {
            while (isActive) {
                delay(15_000)
                flush()
            }
        }
        onPauseOrDispose {
            job.cancel()
            flush()
        }
    }
}

@Composable
fun GoalsScreen(state: GoalsDay?, actions: GoalsActions) {
    val c = Salah.colors
    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EyebrowText("Saved on this phone only", color = c.accent)
            Text("Goals", style = MaterialTheme.typography.headlineLarge, color = c.text)
        }
        if (state == null) return@Column

        Hero(state)
        TodayCard(state, actions)
        WeekCard(state)
        SettingsCard(state.settings, actions)

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp, c.hairline, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(SalahIcons.Shield, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
            Text(
                "Goals use what you already record: prayers and Qada from the prayer tracker, and the adhkar you count. " +
                    "Reading time and your goals are kept on this phone only, never uploaded and left out of cloud backups.",
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun Hero(state: GoalsDay) {
    val c = Salah.colors
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            CountdownRing(state.fraction, diameter = 112.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (state.daily.isEmpty()) "–" else "${state.dailyMet}/${state.daily.size}",
                        fontFamily = Cormorant, fontSize = 32.sp, color = c.text,
                    )
                    EyebrowText("today", modifier = Modifier.padding(top = 2.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EyebrowText("Gentle streak", color = c.accent)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(SalahIcons.Flame, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                    Text(days(state.streak), style = MaterialTheme.typography.titleLarge, color = c.text)
                }
                Text(
                    when {
                        state.allMet -> "All done today, alhamdulillah."
                        state.streak == 0 -> "Meet today's goals to begin. One missed day a week won't break it."
                        else -> "One missed day a week won't break it, and today never does."
                    },
                    style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                )
            }
        }
    }
}

@Composable
private fun TodayCard(state: GoalsDay, actions: GoalsActions) {
    val c = Salah.colors
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today", style = MaterialTheme.typography.titleLarge, color = c.text, modifier = Modifier.weight(1f))
            if (state.daily.isNotEmpty()) EyebrowText("${state.dailyMet} of ${state.daily.size} done")
        }
        if (state.daily.isEmpty()) {
            Text("Turn on a daily goal below to start.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            return@Card
        }
        state.daily.forEach { g ->
            val open = when (g.kind) {
                GoalKind.PRAYERS, GoalKind.ON_TIME -> actions.openTracker
                GoalKind.QURAN -> actions.openQuran
                else -> actions.openAdhkar
            }
            GoalRow(g, g.kind.label(state.settings), open)
            if (g.kind == GoalKind.QURAN) ReadingAdjust(g.done, actions.adjustReading)
        }
    }
}

@Composable
private fun GoalRow(g: GoalProgress, label: String, onClick: (() -> Unit)?) {
    val c = Salah.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: ${g.status()}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(g.kind.icon(), size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(g.status(), style = MaterialTheme.typography.bodySmall, color = if (g.met) c.accent else c.textMuted)
            Bar(g.fraction)
        }
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .then(if (g.met) Modifier.background(c.accent) else Modifier.border(1.dp, c.accentHairline, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (g.met) Icon(SalahIcons.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun Bar(fraction: Float) {
    val c = Salah.colors
    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.text.copy(alpha = 0.08f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.accent))
    }
}

/** For reading from a printed mushaf, which the app cannot see, or to correct the time. */
@Composable
private fun ReadingAdjust(minutes: Int, adjust: (Int) -> Unit) {
    val c = Salah.colors
    Row(Modifier.fillMaxWidth().padding(start = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Read from a mushaf?", style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.weight(1f))
        Pill("− 5 min", enabled = minutes > 0) { adjust(-5) }
        Spacer(Modifier.width(8.dp))
        Pill("+ 5 min", enabled = true) { adjust(5) }
    }
}

@Composable
private fun Pill(text: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(16.dp)
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = if (enabled) c.accent else c.textMuted,
        modifier = Modifier.clip(shape).border(1.dp, if (enabled) c.accentHairline else c.hairline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun WeekCard(state: GoalsDay) {
    val c = Salah.colors
    Card {
        Text("This week", style = MaterialTheme.typography.titleLarge, color = c.text)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            state.week.forEach { (date, mark) ->
                val name = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                val said = when (mark) {
                    DayMark.MET -> "goals met"
                    DayMark.MISSED -> "not met"
                    DayMark.OPEN -> "today, in progress"
                    DayMark.NONE -> "not counted"
                }
                Column(
                    Modifier.semantics(mergeDescendants = true) { contentDescription = "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}: $said" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).then(
                            when (mark) {
                                DayMark.MET -> Modifier.background(c.accent)
                                DayMark.OPEN -> Modifier.border(1.5.dp, c.accent, CircleShape)
                                DayMark.MISSED -> Modifier.border(1.dp, c.accentHairline, CircleShape)
                                DayMark.NONE -> Modifier.border(1.dp, c.hairline, CircleShape)
                            },
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (mark == DayMark.MET) Icon(SalahIcons.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(16.dp))
                    }
                    Text(name, style = MaterialTheme.typography.bodySmall, color = if (mark == DayMark.OPEN) c.accent else c.textMuted)
                }
            }
        }
        if (state.weekly.isEmpty()) {
            Text("Turn on a weekly goal below to aim for this week.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
        state.weekly.forEach { GoalRow(it, it.kind.label(state.settings), onClick = null) }
    }
}

@Composable
private fun SettingsCard(s: GoalSettings, actions: GoalsActions) {
    val c = Salah.colors
    Card {
        Text("Your goals", style = MaterialTheme.typography.titleLarge, color = c.text)
        EyebrowText("Every day")
        Goals.dailyKinds.forEach { kind ->
            Toggle(kind.title, kind.hint(), s.isOn(kind)) { actions.setEnabled(kind, it) }
            if (kind == GoalKind.QURAN && s.isOn(kind)) {
                Stepper("Minutes a day", s.quranMinutes, GoalSettings.quranMinuteRange, GoalSettings.QURAN_MINUTE_STEP, actions.setQuranMinutes)
            }
        }
        EyebrowText("Each week, Friday to Thursday", modifier = Modifier.padding(top = 6.dp))
        Goals.weeklyKinds.forEach { kind ->
            Toggle(kind.title, kind.hint(), s.isOn(kind)) { actions.setEnabled(kind, it) }
            if (kind == GoalKind.GOOD_DAYS && s.isOn(kind)) Stepper("Days a week", s.goodDays, GoalSettings.goodDayRange, 1, actions.setGoodDays)
            if (kind == GoalKind.QADA && s.isOn(kind)) Stepper("Prayers a week", s.qadaPerWeek, GoalSettings.qadaRange, 1, actions.setQadaPerWeek)
        }
    }
}

@Composable
private fun Toggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Salah.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Switch) { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent, checkedTrackColor = c.accent,
                uncheckedThumbColor = c.textMuted, uncheckedTrackColor = c.surfaceDeep, uncheckedBorderColor = c.accentHairline,
            ),
        )
    }
}

@Composable
private fun Stepper(title: String, value: Int, range: IntRange, step: Int, onChange: (Int) -> Unit) {
    val c = Salah.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((value - step).coerceIn(range)) }, enabled = value > range.first) {
            Icon(SalahIcons.Minus, contentDescription = "Less", tint = if (value > range.first) c.accent else c.textMuted, modifier = Modifier.size(18.dp))
        }
        Text("$value", style = MaterialTheme.typography.titleMedium, color = c.text, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
        IconButton(onClick = { onChange((value + step).coerceIn(range)) }, enabled = value < range.last) {
            Icon(SalahIcons.Plus, contentDescription = "More", tint = if (value < range.last) c.accent else c.textMuted, modifier = Modifier.size(18.dp))
        }
    }
}
