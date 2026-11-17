package com.sameerasw.canvas.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.SettingsRepository

private val LightColorScheme = lightColorScheme(
    primary = InkPrimaryLight,
    onPrimary = InkOnPrimaryLight,
    primaryContainer = InkPrimaryContainerLight,
    onPrimaryContainer = InkOnPrimaryContainerLight,
    secondary = InkSecondaryLight,
    onSecondary = InkOnSecondaryLight,
    secondaryContainer = InkSecondaryContainerLight,
    onSecondaryContainer = InkOnSecondaryContainerLight,
    tertiary = InkTertiaryLight,
    onTertiary = InkOnTertiaryLight,
    tertiaryContainer = InkTertiaryContainerLight,
    onTertiaryContainer = InkOnTertiaryContainerLight,
    background = InkBackgroundLight,
    onBackground = InkOnSurfaceLight,
    surface = InkSurfaceLight,
    onSurface = InkOnSurfaceLight,
    surfaceVariant = InkSurfaceVariantLight,
    onSurfaceVariant = InkOnSurfaceVariantLight,
    outline = InkOutlineLight,
    outlineVariant = InkOutlineVariantLight,
    surfaceContainerLowest = InkSurfaceContainerLowestLight,
    surfaceContainerLow = InkSurfaceContainerLowLight,
    surfaceContainer = InkSurfaceContainerLight,
    surfaceContainerHigh = InkSurfaceContainerHighLight,
    surfaceContainerHighest = InkSurfaceContainerHighestLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = InkPrimaryDark,
    onPrimary = InkOnPrimaryDark,
    primaryContainer = InkPrimaryContainerDark,
    onPrimaryContainer = InkOnPrimaryContainerDark,
    secondary = InkSecondaryDark,
    onSecondary = InkOnSecondaryDark,
    secondaryContainer = InkSecondaryContainerDark,
    onSecondaryContainer = InkOnSecondaryContainerDark,
    tertiary = InkTertiaryDark,
    onTertiary = InkOnTertiaryDark,
    tertiaryContainer = InkTertiaryContainerDark,
    onTertiaryContainer = InkOnTertiaryContainerDark,
    background = InkBackgroundDark,
    onBackground = InkOnSurfaceDark,
    surface = InkSurfaceDark,
    onSurface = InkOnSurfaceDark,
    surfaceVariant = InkSurfaceVariantDark,
    onSurfaceVariant = InkOnSurfaceVariantDark,
    outline = InkOutlineDark,
    outlineVariant = InkOutlineVariantDark,
    surfaceContainerLowest = InkSurfaceContainerLowestDark,
    surfaceContainerLow = InkSurfaceContainerLowDark,
    surfaceContainer = InkSurfaceContainerDark,
    surfaceContainerHigh = InkSurfaceContainerHighDark,
    surfaceContainerHighest = InkSurfaceContainerHighestDark,
)

/** Soft, generous corners everywhere: the app is all floating surfaces over a sheet of paper. */
val CanvasShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
private fun colorSchemeFor(darkTheme: Boolean): ColorScheme =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }

/** Whether the app is currently dark, honouring the in-app theme setting. */
@Composable
fun isAppInDarkTheme(): Boolean {
    val themeMode by SettingsRepository.themeMode.collectAsState()
    return when (themeMode) {
        SettingsRepository.ThemeMode.LIGHT -> false
        SettingsRepository.ThemeMode.DARK -> true
        SettingsRepository.ThemeMode.DYNAMIC -> isSystemInDarkTheme()
    }
}

/**
 * App theme. Follows the theme setting live (Light / System / Dark), uses wallpaper colors on
 * Android 12+ and the Ink brand palette below that, with Material 3 Expressive motion.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CanvasTheme(content: @Composable () -> Unit) {
    val isDarkTheme = isAppInDarkTheme()
    val colorScheme = colorSchemeFor(isDarkTheme)

    val context = LocalContext.current
    DisposableEffect(isDarkTheme) {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { isDarkTheme },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { isDarkTheme }
        )
        onDispose {}
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = CanvasShapes,
        typography = Typography,
        content = content
    )
}

/** Kept for callers that still pass a mode; the theme now follows the setting by itself. */
@Composable
fun CanvasThemeWithMode(
    @Suppress("UNUSED_PARAMETER") themeMode: SettingsRepository.ThemeMode = SettingsRepository.ThemeMode.DYNAMIC,
    content: @Composable () -> Unit
) = CanvasTheme(content)
