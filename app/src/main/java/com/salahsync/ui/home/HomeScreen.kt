package com.salahsync.ui.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.core.DayTimes
import com.salahsync.core.HijriDate
import com.salahsync.core.Prayer
import com.salahsync.data.DailyContent
import com.salahsync.quran.Translation
import com.salahsync.ui.quran.ShareCardDialog
import com.salahsync.ui.components.CountdownRing
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GeometricPattern
import com.salahsync.ui.components.GoldDivider
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Cormorant
import com.salahsync.ui.theme.Salah
import com.salahsync.ui.util.formatCountdown
import com.salahsync.ui.util.formatTime
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

data class NextPrayer(val prayer: Prayer, val label: String, val arabic: String, val at: ZonedDateTime, val progress: Float)

/** Works out the next prayer across midnight, and how far through the current interval we are. */
fun nextPrayer(now: ZonedDateTime, today: DayTimes?, tomorrow: DayTimes?): NextPrayer? {
    today ?: return null
    val instant = now.toInstant()
    val next = today.next(instant)
    val (prayer, at, times) = if (next != null) Triple(next, today[next], today)
    else tomorrow?.let { Triple(Prayer.FAJR, it[Prayer.FAJR], it) } ?: return null
    val previousAt = today.current(instant)?.let { today[it] } ?: today[Prayer.FAJR].minusHours(8)
    val total = Duration.between(previousAt, at).seconds.coerceAtLeast(1)
    val elapsed = Duration.between(previousAt, now).seconds
    return NextPrayer(prayer, prayer.labelOn(times.isFriday), prayer.arabicLabelOn(times.isFriday), at, elapsed.toFloat() / total)
}

@Composable
fun HomeScreen(
    name: String,
    streak: Int,
    now: ZonedDateTime,
    hijri: HijriDate,
    asrLabel: String,
    today: DayTimes?,
    tomorrow: DayTimes?,
    onOpenQibla: () -> Unit,
    onOpenTasbih: () -> Unit,
    onOpenQuran: () -> Unit,
    onOpenComingSoon: (String) -> Unit,
    onOpenPrayers: () -> Unit,
) {
    val c = Salah.colors
    val next = nextPrayer(now, today, tomorrow)
    val ayah = DailyContent.ayahFor(now.toLocalDate())
    var sharing by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(c.background)) {
        GeometricPattern(Modifier.fillMaxWidth().height(320.dp))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 120.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val day = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    EyebrowText("$day · $hijri")
                    Text("Assalamu alaikum,", style = MaterialTheme.typography.headlineLarge, color = c.text)
                    if (name.isNotBlank()) {
                        Text(
                            name.substringBefore(' '),
                            style = MaterialTheme.typography.headlineLarge.copy(fontStyle = FontStyle.Italic),
                            color = c.accent,
                        )
                    }
                }
                Row(
                    Modifier.padding(top = 4.dp).height(44.dp).clip(RoundedCornerShape(22.dp))
                        .border(1.dp, c.accentHairline, RoundedCornerShape(22.dp)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(SalahIcons.Flame, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                    Text("$streak", style = MaterialTheme.typography.labelLarge, color = c.text)
                }
            }

            Spacer(Modifier.height(28.dp))
            NextPrayerCard(next, now, asrLabel, onOpenPrayers)

            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Quran", SalahIcons.Book, Modifier.weight(1f), onOpenQuran)
                QuickAction("Qibla", SalahIcons.Compass, Modifier.weight(1f), onOpenQibla)
                QuickAction("Tasbih", SalahIcons.Beads, Modifier.weight(1f), onOpenTasbih)
                QuickAction("Feelings", SalahIcons.Heart, Modifier.weight(1f)) { onOpenComingSoon("Feelings") }
            }

            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Ayah of the Day", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                Text(
                    "Share", style = MaterialTheme.typography.bodyMedium, color = c.accent,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { sharing = true }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                )
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.card).padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(ayah.arabic, style = ArabicStyle.copy(fontSize = 25.sp, lineHeight = 46.sp), color = c.onCard, textAlign = TextAlign.Center)
                GoldDivider(color = c.onCardMuted)
                Text(
                    "“${ayah.english}”",
                    fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 19.sp, lineHeight = 25.sp,
                    color = c.onCard, textAlign = TextAlign.Center,
                )
                EyebrowText(ayah.reference, color = c.onCardMuted)
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp, c.hairline, RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(SalahIcons.Star, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    EyebrowText("Today's good deed")
                    Text(DailyContent.goodDeedFor(now.toLocalDate()), style = MaterialTheme.typography.bodyMedium, color = c.text)
                }
            }
        }
    }
    if (sharing) AyahShare(ayah) { sharing = false }
}

@Composable
private fun AyahShare(ayah: com.salahsync.data.Ayah, onDismiss: () -> Unit) {
    ShareCardDialog(ayah.arabic, mapOf(Translation.ENGLISH to ayah.english), ayah.reference, onDismiss)
}

@Composable
private fun NextPrayerCard(next: NextPrayer?, now: ZonedDateTime, asrLabel: String, onClick: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(c.surface, c.surfaceDeep)))
            .border(1.dp, c.accentHairline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (next == null) {
            Text("Set your location to see prayer times.", style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            return@Row
        }
        val left = Duration.between(now, next.at)
        CountdownRing(next.progress) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatCountdown(left), fontFamily = Cormorant, fontSize = 30.sp, color = c.text)
                EyebrowText(if (left.toHours() >= 1) "hrs left" else "min left", modifier = Modifier.padding(top = 2.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EyebrowText("Next prayer", color = c.accent)
            Text(next.label, style = MaterialTheme.typography.displayMedium, color = c.text)
            Text(next.arabic, style = ArabicStyle, color = c.textMuted)
            val suffix = if (next.prayer == Prayer.ASR) " · $asrLabel" else ""
            Text("${formatTime(next.at)}$suffix", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.heightIn(min = 78.dp).clip(shape).background(c.text.copy(alpha = 0.03f))
            .border(1.dp, c.hairline, shape).clickable(role = Role.Button, onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.text)
    }
}
