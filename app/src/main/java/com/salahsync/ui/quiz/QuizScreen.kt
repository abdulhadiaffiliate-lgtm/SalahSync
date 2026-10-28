package com.salahsync.ui.quiz

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.core.quiz.Quiz
import com.salahsync.core.quiz.QuizItem
import com.salahsync.core.quiz.QuizTopic
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.quran.Quran
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GeometricPattern
import com.salahsync.ui.components.GoldDivider
import com.salahsync.ui.components.HadithReferenceCard
import com.salahsync.ui.components.IconBadge
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Cormorant
import com.salahsync.ui.theme.Salah
import kotlin.random.Random

/** Shown for a wrong pick; the theme has no error colour, so one tuned to each theme. */
private val wrongColor: Color @Composable get() = if (Salah.colors.isDark) Color(0xFFE59A8A) else Color(0xFFA4473A)

/**
 * A short quiz on the basics: pick a topic, answer [Quiz.ROUND_SIZE] questions with the
 * source shown after each, then see the score. Everything is bundled, so it works offline.
 */
@Composable
fun QuizScreen(onOpenAyah: (AyahRef) -> Unit) {
    val c = Salah.colors
    // The round is rebuilt from its seed, so it survives rotation without being saved itself.
    var topicName by rememberSaveable { mutableStateOf<String?>(null) }
    var seed by rememberSaveable { mutableStateOf<Long?>(null) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var picks by rememberSaveable { mutableStateOf(intArrayOf()) }
    val topic = topicName?.let { QuizTopic.valueOf(it) }
    val round = remember(seed, topicName) { seed?.let { Quiz.round(topic, Random(it)) } }

    fun start() {
        seed = Random.nextLong()
        index = 0
        picks = intArrayOf()
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        GeometricPattern(Modifier.fillMaxWidth().height(260.dp))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EyebrowText("Test what you know")
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Islamic quiz", style = MaterialTheme.typography.headlineLarge, color = c.text)
                    Text("اختبر معلوماتك", style = ArabicStyle.copy(fontSize = 18.sp), color = c.accent)
                }
            }
            when {
                round == null -> TopicPicker(topic, onSelect = { topicName = it?.name }, onStart = ::start)
                index < round.size -> QuestionCard(
                    item = round[index],
                    number = index + 1,
                    total = round.size,
                    picked = picks.getOrNull(index),
                    onPick = { if (picks.size == index) picks += it },
                    onNext = { index++ },
                    onOpenAyah = onOpenAyah,
                )
                else -> ResultCard(
                    score = round.indices.count { picks.getOrNull(it) == round[it].answerIndex },
                    total = round.size,
                    topic = topic,
                    onAgain = ::start,
                    onChangeTopic = { seed = null },
                )
            }
        }
    }
}

@Composable
private fun TopicPicker(selected: QuizTopic?, onSelect: (QuizTopic?) -> Unit, onStart: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Brush.linearGradient(listOf(c.surface, c.surfaceDeep)))
            .border(1.dp, c.accentHairline, shape).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconBadge(SalahIcons.Quiz)
        Text(
            "${Quiz.ROUND_SIZE} questions a round",
            fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 24.sp, lineHeight = 28.sp, color = c.text,
        )
        Text(
            "See the answer straight away, with the ayah or hadith it comes from.",
            style = MaterialTheme.typography.bodyMedium, color = c.textMuted,
        )
    }
    EyebrowText("Choose a topic", color = c.accent, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
    TopicRow("Mixed", "Questions from every topic", "منوّع", selected == null) { onSelect(null) }
    QuizTopic.entries.forEach { t ->
        TopicRow(t.displayName, "${Quiz.inTopic(t).size} questions", t.arabicName, selected == t) { onSelect(t) }
    }
    PrimaryButton("Start", Modifier.padding(top = 6.dp), onStart)
}

@Composable
private fun TopicRow(title: String, subtitle: String, arabic: String, selected: Boolean, onClick: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape)
            .background(if (selected) c.accent.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (selected) c.accent else c.hairline, shape)
            .clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (selected) SalahIcons.Check else SalahIcons.Circle, contentDescription = null,
            tint = if (selected) c.accent else c.textMuted, modifier = Modifier.size(20.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Text(arabic, style = ArabicStyle.copy(fontSize = 16.sp), color = c.accent)
    }
}

@Composable
private fun QuestionCard(
    item: QuizItem,
    number: Int,
    total: Int,
    picked: Int?,
    onPick: (Int) -> Unit,
    onNext: () -> Unit,
    onOpenAyah: (AyahRef) -> Unit,
) {
    val c = Salah.colors
    val q = item.question
    val answered = picked != null
    val progress by animateFloatAsState((number - if (answered) 0 else 1).toFloat() / total, label = "progress")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.padding(horizontal = 4.dp)) {
            EyebrowText("Question $number of $total", color = c.accent, modifier = Modifier.weight(1f))
            EyebrowText(q.topic.displayName)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.hairline)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(c.accent))
        }
    }
    Text(
        q.question, fontFamily = Cormorant, fontSize = 26.sp, lineHeight = 31.sp, color = c.text,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
    item.options.forEachIndexed { i, option ->
        OptionRow(
            label = option,
            letter = "ABCD"[i].toString(),
            state = when {
                !answered -> OptionState.OPEN
                i == item.answerIndex -> OptionState.RIGHT
                i == picked -> OptionState.WRONG
                else -> OptionState.DIMMED
            },
            onClick = { onPick(i) },
        )
    }
    if (picked != null) {
        Feedback(item, correct = picked == item.answerIndex, onOpenAyah)
        PrimaryButton(if (number < total) "Next question" else "See my score", onClick = onNext)
    }
}

private enum class OptionState { OPEN, RIGHT, WRONG, DIMMED }

@Composable
private fun OptionRow(label: String, letter: String, state: OptionState, onClick: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(18.dp)
    val tone = when (state) {
        OptionState.RIGHT -> c.accent
        OptionState.WRONG -> wrongColor
        else -> c.hairline
    }
    val border by animateColorAsState(tone, label = "border")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape)
            .background(if (state == OptionState.RIGHT || state == OptionState.WRONG) tone.copy(alpha = 0.12f) else c.text.copy(alpha = 0.03f))
            .border(1.dp, border, shape)
            .clickable(enabled = state == OptionState.OPEN, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(15.dp)).border(1.dp, if (state == OptionState.OPEN || state == OptionState.DIMMED) c.accentHairline else tone, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                OptionState.RIGHT -> Icon(SalahIcons.Check, contentDescription = "Correct answer", tint = tone, modifier = Modifier.size(18.dp))
                OptionState.WRONG -> Icon(SalahIcons.Close, contentDescription = "Your answer", tint = tone, modifier = Modifier.size(14.dp))
                else -> Text(letter, style = MaterialTheme.typography.labelLarge, color = c.accent)
            }
        }
        Text(
            label, style = MaterialTheme.typography.bodyLarge,
            color = if (state == OptionState.DIMMED) c.textMuted else c.text, modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Feedback(item: QuizItem, correct: Boolean, onOpenAyah: (AyahRef) -> Unit) {
    val c = Salah.colors
    val q = item.question
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.accentHairline, shape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (correct) "Correct, masha'Allah" else "The answer is: ${q.answer}",
            style = MaterialTheme.typography.titleMedium, color = if (correct) c.accent else c.text,
        )
        Text(q.explanation, style = MaterialTheme.typography.bodyMedium, color = c.text)
        GoldDivider()
        EyebrowText(if (q.ayahs.size + q.hadith.size > 1) "Sources" else "Source", color = c.accent)
        q.ayahs.forEach { a ->
            val surah = Quran.surah(a.surah)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, c.hairline, RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) { onOpenAyah(a) }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Quran, ${surah.name} ${a.surah}:${a.ayah}", style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text("Open in the reader", style = MaterialTheme.typography.bodySmall, color = c.accent)
                }
                Icon(SalahIcons.ChevronRight, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
            }
        }
        q.hadith.forEach { HadithReferenceCard(it) }
    }
}

@Composable
private fun ResultCard(score: Int, total: Int, topic: QuizTopic?, onAgain: () -> Unit, onChangeTopic: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.card).padding(horizontal = 22.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EyebrowText(topic?.displayName ?: "Mixed", color = c.onCardMuted)
        Text("$score / $total", fontFamily = Cormorant, fontSize = 56.sp, color = c.onCard)
        GoldDivider(color = c.onCardMuted)
        Text(
            when {
                score == total -> "Excellent, every answer right. Masha'Allah!"
                score * 2 >= total -> "Well done. Read through the sources to learn the rest."
                else -> "A good start. Every question is a chance to learn."
            },
            fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 20.sp, lineHeight = 25.sp,
            color = c.onCard, textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(4.dp))
    PrimaryButton("Play again", onClick = onAgain)
    Text(
        "Choose another topic", style = MaterialTheme.typography.labelLarge, color = c.accent, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onChangeTopic)
            .padding(vertical = 14.dp),
    )
}

@Composable
private fun PrimaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Salah.colors
    Text(
        label, style = MaterialTheme.typography.labelLarge, color = c.onAccent, textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.accent)
            .clickable(role = Role.Button, onClick = onClick).padding(vertical = 16.dp),
    )
}
