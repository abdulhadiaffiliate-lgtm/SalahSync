package com.salahsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.salahsync.data.UserPrefs
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.home.HomeScreen
import com.salahsync.ui.more.AboutScreen
import com.salahsync.ui.more.ComingSoonScreen
import com.salahsync.ui.more.MoreScreen
import com.salahsync.ui.onboarding.OnboardingScreen
import com.salahsync.ui.prayers.PrayerTimesScreen
import com.salahsync.ui.qibla.QiblaScreen
import com.salahsync.ui.settings.SettingsActions
import com.salahsync.ui.settings.SettingsScreen
import com.salahsync.ui.tasbih.TasbihScreen
import com.salahsync.ui.theme.Salah
import com.salahsync.ui.theme.SalahSyncTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            val p = prefs ?: return@setContent
            SalahSyncTheme(p.theme) {
                val colors = Salah.colors
                val barStyle = if (colors.isDark) SystemBarStyle.dark(colors.background.toArgb())
                else SystemBarStyle.light(colors.background.toArgb(), colors.text.toArgb())
                SideEffect { enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle) }
                // Pick up permissions granted in system settings, e.g. exact alarms.
                LifecycleResumeEffect(Unit) {
                    vm.rescheduleNow()
                    onPauseOrDispose { }
                }
                if (!p.onboarded) {
                    val locating by vm.locating.collectAsStateWithLifecycle()
                    OnboardingScreen(
                        prefs = p,
                        locating = locating,
                        canScheduleExact = vm::canScheduleExact,
                        onName = vm::setName,
                        onLocate = { vm.locate() },
                        onChooseCity = vm::setLocation,
                        onFinish = vm::completeOnboarding,
                    )
                } else {
                    AppShell(vm, p)
                }
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val PRAYERS = "prayers"
    const val QIBLA = "qibla"
    const val MORE = "more"
    const val TASBIH = "tasbih"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val SOON = "soon/{feature}"
    fun soon(feature: String) = "soon/${android.net.Uri.encode(feature)}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", SalahIcons.Home),
    Tab(Routes.PRAYERS, "Prayers", SalahIcons.Clock),
    Tab(Routes.QIBLA, "Qibla", SalahIcons.Compass),
    Tab(Routes.MORE, "More", SalahIcons.More),
)

@Composable
private fun AppShell(vm: MainViewModel, prefs: UserPrefs) {
    val nav = rememberNavController()
    val now by vm.now.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val tomorrow by vm.tomorrow.collectAsStateWithLifecycle()
    val locating by vm.locating.collectAsStateWithLifecycle()
    val c = Salah.colors

    Box(Modifier.fillMaxSize().background(c.background)) {
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.fillMaxSize()) {
            composable(Routes.HOME) {
                HomeScreen(
                    name = prefs.name,
                    streak = prefs.streakDays,
                    now = now,
                    hijri = vm.hijriFor(now.toLocalDate()),
                    asrLabel = prefs.asrSchool.displayName.substringBefore(','),
                    today = today,
                    tomorrow = tomorrow,
                    onOpenQibla = { nav.switchTab(Routes.QIBLA) },
                    onOpenTasbih = { nav.navigate(Routes.TASBIH) },
                    onOpenComingSoon = { nav.navigate(Routes.soon(it)) },
                    onOpenPrayers = { nav.switchTab(Routes.PRAYERS) },
                )
            }
            composable(Routes.PRAYERS) {
                PrayerTimesScreen(
                    prefs = prefs,
                    now = now,
                    loadTimes = vm::timesFor,
                    hijriFor = vm::hijriFor,
                    onTogglePrayed = vm::togglePrayed,
                    onAlertChange = vm::setAlert,
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.QIBLA) { QiblaScreen(prefs.location?.point, prefs.location?.placeName) }
            composable(Routes.MORE) {
                MoreScreen(
                    onTasbih = { nav.navigate(Routes.TASBIH) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                    onAbout = { nav.navigate(Routes.ABOUT) },
                    onComingSoon = { nav.navigate(Routes.soon(it)) },
                )
            }
            composable(Routes.TASBIH) { TasbihScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    prefs = prefs,
                    locating = locating,
                    canScheduleExact = vm.canScheduleExact(),
                    actions = SettingsActions(
                        setName = vm::setName,
                        locate = { vm.locate() },
                        setLocation = vm::setLocation,
                        setMethodAuto = vm::setMethodAuto,
                        setMethod = vm::setMethod,
                        setAsrSchool = vm::setAsrSchool,
                        setHighLatitude = vm::setHighLatitude,
                        setOffset = vm::setOffset,
                        setUseOnlineTimes = vm::setUseOnlineTimes,
                        setHijriAuto = vm::setHijriAuto,
                        setHijriAdjustment = vm::setHijriAdjustment,
                        setAlert = vm::setAlert,
                        setPreReminder = vm::setPreReminder,
                        setQuranReminder = vm::setQuranReminder,
                        setDuaReminder = vm::setDuaReminder,
                        setTheme = vm::setTheme,
                    ),
                    onOpenAbout = { nav.navigate(Routes.ABOUT) },
                )
            }
            composable(Routes.ABOUT) { AboutScreen() }
            composable(Routes.SOON, arguments = listOf(navArgument("feature") { type = NavType.StringType })) { entry ->
                ComingSoonScreen(entry.arguments?.getString("feature").orEmpty())
            }
        }
        BottomBar(nav, Modifier.align(Alignment.BottomCenter))
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(nav: NavHostController, modifier: Modifier = Modifier) {
    val c = Salah.colors
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp).fillMaxWidth().height(64.dp)
            .clip(shape).background(c.surface.copy(alpha = 0.94f)).border(1.dp, c.accentHairline, shape),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = current == tab.route
            val tint = if (selected) c.accent else c.textMuted
            Column(
                Modifier.weight(1f).fillMaxSize()
                    .selectable(selected = selected, role = Role.Tab) { nav.switchTab(tab.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Text(tab.label, style = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize), color = tint)
            }
        }
    }
}
