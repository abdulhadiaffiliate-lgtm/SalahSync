package com.salahsync.ui.tasbih

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.ui.components.CountdownRing
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Cormorant
import com.salahsync.ui.theme.Salah

private data class Dhikr(val arabic: String, val english: String, val target: Int)

private val sequence = listOf(
    Dhikr("سُبْحَانَ ٱللَّٰهِ", "SubhanAllah", 33),
    Dhikr("ٱلْحَمْدُ لِلَّٰهِ", "Alhamdulillah", 33),
    Dhikr("ٱللَّٰهُ أَكْبَرُ", "Allahu Akbar", 34),
)

/** Tasbih after salah: 33, 33, 34, moving on to the next phrase automatically. */
@Composable
fun TasbihScreen() {
    val c = Salah.colors
    val haptics = LocalHapticFeedback.current
    var index by rememberSaveable { mutableIntStateOf(0) }
    var count by rememberSaveable { mutableIntStateOf(0) }
    var total by rememberSaveable { mutableIntStateOf(0) }
    val dhikr = sequence[index]
    val progress by animateFloatAsState(count / dhikr.target.toFloat(), tween(250), label = "tasbih")

    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding()
            .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                EyebrowText("Total today · $total")
                Text("Tasbih", style = MaterialTheme.typography.headlineLarge, color = c.text)
            }
            IconButton(onClick = { index = 0; count = 0; total = 0 }) {
                Icon(SalahIcons.Reset, contentDescription = "Reset counter", tint = c.textMuted, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sequence.forEachIndexed { i, d ->
                val selected = i == index
                Text(
                    d.english, style = MaterialTheme.typography.bodySmall,
                    color = if (selected) c.onAccent else c.textMuted,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                        .then(if (selected) Modifier.background(c.accent) else Modifier.border(1.dp, c.hairline, RoundedCornerShape(14.dp)))
                        .clickable(role = Role.Tab) { index = i; count = 0 }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(260.dp).clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    total++
                    if (count + 1 >= dhikr.target) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        count = 0
                        index = (index + 1) % sequence.size
                    } else {
                        count++
                    }
                }
                .semantics { contentDescription = "Count ${dhikr.english}, $count of ${dhikr.target}" },
            contentAlignment = Alignment.Center,
        ) {
            CountdownRing(progress, diameter = 260.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(dhikr.arabic, style = ArabicStyle.copy(fontSize = 30.sp, lineHeight = 48.sp), color = c.accent, textAlign = TextAlign.Center)
                    Text("$count", fontFamily = Cormorant, fontSize = 72.sp, color = c.text)
                    EyebrowText("of ${dhikr.target}")
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Tap anywhere on the circle to count", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
}
