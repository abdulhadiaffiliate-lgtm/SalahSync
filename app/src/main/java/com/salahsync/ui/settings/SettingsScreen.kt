package com.salahsync.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.salahsync.BuildConfig
import com.salahsync.core.AsrSchool
import com.salahsync.core.HighLatitude
import com.salahsync.core.Method
import com.salahsync.core.Prayer
import com.salahsync.core.SilenceMode
import com.salahsync.core.daily.DailyNoteKind
import com.salahsync.data.AlertStyle
import com.salahsync.data.Cities
import com.salahsync.data.SavedLocation
import com.salahsync.data.ThemeMode
import com.salahsync.data.UserPrefs
import com.salahsync.notify.Notifications
import com.salahsync.notify.PrayerSilence
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.theme.Salah
import com.salahsync.ui.util.formatTime
import java.time.LocalTime

class SettingsActions(
    val setName: (String) -> Unit,
    val locate: () -> Unit,
    val setLocation: (SavedLocation) -> Unit,
    val setMethodAuto: (Boolean) -> Unit,
    val setMethod: (Method) -> Unit,
    val setAsrSchool: (AsrSchool) -> Unit,
    val setHighLatitude: (HighLatitude) -> Unit,
    val setOffset: (Prayer, Int) -> Unit,
    val setUseOnlineTimes: (Boolean) -> Unit,
    val setHijriAuto: (Boolean) -> Unit,
    val setHijriAdjustment: (Int) -> Unit,
    val setAlert: (Prayer, AlertStyle) -> Unit,
    val setPreReminder: (Int) -> Unit,
    val setQuranReminder: (Boolean) -> Unit,
    val setDuaReminder: (Boolean) -> Unit,
    val setTheme: (ThemeMode) -> Unit,
    val setSilentEnabled: (Boolean) -> Unit,
    val setSilentMode: (SilenceMode) -> Unit,
    val setSilentMinutes: (Int) -> Unit,
    val setSilentJumuahMinutes: (Int) -> Unit,
    val setSilentFor: (Prayer, Boolean) -> Unit,
    val setWeeklyReport: (Boolean) -> Unit,
    val setDailyNote: (Boolean) -> Unit,
    val setDailyNoteKind: (DailyNoteKind) -> Unit,
    val setDailyNoteMinutes: (Int) -> Unit,
    val previewDailyNote: () -> Unit,
)

private sealed interface Picker {
    data object Name : Picker
    data object City : Picker
    data object CalcMethod : Picker
    data object Asr : Picker
    data object HighLat : Picker
    data class Alert(val prayer: Prayer) : Picker
    data object PreReminder : Picker
    data object Theme : Picker
    data object SilentMode : Picker
    data object SilentMinutes : Picker
    data object JumuahMinutes : Picker
    data object DndAccess : Picker
    data object DailyKind : Picker
    data object DailyTime : Picker
}

fun Context.isIgnoringBatteryOptimizations(): Boolean =
    getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) == true

fun Context.openBatterySettings() {
    // Opens the list where the user can exempt SalahSync; no special permission needed.
    runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
}

fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
        }
    }
}

fun Context.openDndAccessSettings() {
    runCatching { startActivity(PrayerSilence.policyAccessIntent()) }
}

fun Context.openNotificationSettings() {
    runCatching {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }
}

@Composable
fun SettingsScreen(
    prefs: UserPrefs,
    locating: Boolean,
    canScheduleExact: Boolean,
    checkDndAccess: () -> Boolean,
    actions: SettingsActions,
    onOpenAbout: () -> Unit,
    onOpenReport: () -> Unit,
) {
    val c = Salah.colors
    val context = LocalContext.current
    var picker by remember { mutableStateOf<Picker?>(null) }
    // Access is granted on a system screen, so check again whenever the user comes back.
    var dndAccess by remember { mutableStateOf(checkDndAccess()) }
    LifecycleResumeEffect(Unit) {
        dndAccess = checkDndAccess()
        onPauseOrDispose { }
    }
    val needsAccess = prefs.silentMode.needsPolicyAccess && !dndAccess
    // Notifications can be switched off in system settings, so check again on return too.
    var canNotify by remember { mutableStateOf(Notifications.canPost(context)) }
    LifecycleResumeEffect(Unit) {
        canNotify = Notifications.canPost(context)
        onPauseOrDispose { }
    }
    val dailyTime = LocalTime.of(prefs.dailyNoteMinutes / 60, prefs.dailyNoteMinutes % 60)

    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 120.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = c.text, modifier = Modifier.padding(horizontal = 4.dp))

        Section("Profile") {
            Item("Name", prefs.name.ifBlank { "Not set" }) { picker = Picker.Name }
        }

        Section("Location") {
            Item("Current location", prefs.location?.placeName ?: "Not set")
            Item("Update from GPS", if (locating) "Locating…" else "Use this phone's location", onClick = actions.locate)
            Item("Choose a city", "Pick from a list instead of GPS") { picker = Picker.City }
        }

        Section("Prayer times") {
            Toggle("Choose method from my location", "Picks the method and Asr timing used in your country", prefs.methodAuto, actions.setMethodAuto)
            Item("Calculation method", prefs.method.displayName) { picker = Picker.CalcMethod }
            Item("Asr timing", prefs.asrSchool.displayName) { picker = Picker.Asr }
            Item("High-latitude rule", prefs.highLatitude.displayName) { picker = Picker.HighLat }
            Toggle("Use AlAdhan online times", "Falls back to the built-in calculator when offline", prefs.useOnlineTimes, actions.setUseOnlineTimes)
            EyebrowText("Adjust minutes to match your mosque", modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
            Prayer.entries.forEach { p ->
                Stepper(p.displayName, prefs.offsets[p] ?: 0, -30..30, suffix = " min") { actions.setOffset(p, it) }
            }
        }

        Section("Hijri date") {
            Toggle("Adjust from my location", "One day earlier in South Asia, following local moon sighting", prefs.hijriAuto, actions.setHijriAuto)
            Stepper("Days adjustment", prefs.hijriAdjustment, -2..2, suffix = " day", onChange = actions.setHijriAdjustment)
        }

        Section("Notifications") {
            Prayer.entries.forEach { p ->
                Item("${p.displayName} alert", prefs.alertFor(p).displayName) { picker = Picker.Alert(p) }
            }
            Item("Reminder before prayer", if (prefs.preReminderMinutes == 0) "Off" else "${prefs.preReminderMinutes} minutes before") {
                picker = Picker.PreReminder
            }
            Toggle("Daily Quran reminder", "Every day at 8:00 AM", prefs.quranReminder, actions.setQuranReminder)
            Toggle("Evening dua reminder", "Every day at 7:30 PM", prefs.duaReminder, actions.setDuaReminder)
            if (!canScheduleExact) {
                Item("Allow exact alarms", "Needed for the adhan to play on time", onClick = context::openExactAlarmSettings)
            }
            Item(
                "Battery optimization",
                if (context.isIgnoringBatteryOptimizations()) "SalahSync is exempt" else "Exempt SalahSync so alerts are never delayed",
                onClick = context::openBatterySettings,
            )
            Item("System notification settings", "Sounds, vibration and lock screen", onClick = context::openNotificationSettings)
        }

        Section("Silent during prayer") {
            Toggle(
                "Silence my phone at prayer times",
                "Turns quiet when each prayer begins and puts the sound back by itself",
                prefs.silentEnabled,
            ) { on ->
                actions.setSilentEnabled(on)
                if (on && needsAccess) picker = Picker.DndAccess
            }
            if (prefs.silentEnabled) {
                Item("How", prefs.silentMode.displayName) { picker = Picker.SilentMode }
                if (needsAccess) {
                    Item(
                        "Allow Do Not Disturb access",
                        "Until you do, SalahSync sets your phone to vibrate instead",
                        onClick = { picker = Picker.DndAccess },
                    )
                }
                Item("For how long", "${prefs.silentMinutes} minutes after each prayer begins") { picker = Picker.SilentMinutes }
                Item("On Fridays, for Jumu'ah", "${prefs.silentJumuahMinutes} minutes") { picker = Picker.JumuahMinutes }
                EyebrowText("Prayers", modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
                Prayer.fard.forEach { p ->
                    Toggle(p.displayName, if (p in prefs.silentPrayers) "Quiet for this prayer" else "Sound stays on", p in prefs.silentPrayers) {
                        actions.setSilentFor(p, it)
                    }
                }
            }
        }

        Section("Daily ayah and good deed") {
            Toggle(
                "Daily notification",
                if (prefs.dailyNote) "Every day at ${formatTime(dailyTime)}, quietly, with no sound" else "A short ayah or a good deed, once a day",
                prefs.dailyNote,
                actions.setDailyNote,
            )
            if (prefs.dailyNote) {
                Item("Show", prefs.dailyNoteKind.displayName) { picker = Picker.DailyKind }
                Item("Time", formatTime(dailyTime)) { picker = Picker.DailyTime }
                if (!canNotify) {
                    Item("Allow notifications", "SalahSync cannot show notifications right now", onClick = context::openNotificationSettings)
                }
                Item("Preview", "Show today's notification now", onClick = actions.previewDailyNote)
            }
        }

        Section("Weekly report") {
            Toggle("Friday report", "A summary of your week every Friday at 9:00 AM", prefs.weeklyReport, actions.setWeeklyReport)
            Item("Open this week's report", "On time, late, missed, Qada and streaks", onClick = onOpenReport)
        }

        Section("Appearance") {
            Item("Theme", prefs.theme.displayName) { picker = Picker.Theme }
        }

        Section("About") {
            Item("About SalahSync", "Version ${BuildConfig.VERSION_NAME}, sources and licenses", onClick = onOpenAbout)
        }
    }

    when (val p = picker) {
        null -> Unit
        Picker.Name -> NameDialog(prefs.name, onDismiss = { picker = null }) { actions.setName(it); picker = null }
        Picker.City -> ChoiceDialog("Choose a city", Cities.all, prefs.location, { it.placeName }, { picker = null }) {
            actions.setLocation(it); picker = null
        }
        Picker.CalcMethod -> ChoiceDialog("Calculation method", Method.entries, prefs.method, { it.displayName }, { picker = null }) {
            actions.setMethod(it); picker = null
        }
        Picker.Asr -> ChoiceDialog("Asr timing", AsrSchool.entries, prefs.asrSchool, { it.displayName }, { picker = null }) {
            actions.setAsrSchool(it); picker = null
        }
        Picker.HighLat -> ChoiceDialog("High-latitude rule", HighLatitude.entries, prefs.highLatitude, { it.displayName }, { picker = null }) {
            actions.setHighLatitude(it); picker = null
        }
        is Picker.Alert -> {
            val options = if (p.prayer == Prayer.SUNRISE) listOf(AlertStyle.SILENT, AlertStyle.OFF) else AlertStyle.entries
            ChoiceDialog("${p.prayer.displayName} alert", options, prefs.alertFor(p.prayer), { it.displayName }, { picker = null }) {
                actions.setAlert(p.prayer, it); picker = null
            }
        }
        Picker.PreReminder -> ChoiceDialog(
            "Reminder before prayer", listOf(0, 10, 15, 30), prefs.preReminderMinutes,
            { if (it == 0) "Off" else "$it minutes before" }, { picker = null },
        ) { actions.setPreReminder(it); picker = null }
        Picker.Theme -> ChoiceDialog("Theme", ThemeMode.entries, prefs.theme, { it.displayName }, { picker = null }) {
            actions.setTheme(it); picker = null
        }
        Picker.SilentMode -> ChoiceDialog(
            "Silence with", SilenceMode.entries, prefs.silentMode, { "${it.displayName}: ${it.description}" }, { picker = null },
        ) {
            actions.setSilentMode(it)
            picker = if (it.needsPolicyAccess && !dndAccess) Picker.DndAccess else null
        }
        Picker.SilentMinutes -> ChoiceDialog(
            "For how long", UserPrefs.silentMinuteOptions, prefs.silentMinutes, { "$it minutes" }, { picker = null },
        ) { actions.setSilentMinutes(it); picker = null }
        Picker.JumuahMinutes -> ChoiceDialog(
            "Jumu'ah", UserPrefs.jumuahMinuteOptions, prefs.silentJumuahMinutes, { "$it minutes" }, { picker = null },
        ) { actions.setSilentJumuahMinutes(it); picker = null }
        Picker.DailyKind -> ChoiceDialog("Show", DailyNoteKind.entries, prefs.dailyNoteKind, { it.displayName }, { picker = null }) {
            actions.setDailyNoteKind(it); picker = null
        }
        Picker.DailyTime -> TimeDialog("Daily notification time", dailyTime, onDismiss = { picker = null }) {
            actions.setDailyNoteMinutes(it.hour * 60 + it.minute); picker = null
        }
        Picker.DndAccess -> DndAccessDialog(
            mode = prefs.silentMode,
            onDismiss = { picker = null },
            onOpenSettings = { picker = null; context.openDndAccessSettings() },
            onUseVibrate = { actions.setSilentMode(SilenceMode.VIBRATE); picker = null },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val c = Salah.colors
    Spacer(Modifier.height(28.dp))
    EyebrowText(title, color = c.accent, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).border(1.dp, c.hairline, RoundedCornerShape(20.dp)).padding(vertical = 6.dp),
    ) { content() }
}

@Composable
private fun Item(title: String, value: String, onClick: (() -> Unit)? = null) {
    val c = Salah.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.text)
            Text(value, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        if (onClick != null) Icon(SalahIcons.ChevronRight, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun Toggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Salah.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent, checkedTrackColor = c.accent,
                uncheckedThumbColor = c.textMuted, uncheckedTrackColor = c.surfaceDeep, uncheckedBorderColor = c.accentHairline,
            ),
        )
    }
}

@Composable
private fun Stepper(title: String, value: Int, range: IntRange, suffix: String, onChange: (Int) -> Unit) {
    val c = Salah.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((value - 1).coerceIn(range)) }, enabled = value > range.first) {
            Text("−", style = MaterialTheme.typography.titleLarge, color = c.accent)
        }
        val sign = if (value > 0) "+" else ""
        Text("$sign$value$suffix${if (kotlin.math.abs(value) == 1 || suffix != " day") "" else "s"}",
            style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.width(64.dp))
        IconButton(onClick = { onChange((value + 1).coerceIn(range)) }, enabled = value < range.last) {
            Text("+", style = MaterialTheme.typography.titleLarge, color = c.accent)
        }
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    val c = Salah.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = c.text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { o ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected = o == selected, role = Role.RadioButton) { onSelect(o) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = o == selected, onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.textMuted),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(label(o), style = MaterialTheme.typography.bodyLarge, color = c.text)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = c.accent) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(title: String, initial: LocalTime, onDismiss: () -> Unit, onSave: (LocalTime) -> Unit) {
    val c = Salah.colors
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = c.text) },
        text = {
            TimePicker(
                state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = c.surfaceDeep,
                    selectorColor = c.accent,
                    clockDialSelectedContentColor = c.onAccent,
                    clockDialUnselectedContentColor = c.text,
                    periodSelectorBorderColor = c.accentHairline,
                    periodSelectorSelectedContainerColor = c.accent,
                    periodSelectorSelectedContentColor = c.onAccent,
                    periodSelectorUnselectedContentColor = c.textMuted,
                    timeSelectorSelectedContainerColor = c.accent,
                    timeSelectorSelectedContentColor = c.onAccent,
                    timeSelectorUnselectedContainerColor = c.surfaceDeep,
                    timeSelectorUnselectedContentColor = c.text,
                ),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(LocalTime.of(state.hour, state.minute)) }) { Text("Save", color = c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
    )
}

@Composable
private fun DndAccessDialog(mode: SilenceMode, onDismiss: () -> Unit, onOpenSettings: () -> Unit, onUseVibrate: () -> Unit) {
    val c = Salah.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Allow Do Not Disturb access", style = MaterialTheme.typography.titleLarge, color = c.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "To use ${mode.displayName.lowercase()} during prayer, Android needs you to let SalahSync change Do Not Disturb. " +
                        "On the next screen, find SalahSync and switch it on, then come back.",
                    style = MaterialTheme.typography.bodyMedium, color = c.text,
                )
                Text(
                    "SalahSync only uses this during the minutes you chose, and always puts your sound back afterwards. " +
                        "Until access is allowed, your phone is set to vibrate instead.",
                    style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                )
            }
        },
        confirmButton = { TextButton(onClick = onOpenSettings) { Text("Open settings", color = c.accent) } },
        dismissButton = { TextButton(onClick = onUseVibrate) { Text("Use vibrate", color = c.textMuted) } },
    )
}

@Composable
private fun NameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val c = Salah.colors
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Your name", style = MaterialTheme.typography.titleLarge, color = c.text) },
        text = { OutlinedTextField(text, { text = it }, singleLine = true, label = { Text("Name") }) },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Save", color = c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
    )
}
