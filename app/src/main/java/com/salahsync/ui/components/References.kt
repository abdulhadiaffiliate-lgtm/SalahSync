package com.salahsync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.salahsync.core.adhkar.HadithRef
import com.salahsync.ui.theme.Salah

/** One hadith source: book and number, narrator, grade, and a link to sunnah.com. */
@Composable
fun HadithReferenceCard(r: HadithRef, modifier: Modifier = Modifier) {
    val c = Salah.colors
    val uri = LocalUriHandler.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).border(1.dp, c.hairline, shape).padding(14.dp),
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

/** A row of pill-shaped choices, one selected, that scrolls sideways when it does not fit. */
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val c = Salah.colors
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o == selected
            val shape = RoundedCornerShape(18.dp)
            Text(
                label(o), style = MaterialTheme.typography.labelLarge,
                color = if (on) c.onAccent else c.textMuted,
                modifier = Modifier.clip(shape)
                    .then(if (on) Modifier.background(c.accent) else Modifier.border(1.dp, c.hairline, shape))
                    .clickable(role = Role.Tab) { onSelect(o) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}
