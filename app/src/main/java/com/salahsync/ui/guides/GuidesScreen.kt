package com.salahsync.ui.guides

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.core.guides.Guide
import com.salahsync.core.guides.GuideStep
import com.salahsync.core.guides.Guides
import com.salahsync.core.guides.Madhhab
import com.salahsync.core.guides.PrayerRakats
import com.salahsync.core.guides.RakatKind
import com.salahsync.core.guides.RakatTiming
import com.salahsync.core.guides.Rakats
import com.salahsync.core.guides.Ruling
import com.salahsync.ui.components.ChoiceChips
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GoldDivider
import com.salahsync.ui.components.HadithReferenceCard
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah

enum class GuideTab(val displayName: String) { WUDU("Wudu"), GHUSL("Ghusl"), RAKATS("Rakats") }

/** Wudu, ghusl and rak'ah counts, with a Hanafi or Shafi'i view. [defaultMadhhab] follows the Asr setting. */
@Composable
fun GuidesScreen(defaultMadhhab: Madhhab) {
    val c = Salah.colors
    var tab by rememberSaveable { mutableStateOf(GuideTab.WUDU) }
    var madhhab by rememberSaveable { mutableStateOf(defaultMadhhab) }
    var info by remember { mutableStateOf<GuideStep?>(null) }

    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EyebrowText("Step by step")
            Text("Worship guides", style = MaterialTheme.typography.headlineLarge, color = c.text)
        }
        ChoiceChips(GuideTab.entries, tab, { it.displayName }, { tab = it })
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EyebrowText("School", modifier = Modifier.padding(start = 4.dp))
            ChoiceChips(Madhhab.entries, madhhab, { it.displayName }, { madhhab = it })
        }

        when (tab) {
            GuideTab.WUDU -> GuideContent(Guides.wudu, madhhab) { info = it }
            GuideTab.GHUSL -> GuideContent(Guides.ghusl, madhhab) { info = it }
            GuideTab.RAKATS -> RakatsContent(madhhab)
        }

        Text(
            "Rulings follow ${madhhab.manual}. For a question about your own situation, ask a scholar you trust.",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = c.textMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
    }

    info?.let { s -> StepSourceDialog(s, madhhab) { info = null } }
}

@Composable
private fun GuideContent(guide: Guide, madhhab: Madhhab, onInfo: (GuideStep) -> Unit) {
    val c = Salah.colors
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
        Text(guide.title, style = MaterialTheme.typography.titleLarge, color = c.text)
        Text(guide.arabicName, style = ArabicStyle.copy(fontSize = 20.sp), color = c.accent)
    }
    Text(guide.intro, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(horizontal = 4.dp))
    guide.madhhabNote[madhhab]?.let { Note(it) }
    guide.steps.forEachIndexed { i, s -> StepCard(i + 1, s, madhhab) { onInfo(s) } }
}

@Composable
private fun Note(text: String) {
    val c = Salah.colors
    val shape = RoundedCornerShape(16.dp)
    Text(
        text, style = MaterialTheme.typography.bodySmall, color = c.text,
        modifier = Modifier.fillMaxWidth().clip(shape).background(c.surfaceDeep).border(1.dp, c.accentHairline, shape).padding(14.dp),
    )
}

@Composable
private fun StepCard(number: Int, step: GuideStep, madhhab: Madhhab, onInfo: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(22.dp)
    val ruling = step.ruling.getValue(madhhab)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.hairline, shape)
            .padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.padding(top = 2.dp).size(30.dp).clip(CircleShape).border(1.dp, c.accentHairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = MaterialTheme.typography.labelLarge, color = c.accent)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(step.title, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f, fill = false))
                RulingBadge(ruling)
            }
            step.arabic?.let {
                Text(it, style = ArabicStyle.copy(fontSize = 20.sp, lineHeight = 38.sp), color = c.text, modifier = Modifier.fillMaxWidth().padding(end = 8.dp))
            }
            Text(step.detail, style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(end = 8.dp))
            step.madhhabNote[madhhab]?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.accent, modifier = Modifier.padding(end = 8.dp))
            }
        }
        if (step.references.isNotEmpty() || step.quran != null) {
            IconButton(onClick = onInfo) {
                Icon(SalahIcons.Info, contentDescription = "Source of ${step.title}", tint = c.accent, modifier = Modifier.size(20.dp))
            }
        } else {
            Box(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun RulingBadge(ruling: Ruling) {
    val c = Salah.colors
    val shape = RoundedCornerShape(10.dp)
    val fard = ruling == Ruling.FARD
    Text(
        ruling.displayName, style = MaterialTheme.typography.labelSmall,
        color = if (fard) c.onAccent else c.textMuted,
        modifier = Modifier.clip(shape)
            .then(if (fard) Modifier.background(c.accent) else Modifier.border(1.dp, c.hairline, shape))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun StepSourceDialog(step: GuideStep, madhhab: Madhhab, onDismiss: () -> Unit) {
    val c = Salah.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(step.title, style = MaterialTheme.typography.titleLarge, color = c.text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EyebrowText("${step.ruling.getValue(madhhab).displayName} in the ${madhhab.displayName} school", color = c.accent)
                step.quran?.let { Text("Quran: $it", style = MaterialTheme.typography.bodyMedium, color = c.text) }
                if (step.references.isNotEmpty()) {
                    GoldDivider()
                    EyebrowText(if (step.references.size > 1) "References" else "Reference", color = c.accent)
                    step.references.forEach { HadithReferenceCard(it) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = c.accent) } },
    )
}

@Composable
private fun RakatsContent(madhhab: Madhhab) {
    val c = Salah.colors
    Text(
        "The fard is the same in every school. The sunnah prayers around it are shown in the order you pray them.",
        style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
    Rakats.forMadhhab(madhhab).forEach { PrayerRakatsCard(it) }
    Legend()
    EyebrowText("References", color = c.accent, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
    Rakats.references.forEach { (what, ref) ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(what, style = MaterialTheme.typography.bodySmall, color = c.text, modifier = Modifier.padding(horizontal = 4.dp))
            HadithReferenceCard(ref)
        }
    }
}

@Composable
private fun PrayerRakatsCard(p: PrayerRakats) {
    val c = Salah.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.hairline, shape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(p.name, style = MaterialTheme.typography.titleLarge, color = c.text)
            Text(p.arabicName, style = ArabicStyle.copy(fontSize = 18.sp), color = c.accent, modifier = Modifier.weight(1f))
            EyebrowText("${p.total} in all")
        }
        p.sets.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val main = s.kind == RakatKind.FARD || s.kind == RakatKind.WAJIB
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .then(if (main) Modifier.background(c.accent) else Modifier.border(1.dp, c.accentHairline, CircleShape)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${s.rakats}", style = MaterialTheme.typography.titleMedium, color = if (main) c.onAccent else c.accent)
                }
                Column(Modifier.weight(1f)) {
                    Text(s.note ?: s.kind.displayName, style = MaterialTheme.typography.bodyLarge, color = c.text)
                    val timing = when (s.timing) {
                        RakatTiming.BEFORE -> "Before the fard"
                        RakatTiming.MAIN -> "The obligatory prayer"
                        RakatTiming.AFTER -> "After the fard"
                    }
                    Text(if (s.note != null) "${s.kind.displayName} · $timing" else timing, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
            }
        }
        p.note?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = c.textMuted) }
    }
}

@Composable
private fun Legend() {
    val c = Salah.colors
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Fard: obligatory. Wajib: binding, just below fard (Hanafi).", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        Text("Sunnah mu'akkadah: the Prophet ﷺ kept to it regularly.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        Text("Sunnah ghayr mu'akkadah: prayed at times; rewarded, not stressed.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
}
