package com.salahsync

import android.content.Intent
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.salahsync.adhkar.AdhkarViewModel
import com.salahsync.core.hadith.HadithBook
import com.salahsync.core.quran.AyahRef
import com.salahsync.data.UserPrefs
import com.salahsync.hadith.HadithItem
import com.salahsync.hadith.HadithViewModel
import com.salahsync.quran.QuranViewModel
import com.salahsync.tracker.TrackerViewModel
import com.salahsync.ui.adhkar.AdhkarScreen
import com.salahsync.ui.components.SalahIcons
import com.salahsync.ui.hadith.HadithBookScreen
import com.salahsync.ui.hadith.HadithChapterScreen
import com.salahsync.ui.hadith.HadithDetailScreen
import com.salahsync.ui.hadith.HadithScreen
import com.salahsync.ui.home.HomeScreen
import com.salahsync.ui.more.AboutScreen
import com.salahsync.ui.more.ComingSoonScreen
import com.salahsync.ui.more.MoreScreen
import com.salahsync.ui.onboarding.OnboardingScreen
import com.salahsync.ui.prayers.PrayerTimesScreen
import com.salahsync.ui.qibla.QiblaScreen
import com.salahsync.ui.quran.MiniPlayer
import com.salahsync.ui.quran.PlayerScreen
import com.salahsync.ui.quran.QuranScreen
import com.salahsync.ui.quran.ReaderScreen
import com.salahsync.ui.settings.SettingsActions
import com.salahsync.ui.settings.SettingsScreen
import com.salahsync.ui.tasbih.TasbihScreen
import com.salahsync.ui.theme.Salah
import com.salahsync.ui.theme.SalahSyncTheme
import com.salahsync.ui.tracker.TrackerActions
import com.salahsync.ui.tracker.TrackerScreen
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    /** Set when the recitation notification is tapped, to open the full player. */
    private val openPlayer = MutableStateFlow(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) openPlayer.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) openPlayer.value = true
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
                    AppShell(vm, p, openPlayer)
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "com.salahsync.OPEN_PLAYER"
    }
}

private object Routes {
    const val HOME = "home"
    const val PRAYERS = "prayers"
    const val QURAN = "quran"
    const val READER = "quran/read/{surah}?ayah={ayah}"
    const val PLAYER = "player"
    fun reader(ref: AyahRef) = "quran/read/${ref.surah}?ayah=${ref.ayah}"
    const val QIBLA = "qibla"
    const val MORE = "more"
    const val TASBIH = "tasbih"
    const val TRACKER = "tracker"
    const val ADHKAR = "adhkar"
    const val HADITH = "hadith"
    const val HADITH_BOOK = "hadith/book/{book}"
    const val HADITH_CHAPTER = "hadith/book/{book}/{chapter}"
    const val HADITH_VIEW = "hadith/view/{book}/{ref}"
    fun hadithBook(book: HadithBook) = "hadith/book/${book.slug}"
    fun hadithChapter(book: HadithBook, chapter: Int) = "hadith/book/${book.slug}/$chapter"
    fun hadithView(item: HadithItem) = "hadith/view/${item.book.slug}/${android.net.Uri.encode(item.ref)}"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val SOON = "soon/{feature}"
    fun soon(feature: String) = "soon/${android.net.Uri.encode(feature)}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", SalahIcons.Home),
    Tab(Routes.PRAYERS, "Prayers", SalahIcons.Clock),
    Tab(Routes.QURAN, "Quran", SalahIcons.Book),
    Tab(Routes.QIBLA, "Qibla", SalahIcons.Compass),
    Tab(Routes.MORE, "More", SalahIcons.More),
)

@Composable
private fun AppShell(vm: MainViewModel, prefs: UserPrefs, openPlayer: MutableStateFlow<Boolean>) {
    val nav = rememberNavController()
    val quran: QuranViewModel = viewModel()
    val tracker: TrackerViewModel = viewModel()
    val hadith: HadithViewModel = viewModel()
    val hadithPrefs by hadith.prefs.collectAsStateWithLifecycle()
    val dailyHadith by hadith.daily.collectAsStateWithLifecycle()
    val trackerState by tracker.state.collectAsStateWithLifecycle()
    val playback by quran.playback.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val wantsPlayer by openPlayer.collectAsStateWithLifecycle()
    LaunchedEffect(wantsPlayer, playback.active) {
        if (wantsPlayer && playback.active) {
            if (route != Routes.PLAYER) nav.navigate(Routes.PLAYER) { launchSingleTop = true }
            openPlayer.value = false
        }
    }
    val showMini = playback.active && route != Routes.PLAYER
    val now by vm.now.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val tomorrow by vm.tomorrow.collectAsStateWithLifecycle()
    val locating by vm.locating.collectAsStateWithLifecycle()
    val c = Salah.colors

    Box(Modifier.fillMaxSize().background(c.background)) {
        NavHost(
            nav, startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().padding(bottom = if (showMini) 68.dp else 0.dp),
        ) {
            composable(Routes.HOME) {
                // Read through the collected state so the card appears once the daily set loads.
                val todaysHadith = dailyHadith.takeIf { it.isNotEmpty() }?.let { hadith.dailyFor(now.toLocalDate()) }
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
                    onOpenQuran = { nav.switchTab(Routes.QURAN) },
                    onOpenComingSoon = { nav.navigate(Routes.soon(it)) },
                    onOpenPrayers = { nav.switchTab(Routes.PRAYERS) },
                    dailyHadith = todaysHadith,
                    hadithTopic = todaysHadith?.let { hadith.topicOf(it)?.topic },
                    hadithLanguage = hadithPrefs.language,
                    onOpenDailyHadith = { nav.navigate(Routes.hadithView(it)) },
                    onOpenHadith = { nav.navigate(Routes.HADITH) },
                )
            }
            composable(Routes.QURAN) { QuranScreen(quran, onRead = { nav.navigate(Routes.reader(it)) }) }
            composable(
                Routes.READER,
                arguments = listOf(
                    navArgument("surah") { type = NavType.IntType },
                    navArgument("ayah") { type = NavType.IntType; defaultValue = 1 },
                ),
            ) { e ->
                val surah = (e.arguments?.getInt("surah") ?: 1).coerceIn(1, 114)
                ReaderScreen(
                    vm = quran,
                    surahNumber = surah,
                    startAyah = e.arguments?.getInt("ayah") ?: 1,
                    onBack = { nav.popBackStack() },
                    onOpenSurah = { nav.navigate(Routes.reader(it)) { popUpTo(Routes.READER) { inclusive = true } } },
                )
            }
            composable(Routes.PLAYER) {
                PlayerScreen(
                    vm = quran,
                    onCollapse = { if (nav.currentBackStackEntry?.destination?.route == Routes.PLAYER) nav.popBackStack() },
                    onOpenReader = { ref ->
                        nav.popBackStack()
                        nav.navigate(Routes.reader(ref))
                    },
                )
            }
            composable(Routes.PRAYERS) {
                PrayerTimesScreen(
                    prefs = prefs,
                    now = now,
                    loadTimes = vm::timesFor,
                    hijriFor = vm::hijriFor,
                    isPrayed = { d, p -> trackerState?.isPrayed(d, p) == true },
                    onTogglePrayed = tracker::togglePrayed,
                    qadaOwed = trackerState?.totalOwed ?: 0,
                    onOpenTracker = { nav.navigate(Routes.TRACKER) },
                    onAlertChange = vm::setAlert,
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.QIBLA) { QiblaScreen(prefs.location?.point, prefs.location?.placeName) }
            composable(Routes.MORE) {
                MoreScreen(
                    onTasbih = { nav.navigate(Routes.TASBIH) },
                    onAdhkar = { nav.navigate(Routes.ADHKAR) },
                    onTracker = { nav.navigate(Routes.TRACKER) },
                    onQuran = { nav.switchTab(Routes.QURAN) },
                    onHadith = { nav.navigate(Routes.HADITH) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                    onAbout = { nav.navigate(Routes.ABOUT) },
                )
            }
            composable(Routes.HADITH) {
                HadithScreen(
                    hadith,
                    onOpenBook = { nav.navigate(Routes.hadithBook(it)) },
                    onOpenHadith = { nav.navigate(Routes.hadithView(it)) },
                )
            }
            composable(Routes.HADITH_BOOK, arguments = listOf(navArgument("book") { type = NavType.StringType })) { e ->
                val book = HadithBook.bySlug(e.arguments?.getString("book")) ?: HadithBook.BUKHARI
                HadithBookScreen(hadith, book, onBack = { nav.popBackStack() }, onOpenChapter = { nav.navigate(Routes.hadithChapter(book, it)) })
            }
            composable(
                Routes.HADITH_CHAPTER,
                arguments = listOf(navArgument("book") { type = NavType.StringType }, navArgument("chapter") { type = NavType.IntType }),
            ) { e ->
                val book = HadithBook.bySlug(e.arguments?.getString("book")) ?: HadithBook.BUKHARI
                HadithChapterScreen(hadith, book, e.arguments?.getInt("chapter") ?: 1, onBack = { nav.popBackStack() })
            }
            composable(
                Routes.HADITH_VIEW,
                arguments = listOf(navArgument("book") { type = NavType.StringType }, navArgument("ref") { type = NavType.StringType }),
            ) { e ->
                val book = HadithBook.bySlug(e.arguments?.getString("book")) ?: HadithBook.BUKHARI
                HadithDetailScreen(
                    hadith, book, e.arguments?.getString("ref").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenBook = { nav.navigate(Routes.hadithBook(it)) },
                )
            }
            composable(Routes.TASBIH) { TasbihScreen() }
            composable(Routes.TRACKER) {
                TrackerScreen(
                    state = trackerState,
                    actions = TrackerActions(
                        togglePrayed = tracker::togglePrayed,
                        logMadeUp = { tracker.logMadeUp(it) },
                        undoMadeUp = { tracker.undoMadeUp(it) },
                        setEarlier = { p, n -> tracker.setEarlier(p, n) },
                    ),
                )
            }
            composable(Routes.ADHKAR) {
                val adhkar: AdhkarViewModel = viewModel()
                AdhkarScreen(adhkar)
            }
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
        if (route != Routes.PLAYER) {
            Column(Modifier.align(Alignment.BottomCenter)) {
                AnimatedVisibility(showMini, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
                    MiniPlayer(quran, onExpand = { nav.navigate(Routes.PLAYER) { launchSingleTop = true } }, Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp))
                }
                BottomBar(nav)
            }
        }
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
            val selected = current == tab.route || (tab.route == Routes.QURAN && current == Routes.READER)
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
