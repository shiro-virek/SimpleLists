package com.simplelists.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Teal = Color(0xFF1A6B5A)
private val TealLight = Color(0xFF63DBB4)
private val TealDark = Color(0xFF00382E)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA6F2D8),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4B635B),
    secondaryContainer = Color(0xFFCDE9DD),
    surface = Color(0xFFF6FBF7),
    background = Color(0xFFF6FBF7)
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF00382E),
    primaryContainer = Teal,
    onPrimaryContainer = Color(0xFFA6F2D8),
    secondary = Color(0xFFB1CCC1),
    surface = Color(0xFF101512),
    background = Color(0xFF101512)
)

@Composable
fun SimpleListsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
