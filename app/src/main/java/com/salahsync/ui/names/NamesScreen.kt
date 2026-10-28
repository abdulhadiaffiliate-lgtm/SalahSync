package com.salahsync.ui.names

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.core.names.AllahName
import com.salahsync.core.names.AllahNames
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GeometricPattern
import com.salahsync.ui.components.GoldDivider
import com.salahsync.ui.components.HadithReferenceCard
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.quran.StarBadge
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Cormorant
import com.salahsync.ui.theme.Salah
import java.time.LocalDate

/**
 * The 99 Names of Allah: today's name in a card at the top, then every name with its
 * transliteration and meaning, searchable. Everything is bundled, so it works offline.
 */
@Composable
fun NamesScreen(today: LocalDate) {
    val c = Salah.colors
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(query) { AllahNames.search(query) }
    Box(Modifier.fillMaxSize().background(c.background)) {
        GeometricPattern(Modifier.fillMaxWidth().height(260.dp))
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EyebrowText("Al-Asma ul-Husna")
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("99 Names of Allah", style = MaterialTheme.typography.headlineLarge, color = c.text)
                        Text("الأسماء الحسنى", style = ArabicStyle.copy(fontSize = 18.sp), color = c.accent)
                    }
                }
            }
            item { NameOfTheDay(AllahNames.forDate(today), Modifier.padding(top = 6.dp)) }
            item { SearchField(query, { query = it }, Modifier.padding(top = 8.dp, bottom = 4.dp)) }
            items(shown, key = { it.number }) { NameRow(it) }
            if (shown.isEmpty()) {
                item {
                    Text(
                        "No name matches “$query”.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), textAlign = TextAlign.Center,
                    )
                }
            }
            item { About(Modifier.padding(top = 14.dp)) }
        }
    }
}

@Composable
private fun NameOfTheDay(name: AllahName, modifier: Modifier = Modifier) {
    val c = Salah.colors
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.card).padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EyebrowText("Name of the day · ${name.number} of 99", color = c.onCardMuted)
        Text(name.arabic, style = ArabicStyle.copy(fontSize = 44.sp, lineHeight = 72.sp), color = c.onCard, textAlign = TextAlign.Center)
        GoldDivider(color = c.onCardMuted)
        Text(
            name.transliteration, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 28.sp, lineHeight = 32.sp,
            color = c.onCard, textAlign = TextAlign.Center,
        )
        Text(name.meaning, style = MaterialTheme.typography.bodyLarge, color = c.onCardMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NameRow(name: AllahName) {
    val c = Salah.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).clip(shape).background(c.text.copy(alpha = 0.03f))
            .border(1.dp, c.hairline, shape).padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StarBadge(name.number)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name.transliteration, fontFamily = Cormorant, fontSize = 21.sp, lineHeight = 25.sp, color = c.text)
            Text(name.meaning, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Text(name.arabic, style = ArabicStyle.copy(fontSize = 24.sp, lineHeight = 40.sp), color = c.accent, textAlign = TextAlign.End)
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Salah.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.fillMaxWidth().height(48.dp).clip(shape).background(c.text.copy(alpha = 0.04f)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(SalahIcons.Search, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Search by name, meaning or number", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.text),
                cursorBrush = SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search the names" },
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                SalahIcons.Close, contentDescription = "Clear search", tint = c.textMuted,
                modifier = Modifier.size(18.dp).clickable(role = Role.Button) { onChange("") },
            )
        }
    }
}

/** Where the names come from, under the list. */
@Composable
private fun About(modifier: Modifier = Modifier) {
    val c = Salah.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(Brush.linearGradient(listOf(c.surface, c.surfaceDeep)))
            .border(1.dp, c.accentHairline, shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "“Allah has ninety-nine names, one hundred minus one, and whoever knows them will go to Paradise.”",
            fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 20.sp, lineHeight = 25.sp, color = c.text,
        )
        HadithReferenceCard(AllahNames.source)
        Text(
            "The names and their order follow the well-known list narrated in Jami' at-Tirmidhi 3507, with Al-Ahad added " +
                "after Al-Wahid as in most lists. Scholars grade that listing weak, so lists differ slightly, " +
                "while the hadith above, that Allah has ninety-nine names, is authentic.",
            style = MaterialTheme.typography.bodySmall, color = c.textMuted,
        )
    }
}
