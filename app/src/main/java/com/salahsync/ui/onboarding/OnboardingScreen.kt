package com.salahsync.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salahsync.data.Cities
import com.salahsync.data.SavedLocation
import com.salahsync.data.UserPrefs
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GeometricPattern
import com.salahsync.ui.components.IconBadge
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.settings.isIgnoringBatteryOptimizations
import com.salahsync.ui.settings.openBatterySettings
import com.salahsync.ui.settings.openExactAlarmSettings
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah

@Composable
fun OnboardingScreen(
    prefs: UserPrefs,
    locating: Boolean,
    canScheduleExact: () -> Boolean,
    onName: (String) -> Unit,
    onLocate: () -> Unit,
    onChooseCity: (SavedLocation) -> Unit,
    onFinish: () -> Unit,
) {
    val c = Salah.colors
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(prefs.name) }
    var showCities by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) onLocate() else showCities = true
    }
    var notificationsAsked by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAsked = true
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        GeometricPattern(Modifier.fillMaxWidth().height(360.dp))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(24.dp),
        ) {
            if (step > 0) {
                EyebrowText("Step $step of 3", color = c.accent)
                Spacer(Modifier.height(12.dp))
            }
            when (step) {
                0 -> {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(12.dp))
                        Text("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", style = ArabicStyle, color = c.accent)
                        Spacer(Modifier.height(8.dp))
                        Text("Welcome to SalahSync", style = MaterialTheme.typography.headlineLarge, color = c.text)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "A calm companion for your five daily prayers. Here is what's inside.",
                            style = MaterialTheme.typography.bodyLarge, color = c.textMuted,
                        )
                        Spacer(Modifier.height(24.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Highlight(SalahIcons.Clock, "Prayer times and adhan", "Accurate for where you are, and they work offline")
                            Highlight(SalahIcons.Book, "The Quran, to read and listen", "Arabic with English and Urdu, and 8 reciters")
                            Highlight(SalahIcons.Check, "Keep track of every prayer", "Qada, streaks and a weekly report")
                            Highlight(SalahIcons.Heart, "Adhkar, duas and hadith", "Each one with its source")
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    Text(
                        "Free, with no ads and no account.",
                        style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), textAlign = TextAlign.Center,
                    )
                    PrimaryButton("Get started", enabled = true) { step = 1 }
                }

                1 -> {
                    Text("What should we call you?", style = MaterialTheme.typography.headlineLarge, color = c.text)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "SalahSync uses it only to greet you, and never sends it anywhere.",
                        style = MaterialTheme.typography.bodyLarge, color = c.textMuted,
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = name, onValueChange = { name = it.take(40) },
                        label = { Text("Your name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { if (name.isNotBlank()) { onName(name); step = 2 } }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent, unfocusedBorderColor = c.accentHairline,
                            focusedLabelColor = c.accent, unfocusedLabelColor = c.textMuted,
                            focusedTextColor = c.text, unfocusedTextColor = c.text, cursorColor = c.accent,
                        ),
                    )
                    Spacer(Modifier.weight(1f))
                    PrimaryButton("Continue", enabled = name.isNotBlank()) { onName(name); step = 2 }
                }

                2 -> {
                    Text("Where do you pray?", style = MaterialTheme.typography.headlineLarge, color = c.text)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Prayer times are calculated on your phone from your location. It never leaves your device, except as coordinates for the AlAdhan timetable.",
                        style = MaterialTheme.typography.bodyLarge, color = c.textMuted,
                    )
                    Spacer(Modifier.height(24.dp))
                    prefs.location?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(SalahIcons.Pin, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                            Column {
                                Text(it.placeName, style = MaterialTheme.typography.titleLarge, color = c.text)
                                Text(
                                    "${prefs.method.displayName} · ${prefs.asrSchool.displayName} Asr",
                                    style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    SecondaryButton(if (locating) "Finding you…" else "Use my location", SalahIcons.Pin) {
                        locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    }
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton("Choose a city", SalahIcons.Sliders) { showCities = !showCities }
                    if (showCities) {
                        LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
                            items(Cities.all) { city ->
                                Text(
                                    city.placeName, style = MaterialTheme.typography.bodyLarge,
                                    color = if (city.placeName == prefs.location?.placeName) c.accent else c.text,
                                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) {
                                        onChooseCity(city); showCities = false
                                    }.padding(vertical = 14.dp, horizontal = 4.dp),
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    PrimaryButton("Continue", enabled = prefs.location != null) { step = 3 }
                }

                else -> {
                    Text("Never miss a prayer", style = MaterialTheme.typography.headlineLarge, color = c.text)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Android needs three permissions to play the adhan on time, even when your phone is asleep.",
                        style = MaterialTheme.typography.bodyLarge, color = c.textMuted,
                    )
                    Spacer(Modifier.height(24.dp))
                    if (Build.VERSION.SDK_INT >= 33) {
                        SecondaryButton(if (notificationsAsked) "Notifications: asked" else "Allow notifications", SalahIcons.Bell) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    if (!canScheduleExact()) {
                        SecondaryButton("Allow exact alarms", SalahIcons.Clock) { context.openExactAlarmSettings() }
                        Spacer(Modifier.height(10.dp))
                    }
                    SecondaryButton(
                        if (context.isIgnoringBatteryOptimizations()) "Battery: SalahSync is exempt" else "Exempt from battery optimization",
                        SalahIcons.Shield,
                    ) { context.openBatterySettings() }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "On Oppo, OnePlus, Realme and Xiaomi phones, also allow SalahSync to auto-start and run in the background in the phone's battery settings.",
                        style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                    )
                    Spacer(Modifier.weight(1f))
                    PrimaryButton("Start using SalahSync", enabled = true, onClick = onFinish)
                }
            }
        }
    }
}

@Composable
private fun Highlight(icon: ImageVector, title: String, body: String) {
    val c = Salah.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconBadge(icon)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(body, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Salah.colors
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(28.dp))
            .background(if (enabled) c.accent else c.accent.copy(alpha = 0.3f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.onAccent)
    }
}

@Composable
private fun SecondaryButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val c = Salah.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(18.dp))
            .border(1.dp, c.accentHairline, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = c.text)
    }
}
