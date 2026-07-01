package com.minded.pro.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- Brand palette -------------------------------------------------------

private val Ink = Color(0xFF100E18)         // app background
private val Panel = Color(0xFF1C1930)       // card surface
private val PanelRaised = Color(0xFF2A2640) // chips, tracks
private val TextBright = Color(0xFFEDEAF6)
private val TextMuted = Color(0xFF9C98B6)

/** Gold — the "Pro" signature accent, used for the spoken word. */
val ProGold = Color(0xFFE6B450)

/** Violet — connection / signal accents. */
val SignalAccent = Color(0xFF9B6CFF)

/** Amber — the attention metric. */
val AttentionAccent = Color(0xFFFFB23E)

/** Teal — the meditation metric. */
val MeditationAccent = Color(0xFF53D8C4)

/** Pink — blink events and error states. */
val BlinkAccent = Color(0xFFFF5C8A)

/** Neutral arc / bar track drawn behind coloured values. */
val TrackColor = Color(0xFF302C4C)

/** Dimmed caption text. */
val MutedText = TextMuted

private val MindedProColors = darkColorScheme(
    primary = ProGold,
    onPrimary = Ink,
    secondary = MeditationAccent,
    onSecondary = Ink,
    tertiary = AttentionAccent,
    background = Ink,
    onBackground = TextBright,
    surface = Panel,
    onSurface = TextBright,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = TextMuted,
    error = BlinkAccent,
    onError = Ink,
)

/**
 * The single Material 3 theme used across Minded Pro. The app is dark-only by
 * design; [darkTheme] is accepted for preview tooling but does not change the
 * palette.
 */
@Composable
fun MindedProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = MindedProColors,
        typography = Typography(),
        content = content,
    )
}
