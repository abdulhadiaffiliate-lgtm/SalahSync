package com.salahsync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.core.Hijri
import com.salahsync.core.HijriDate
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val fullDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val weekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

/**
 * Shows a date in both calendars, with a month grid where each day carries its
 * Gregorian number and the Hijri day underneath. [onPick] adds a button that
 * hands the chosen day back, for screens that can jump to it.
 */
@Composable
fun DualCalendarDialog(
    initial: LocalDate,
    today: LocalDate,
    hijriFor: (LocalDate) -> HijriDate,
    onDismiss: () -> Unit,
    pickLabel: String = "Show this day",
    onPick: ((LocalDate) -> Unit)? = null,
) {
    val c = Salah.colors
    var selected by remember { mutableStateOf(initial) }
    var month by remember { mutableStateOf(YearMonth.from(initial)) }
    val hijri = hijriFor(selected)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(selected.format(fullDate), style = MaterialTheme.typography.titleLarge, color = c.text)
                Text(hijri.toString(), style = MaterialTheme.typography.titleMedium, color = c.accent)
                Text(
                    "${hijri.day} ${hijri.monthNameArabic} ${hijri.year}",
                    style = ArabicStyle.copy(fontSize = 18.sp, lineHeight = 30.sp), color = c.textMuted,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Icon(SalahIcons.ChevronLeft, contentDescription = "Previous month", tint = c.textMuted, modifier = Modifier.size(18.dp))
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(month.format(monthTitle), style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(
                            Hijri.span(hijriFor(month.atDay(1)), hijriFor(month.atEndOfMonth())),
                            style = MaterialTheme.typography.bodySmall, color = c.accent, textAlign = TextAlign.Center,
                        )
                    }
                    IconButton(onClick = { month = month.plusMonths(1) }) {
                        Icon(SalahIcons.ChevronRight, contentDescription = "Next month", tint = c.textMuted, modifier = Modifier.size(18.dp))
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    weekdays.forEach {
                        Text(
                            it, style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                            textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                        )
                    }
                }
                // Monday-first grid, padded with blanks before the 1st and after the last day.
                val lead = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
                val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                                if (day != null) {
                                    DayCell(day, hijriFor(day), day == selected, day == today) { selected = day }
                                }
                            }
                        }
                        repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = {
            if (onPick != null) {
                TextButton(onClick = { onPick(selected) }) { Text(pickLabel, color = c.accent) }
            } else {
                TextButton(onClick = onDismiss) { Text("Close", color = c.accent) }
            }
        },
        dismissButton = {
            Row {
                if (selected != today) {
                    TextButton(onClick = { selected = today; month = YearMonth.from(today) }) { Text("Today", color = c.textMuted) }
                }
                if (onPick != null) TextButton(onClick = onDismiss) { Text("Close", color = c.textMuted) }
            }
        },
    )
}

@Composable
private fun DayCell(day: LocalDate, hijri: HijriDate, selected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val c = Salah.colors
    val shape = CircleShape
    var mod: Modifier = Modifier.size(40.dp).clip(shape)
    mod = when {
        selected -> mod.background(c.accent)
        isToday -> mod.border(1.dp, c.accent, shape)
        else -> mod
    }
    Column(
        mod.clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${day.dayOfMonth} ${day.month.name.lowercase().replaceFirstChar { it.uppercase() }}, ${hijri}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "${day.dayOfMonth}", style = MaterialTheme.typography.bodyMedium,
            color = if (selected) c.onAccent else c.text,
        )
        Text(
            "${hijri.day}", fontSize = 9.sp, lineHeight = 10.sp,
            color = if (selected) c.onAccent else c.accent,
        )
    }
}
