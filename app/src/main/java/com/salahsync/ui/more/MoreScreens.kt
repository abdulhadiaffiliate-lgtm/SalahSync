package com.salahsync.ui.more

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salahsync.BuildConfig
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GeometricPattern
import com.salahsync.ui.components.GoldDivider
import com.salahsync.ui.components.IconBadge
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Cormorant
import com.salahsync.ui.theme.Salah

@Composable
fun MoreScreen(
    onTasbih: () -> Unit,
    onAdhkar: () -> Unit,
    onDuas: () -> Unit,
    onGuides: () -> Unit,
    onTracker: () -> Unit,
    onQuran: () -> Unit,
    onHadith: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    val c = Salah.colors
    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("More", style = MaterialTheme.typography.headlineLarge, color = c.text, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Section("Pray and remember")
        Entry("Prayer tracker", "Daily prayers and Qada, on this phone only", SalahIcons.Check, onTracker)
        Entry("Morning and evening adhkar", "With the hadith source for each", SalahIcons.Heart, onAdhkar)
        Entry("Tasbih", "Dhikr counter: 33, 33, 34", SalahIcons.Beads, onTasbih)
        Entry("Duas", "Daily supplications from the Sunnah", SalahIcons.Star, onDuas)
        Section("Read and learn")
        Entry("Quran", "Read, listen and keep your place", SalahIcons.Book, onQuran)
        Entry("Hadith", "Bukhari, Muslim, Abu Dawud and Tirmidhi", SalahIcons.Notes, onHadith)
        Entry("Wudu, Ghusl and Rakats", "Step-by-step guides, Hanafi or Shafi'i", SalahIcons.Drop, onGuides)
        Section("SalahSync")
        Entry("Settings", "Prayer times, alerts, appearance", SalahIcons.Sliders, onSettings)
        Entry("About", "Why it was made, privacy and sources", SalahIcons.Shield, onAbout)
    }
}

@Composable
private fun Section(title: String) {
    EyebrowText(title, color = Salah.colors.accent, modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 2.dp))
}

@Composable
private fun Entry(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val c = Salah.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(shape).border(1.dp, c.hairline, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconBadge(icon, size = 42.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Icon(SalahIcons.ChevronRight, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun ComingSoonScreen(feature: String) {
    val c = Salah.colors
    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("إِنْ شَاءَ ٱللَّهُ", style = ArabicStyle, color = c.accent)
        Spacer(Modifier.height(8.dp))
        Text(feature, style = MaterialTheme.typography.headlineLarge, color = c.text)
        Spacer(Modifier.height(8.dp))
        Text(
            "This is coming in a future update of SalahSync.",
            style = MaterialTheme.typography.bodyLarge, color = c.textMuted, textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AboutScreen() {
    val c = Salah.colors
    Box(Modifier.fillMaxSize().background(c.background)) {
        GeometricPattern(Modifier.fillMaxWidth().height(260.dp))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AboutContent()
        }
    }
}

@Composable
private fun AboutContent() {
    val c = Salah.colors
    EyebrowText("Version ${BuildConfig.VERSION_NAME}", color = c.accent)
    Text("Made with care,\nfor your salah", style = MaterialTheme.typography.headlineLarge, color = c.text)
    Text(
        "SalahSync was made by one person, not a company, for one simple purpose: to help Muslims keep their five daily prayers, " +
            "on time, every day. Everything else in the app, from the Quran to the duas, is there to bring you a little closer to that.",
        style = MaterialTheme.typography.bodyLarge, color = c.textMuted,
    )

    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.accentHairline, shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Promise(SalahIcons.Star, "Free, with no ads", "Nothing to buy, nothing to unlock, and no adverts between you and your prayer.")
        Promise(SalahIcons.Shield, "No account, no tracking", "No sign-up and no analytics. SalahSync has no servers of its own.")
        Promise(SalahIcons.Check, "Your prayer record stays yours", "Your prayer history and Qada are never uploaded, and are left out of backups.")
    }

    // The ayah and translation are copied verbatim from the bundled Tanzil text and Saheeh International (2:127).
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.card).padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EyebrowText("A small request", color = c.onCardMuted)
        Text(
            "رَبَّنَا تَقَبَّلْ مِنَّآ إِنَّكَ أَنتَ ٱلسَّمِيعُ ٱلْعَلِيمُ",
            style = ArabicStyle.copy(fontSize = 24.sp, lineHeight = 44.sp), color = c.onCard, textAlign = TextAlign.Center,
        )
        Text(
            "“Our Lord, accept [this] from us. Indeed, You are the Hearing, the Knowing.”",
            fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 19.sp, lineHeight = 25.sp,
            color = c.onCard, textAlign = TextAlign.Center,
        )
        EyebrowText("Al-Baqarah 2:127", color = c.onCardMuted)
        GoldDivider(color = c.onCardMuted)
        Text(
            "If SalahSync helps you pray, please remember the one who made it, and their family, in your duas. " +
                "And if it helps you, share it with someone you love. The Prophet ﷺ said: “Whoever guides someone to good " +
                "will have a reward like the one who does it.” (Sahih Muslim 1893)",
            style = MaterialTheme.typography.bodyMedium, color = c.onCard, textAlign = TextAlign.Center,
        )
    }

    Column(
        Modifier.fillMaxWidth().clip(shape).border(1.dp, c.hairline, shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("What goes online", style = MaterialTheme.typography.titleLarge, color = c.text)
        Text(
            "SalahSync only connects to the internet to fetch the AlAdhan prayer timetable for your location's coordinates " +
                "(you can turn this off in Settings), to play or download Quran recitations from EveryAyah, and to download " +
                "the hadith books you choose. Your name, settings and bookmarks are kept on this phone, and in your own Android " +
                "backup if you use one.",
            style = MaterialTheme.typography.bodyMedium, color = c.textMuted,
        )
    }

    EyebrowText("Sources", color = c.accent)
    Source("Prayer times", "AlAdhan API (aladhan.com) online, and the Adhan library by Batoul Apps (MIT License) offline.")
    Source("Hijri calendar", "Umm al-Qura calendar from java.time, with a local moon-sighting adjustment.")
    Source("Quranic text", "Tanzil Quran Text (Uthmani, version 1.1), copyright 2007-2021 Tanzil Project, tanzil.net. Creative Commons Attribution 3.0; reproduced verbatim.")
    Source("English translation", "Saheeh International, published by Noor International Center. From QuranEnc.com, version 1.1.2, reproduced verbatim.")
    Source("Urdu translation", "Maulana Muhammad Junagarhi, with notes by Hafiz Salahuddin Yusuf. From QuranEnc.com, version 1.1.3, reproduced verbatim.")
    Source("Recitations", "Streamed per ayah from EveryAyah.com. Mishary Rashid Alafasy's recitation of Al-Fatihah and the last 32 surahs is included for offline listening.")
    Source("Surah and juz data", "Tanzil Project quran-data metadata.")
    Source(
        "Hadith",
        "Sahih al-Bukhari, Sahih Muslim, Sunan Abi Dawud and Jami' at-Tirmidhi in Arabic, English and Urdu, from the hadith-api dataset by " +
            "Fawaz Ahmed (github.com/fawazahmed0/hadith-api), released under the Unlicense. Reproduced unchanged from one fixed version, " +
            "each file checked against its fingerprint. English: Muhsin Khan (Bukhari), Abdul Hamid Siddiqui (Muslim). Gradings for Abu Dawud " +
            "and Tirmidhi are as the dataset gives them. Muslim is numbered as on sunnah.com.",
    )
    Source("Adhkar", "Morning and evening adhkar from Sahih al-Bukhari, Sahih Muslim, Sunan Abi Dawud and Jami' at-Tirmidhi, with gradings as given on sunnah.com. Each dhikr's ⓘ shows its source.")
    Source("Duas", "Daily supplications quoted in the wording of Sahih al-Bukhari, Sahih Muslim, Sunan Abi Dawud, Jami' at-Tirmidhi and Sunan Ibn Majah, with gradings as given on sunnah.com.")
    Source("Wudu, Ghusl and Rakats", "Steps from the hadith of 'Uthman, 'Abdullah ibn Zayd, 'A'ishah and Maymunah in Sahih al-Bukhari. Rulings and rak'ah counts from Nur al-Idah (Hanafi) and Minhaj at-Talibin (Shafi'i).")

    EyebrowText("Open-source licenses", color = c.accent)
    Source("Adhan for Java", "Copyright Batoul Apps. MIT License.")
    Source("Cormorant Garamond", "Copyright 2015 The Cormorant Project Authors. SIL Open Font License 1.1.")
    Source("Manrope", "Copyright 2019 The Manrope Project Authors. SIL Open Font License 1.1.")
    Source("Amiri Quran", "Copyright 2010-2022 The Amiri Project Authors. SIL Open Font License 1.1.")
    Source("Noto Nastaliq Urdu", "Copyright 2022 The Noto Project Authors. SIL Open Font License 1.1.")
    Source("AndroidX Media3", "Copyright The Android Open Source Project. Apache License 2.0.")
    Source("AndroidX Room", "Copyright The Android Open Source Project. Apache License 2.0.")
    Source("AndroidX and Jetpack Compose", "Copyright The Android Open Source Project. Apache License 2.0.")
    Source("Kotlin and kotlinx.coroutines", "Copyright JetBrains s.r.o. Apache License 2.0.")
}

@Composable
private fun Promise(icon: ImageVector, title: String, body: String) {
    val c = Salah.colors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBadge(icon, size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(body, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
    }
}

@Composable
private fun Source(title: String, body: String) {
    val c = Salah.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = c.text)
        Text(body, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
}
