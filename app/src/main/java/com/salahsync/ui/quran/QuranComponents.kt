package com.salahsync.ui.quran

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.core.quran.QuranText
import com.salahsync.quran.QuranPrefs
import com.salahsync.quran.TranslationMode
import com.salahsync.quran.audio.Reciter
import com.salahsync.quran.audio.Reciters
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah

/** An eight-pointed star outline with a number inside, as used for surah and ayah numbers. */
@Composable
fun StarBadge(number: Int, modifier: Modifier = Modifier, size: Dp = 40.dp, color: Color = Salah.colors.accent) {
    val c = Salah.colors
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val cx = this.size.width / 2
            val cy = this.size.height / 2
            val outer = this.size.minDimension / 2 - 1.dp.toPx()
            val inner = outer * 0.72f
            val p = Path()
            for (i in 0 until 16) {
                val r = if (i % 2 == 0) outer else inner
                val a = Math.toRadians(i * 22.5 - 90.0)
                val x = cx + r * Math.cos(a).toFloat()
                val y = cy + r * Math.sin(a).toFloat()
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close()
            drawPath(p, color.copy(alpha = 0.7f), style = Stroke(1.dp.toPx()))
        }
        Text(
            "$number",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = if (number > 99) 10.sp else 12.sp),
            color = c.text,
        )
    }
}

/** A small rounded action with an icon and label. */
@Composable
fun Pill(
    label: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = Salah.colors
    val shape = RoundedCornerShape(20.dp)
    val fg = if (filled) c.onAccent else c.accent
    Row(
        modifier.heightIn(min = 40.dp).clip(shape)
            .then(if (filled) Modifier.background(c.accent) else Modifier.border(1.dp, c.accentHairline, shape))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/** A row of options where one is chosen, like Surahs / Juz / Bookmarks. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, modifier: Modifier = Modifier, onSelect: (T) -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.fillMaxWidth().height(44.dp).clip(shape).border(1.dp, c.hairline, shape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier.weight(1f).fillMaxWidth().height(36.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (on) c.accent.copy(alpha = 0.16f) else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab) { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), style = MaterialTheme.typography.labelLarge, color = if (on) c.accent else c.textMuted)
            }
        }
    }
}

/** A translation with its footnote markers drawn small and raised, in gold. */
fun translationText(text: String, accent: Color): AnnotatedString = buildAnnotatedString {
    QuranText.segments(text).forEach { (run, isRef) ->
        if (isRef) {
            withStyle(SpanStyle(color = accent, fontSize = 10.sp, baselineShift = BaselineShift.Superscript, fontWeight = FontWeight.SemiBold)) {
                append(run)
            }
        } else {
            append(run)
        }
    }
}

@Composable
fun ReciterDialog(selected: Reciter, onDismiss: () -> Unit, onSelect: (Reciter) -> Unit) {
    val c = Salah.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Reciter", style = MaterialTheme.typography.titleLarge, color = c.text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Reciters.all.forEach { r ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .selectable(selected = r == selected, role = Role.RadioButton) { onSelect(r) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = r == selected, onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.textMuted),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.name, style = MaterialTheme.typography.bodyLarge, color = c.text)
                            Text(
                                if (r == Reciters.default) "${r.style} · short surahs included offline" else r.style,
                                style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                            )
                        }
                        Text(r.arabicName, style = ArabicStyle.copy(fontSize = 15.sp, lineHeight = 26.sp), color = c.textMuted)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = c.accent) } },
    )
}

/** Translation and text size for the reader. */
@Composable
fun DisplayDialog(
    prefs: QuranPrefs,
    onMode: (TranslationMode) -> Unit,
    onSize: (Int) -> Unit,
    onWordByWord: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Salah.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Reading", style = MaterialTheme.typography.titleLarge, color = c.text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Translation", style = MaterialTheme.typography.labelLarge, color = c.accent)
                TranslationMode.entries.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp)
                            .selectable(selected = m == prefs.mode, role = Role.RadioButton) { onMode(m) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = m == prefs.mode, onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.textMuted),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(m.label, style = MaterialTheme.typography.bodyLarge, color = c.text)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .clickable(role = Role.Switch) { onWordByWord(!prefs.wordByWord) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Word by word", style = MaterialTheme.typography.bodyLarge, color = c.text)
                        Text("English meaning under each Arabic word", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = prefs.wordByWord, onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = c.onAccent, checkedTrackColor = c.accent,
                            uncheckedThumbColor = c.textMuted, uncheckedTrackColor = c.surfaceDeep, uncheckedBorderColor = c.accentHairline,
                        ),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Arabic size", style = MaterialTheme.typography.labelLarge, color = c.accent)
                Text(
                    "بِسْمِ ٱللَّهِ",
                    style = ArabicStyle.copy(fontSize = prefs.arabicSize.sp, lineHeight = (prefs.arabicSize * 1.8f).sp),
                    color = c.text,
                    modifier = Modifier.fillMaxWidth(),
                )
                Slider(
                    value = prefs.arabicSize.toFloat(),
                    onValueChange = { onSize(it.toInt()) },
                    valueRange = QuranPrefs.arabicSizes.first.toFloat()..QuranPrefs.arabicSizes.last.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.hairline),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = c.accent) } },
    )
}
