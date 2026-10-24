package com.salahsync.ui.tracker

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.core.Prayer
import com.salahsync.core.tracker.QadaCount
import com.salahsync.tracker.TrackerState
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Cormorant
import com.salahsync.ui.theme.Salah
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayLabel = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val sinceLabel = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

/** Shown and how many past days of the history are editable here. */
private const val HISTORY_DAYS = 7L

class TrackerActions(
    val togglePrayed: (LocalDate, Prayer) -> Unit,
    val logMadeUp: (Prayer) -> Unit,
    val undoMadeUp: (Prayer) -> Unit,
    val setEarlier: (Prayer, Int) -> Unit,
)

/** Daily prayer ticks and the Qada they leave behind, all kept on this phone. */
@Composable
fun TrackerScreen(state: TrackerState?, actions: TrackerActions) {
    val c = Salah.colors
    var editingEarlier by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EyebrowText("Saved on this phone only", color = c.accent)
            Text("Prayer tracker", style = MaterialTheme.typography.headlineLarge, color = c.text)
        }
        if (state == null) return@Column

        TodayCard(state, actions)
        QadaCard(state, actions, onEditEarlier = { editingEarlier = true })
        HistoryCard(state, actions)

        Text(
            "A prayer that is not ticked by the end of its day is counted as Qada. If you prayed it and forgot to tick it, " +
                "tick it in the last week above and it is taken off. Tracking began on ${state.since.format(sinceLabel)}.",
            style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(horizontal = 4.dp),
        )
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp, c.hairline, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(SalahIcons.Shield, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
            Text(
                "Your prayer record is kept in a private database on this phone. It is never uploaded and is left out of " +
                    "cloud backups, so uninstalling SalahSync or clearing its data deletes it.",
                style = MaterialTheme.typography.bodySmall, color = c.textMuted,
            )
        }
    }
    if (editingEarlier && state != null) {
        EarlierDialog(state.qada, onDismiss = { editingEarlier = false }) { values ->
            values.forEach { (p, n) -> actions.setEarlier(p, n) }
            editingEarlier = false
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
private fun TodayCard(state: TrackerState, actions: TrackerActions) {
    val c = Salah.colors
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today", style = MaterialTheme.typography.titleLarge, color = c.text, modifier = Modifier.weight(1f))
            EyebrowText("${state.prayedOn(state.today)} of 5 prayed")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Prayer.fard.forEach { p ->
                val done = state.isPrayed(state.today, p)
                Column(
                    Modifier.clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Checkbox, onClickLabel = if (done) "Undo" else "Mark prayed") { actions.togglePrayed(state.today, p) }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .then(if (done) Modifier.background(c.accent) else Modifier.border(1.dp, c.accentHairline, CircleShape)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(SalahIcons.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(22.dp))
                        else Text(p.arabicName, style = ArabicStyle.copy(fontSize = 13.sp, lineHeight = 20.sp), color = c.textMuted)
                    }
                    Text(
                        p.displayName, style = MaterialTheme.typography.bodySmall, color = if (done) c.accent else c.text,
                    )
                }
            }
        }
    }
}

@Composable
private fun QadaCard(state: TrackerState, actions: TrackerActions, onEditEarlier: () -> Unit) {
    val c = Salah.colors
    Card {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                EyebrowText("Qada to make up")
                Text("${state.totalOwed}", fontFamily = Cormorant, fontSize = 56.sp, lineHeight = 60.sp, color = c.accent)
            }
            Text(
                if (state.totalOwed == 1) "prayer" else "prayers",
                style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        Prayer.fard.forEach { p -> QadaRow(p, state.qada.getValue(p), actions) }
        Text(
            "Add missed prayers from before",
            style = MaterialTheme.typography.labelLarge, color = c.accent,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onEditEarlier)
                .padding(vertical = 10.dp, horizontal = 2.dp),
        )
    }
}

@Composable
private fun QadaRow(prayer: Prayer, count: QadaCount, actions: TrackerActions) {
    val c = Salah.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(prayer.displayName, style = MaterialTheme.typography.titleMedium, color = c.text)
                Text("${count.owed} owed", style = MaterialTheme.typography.labelLarge, color = if (count.owed > 0) c.accent else c.textMuted)
            }
            val parts = buildList {
                add("${count.missed} missed")
                if (count.added != 0) add("${count.added} from before")
                add("${count.madeUp} made up")
            }
            Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        IconButton(onClick = { actions.undoMadeUp(prayer) }, enabled = count.madeUp > 0) {
            Icon(
                SalahIcons.Minus, contentDescription = "Undo last made-up ${prayer.displayName}",
                tint = if (count.madeUp > 0) c.textMuted else c.textMuted.copy(alpha = 0.3f), modifier = Modifier.size(18.dp),
            )
        }
        val enabled = count.owed > 0
        Row(
            Modifier.clip(RoundedCornerShape(18.dp))
                .then(if (enabled) Modifier.background(c.accent) else Modifier.border(1.dp, c.hairline, RoundedCornerShape(18.dp)))
                .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Log one made-up ${prayer.displayName}") { actions.logMadeUp(prayer) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(SalahIcons.Plus, contentDescription = null, tint = if (enabled) c.onAccent else c.textMuted, modifier = Modifier.size(14.dp))
            Text("Made up", style = MaterialTheme.typography.labelLarge, color = if (enabled) c.onAccent else c.textMuted)
        }
    }
}

@Composable
private fun HistoryCard(state: TrackerState, actions: TrackerActions) {
    val c = Salah.colors
    val days = (1..HISTORY_DAYS).map { state.today.minusDays(it) }.filter { !it.isBefore(state.since) }
    Card {
        Text("Last week", style = MaterialTheme.typography.titleLarge, color = c.text)
        if (days.isEmpty()) {
            Text("Past days will appear here from tomorrow.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            return@Card
        }
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Prayer.fard.forEach { p ->
                Text(
                    p.displayName.take(1), style = MaterialTheme.typography.labelMedium, color = c.textMuted,
                    modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        days.forEach { day ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(day.format(dayLabel), style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
                Prayer.fard.forEach { p ->
                    val done = state.isPrayed(day, p)
                    IconButton(onClick = { actions.togglePrayed(day, p) }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            if (done) SalahIcons.Check else SalahIcons.Circle,
                            contentDescription = "${p.displayName}, ${day.format(dayLabel)}: " + if (done) "prayed" else "missed",
                            tint = if (done) c.accent else c.text.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EarlierDialog(qada: Map<Prayer, QadaCount>, onDismiss: () -> Unit, onSave: (Map<Prayer, Int>) -> Unit) {
    val c = Salah.colors
    val values = remember { mutableStateMapOf<Prayer, String>().apply { Prayer.fard.forEach { put(it, qada.getValue(it).added.toString()) } } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Missed before tracking", style = MaterialTheme.typography.titleLarge, color = c.text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "If you owe prayers from before you started tracking, enter how many of each. They are added to your Qada.",
                    style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                )
                Prayer.fard.forEach { p ->
                    OutlinedTextField(
                        value = values[p].orEmpty(),
                        onValueChange = { v -> values[p] = v.filter { it.isDigit() }.take(6) },
                        singleLine = true,
                        label = { Text(p.displayName) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(Prayer.fard.associateWith { values[it]?.toIntOrNull() ?: 0 }) }) {
                Text("Save", color = c.accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
    )
}
