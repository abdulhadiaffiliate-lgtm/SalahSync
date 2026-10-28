package com.salahsync.ui.names

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.core.names.AllahName
import com.salahsync.core.names.AllahNames
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GeometricPattern
import com.salahsync.ui.components.GoldDivider
import com.salahsync.ui.components.HadithReferenceCard
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Cormorant
import com.salahsync.ui.theme.Salah
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * The 99 Names of Allah: today's name in a card at the top, then every name as a hexagon tile
 * in a scrolling honeycomb. Tapping a tile opens its meaning in a sheet. Everything is bundled,
 * so it works offline.
 */
@Composable
fun NamesScreen(today: LocalDate) {
    val c = Salah.colors
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    // Honeycomb rows alternate three and two tiles.
    val rows = remember {
        buildList {
            var i = 0
            while (i < AllahNames.all.size) {
                val n = if (size % 2 == 0) 3 else 2
                add(AllahNames.all.subList(i, minOf(i + n, AllahNames.all.size)))
                i += n
            }
        }
    }
    Box(Modifier.fillMaxSize().background(c.background)) {
        GeometricPattern(Modifier.fillMaxWidth().height(260.dp))
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
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
            item { NameOfTheDay(AllahNames.forDate(today), Modifier.padding(top = 14.dp)) }
            item {
                Text(
                    "Tap a name to read its meaning", style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                )
            }
            itemsIndexed(rows, key = { _, row -> row.first().number }) { i, row ->
                HexRow(row, last = i == rows.lastIndex, selected = open) { open = it.number }
            }
            item { About(Modifier.padding(top = 22.dp)) }
        }
    }
    open?.let { number ->
        NameSheet(
            AllahNames.all[number - 1],
            onDismiss = { open = null },
            onStep = { step -> open = Math.floorMod(number - 1 + step, AllahNames.all.size) + 1 },
        )
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

/** A pointy-top hexagon filling its bounds. */
private val HexagonShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height * 0.25f)
    lineTo(size.width, size.height * 0.75f)
    lineTo(size.width / 2f, size.height)
    lineTo(0f, size.height * 0.75f)
    lineTo(0f, size.height * 0.25f)
    close()
}

private val HexGap = 6.dp
private const val HexHeightRatio = 1.1547f // 2 / sqrt(3)

/**
 * One row of the honeycomb. Tiles are sized so three fit across; a row of two is centred so its tiles
 * sit between the ones above. The row reports only its step height, so the next row tucks into the
 * notches, while the tiles themselves draw at full height.
 */
@Composable
private fun HexRow(names: List<AllahName>, last: Boolean, selected: Int?, onTap: (AllahName) -> Unit) {
    Layout(
        content = { names.forEach { HexTile(it, selected == it.number) { onTap(it) } } },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val gap = HexGap.roundToPx()
        val w = (constraints.maxWidth - 2 * gap) / 3
        val h = (w * HexHeightRatio).roundToInt()
        val step = ((w + gap) * 0.866f).roundToInt()
        val placeables = measurables.map { it.measure(Constraints.fixed(w, h)) }
        val rowWidth = placeables.size * w + (placeables.size - 1) * gap
        val start = (constraints.maxWidth - rowWidth) / 2
        layout(constraints.maxWidth, if (last) h else step) {
            placeables.forEachIndexed { i, p -> p.place(start + i * (w + gap), 0) }
        }
    }
}

@Composable
private fun HexTile(name: AllahName, selected: Boolean, onClick: () -> Unit) {
    val c = Salah.colors
    val longArabic = name.arabic.contains(' ')
    Column(
        Modifier.clip(HexagonShape)
            .background(if (selected) c.accentHairline else c.text.copy(alpha = 0.03f))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) c.accent else c.accentHairline, HexagonShape)
            .clickable(role = Role.Button, onClickLabel = "Read meaning", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "${name.number}. ${name.transliteration}" }
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("${name.number}", style = MaterialTheme.typography.labelSmall, color = c.accent)
        Text(
            name.arabic, color = c.accent, textAlign = TextAlign.Center, maxLines = 1,
            style = ArabicStyle.copy(fontSize = if (longArabic) 13.sp else 19.sp, lineHeight = if (longArabic) 26.sp else 32.sp),
        )
        Text(
            name.transliteration, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 14.sp,
            color = c.text, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The tapped name in full, with steps to the neighbouring names. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NameSheet(name: AllahName, onDismiss: () -> Unit, onStep: (Int) -> Unit) {
    val c = Salah.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface, contentColor = c.text) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            EyebrowText("${name.number} of 99", color = c.accent)
            Text(name.arabic, style = ArabicStyle.copy(fontSize = 44.sp, lineHeight = 76.sp), color = c.accent, textAlign = TextAlign.Center)
            GoldDivider()
            Text(
                name.transliteration, fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 30.sp, lineHeight = 34.sp,
                color = c.text, textAlign = TextAlign.Center,
            )
            Text(name.meaning, style = MaterialTheme.typography.bodyLarge, color = c.textMuted, textAlign = TextAlign.Center)
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { onStep(-1) }, border = BorderStroke(1.dp, c.accentHairline)) { Text("‹ Previous", color = c.text) }
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent)) {
                    Text("Close")
                }
                OutlinedButton(onClick = { onStep(1) }, border = BorderStroke(1.dp, c.accentHairline)) { Text("Next ›", color = c.text) }
            }
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
