package com.salahsync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.salahsync.data.ThemeMode

/** The Midnight emerald palette (design version A) and its ivory light counterpart. */
@Immutable
data class SalahColors(
    val background: Color,
    val surface: Color,
    val surfaceDeep: Color,
    val accent: Color,
    val onAccent: Color,
    val text: Color,
    val textMuted: Color,
    val hairline: Color,
    val accentHairline: Color,
    val card: Color,
    val onCard: Color,
    val onCardMuted: Color,
    val isDark: Boolean,
)

val MidnightEmerald = SalahColors(
    background = Color(0xFF0B231E),
    surface = Color(0xFF14362E),
    surfaceDeep = Color(0xFF0F2B25),
    accent = Color(0xFFD9BC84),
    onAccent = Color(0xFF0B231E),
    text = Color(0xFFF6F1E7),
    textMuted = Color(0xFFB9B4A8),
    hairline = Color(0x1AF6F1E7),
    accentHairline = Color(0x38D9BC84),
    card = Color(0xFFF6F1E7),
    onCard = Color(0xFF0B231E),
    onCardMuted = Color(0xFF6B5A35),
    isDark = true,
)

val IvoryDaylight = SalahColors(
    background = Color(0xFFF6F1E7),
    surface = Color(0xFFFFFFFF),
    surfaceDeep = Color(0xFFEFE8DA),
    accent = Color(0xFF7A5C26),
    onAccent = Color(0xFFF6F1E7),
    text = Color(0xFF0E2A24),
    textMuted = Color(0xFF5E665F),
    hairline = Color(0x1F0E2A24),
    accentHairline = Color(0x477A5C26),
    card = Color(0xFF0E2A24),
    onCard = Color(0xFFF6F1E7),
    onCardMuted = Color(0xFFD9BC84),
    isDark = false,
)

val LocalSalahColors = staticCompositionLocalOf { MidnightEmerald }

object Salah {
    val colors: SalahColors
        @Composable get() = LocalSalahColors.current
}

@Composable
fun SalahSyncTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val c = if (dark) MidnightEmerald else IvoryDaylight
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            background = c.background, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surfaceDeep, onSurfaceVariant = c.textMuted,
            outline = c.accentHairline,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            background = c.background, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surfaceDeep, onSurfaceVariant = c.textMuted,
            outline = c.accentHairline,
        )
    }
    CompositionLocalProvider(LocalSalahColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = SalahTypography, content = content)
    }
}
