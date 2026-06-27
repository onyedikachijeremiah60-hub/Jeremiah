package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LuxuryGold,
    onPrimary = LuxuryBlack,
    primaryContainer = LuxuryGoldAccent,
    onPrimaryContainer = LuxuryWhite,
    secondary = LuxuryGoldAccent,
    onSecondary = LuxuryWhite,
    background = LuxuryBlack,
    onBackground = LuxuryWhite,
    surface = LuxuryDarkGrey,
    onSurface = LuxuryWhite,
    outline = LuxuryBorderGrey
)

private val LightColorScheme = lightColorScheme(
    primary = LuxuryGold,
    onPrimary = LuxuryBlack,
    primaryContainer = LuxuryGoldAccent,
    onPrimaryContainer = LuxuryWhite,
    secondary = LuxuryGoldAccent,
    onSecondary = LuxuryWhite,
    background = LuxuryLightBg,
    onBackground = LuxuryBlack,
    surface = LuxuryLightSurface,
    onSurface = LuxuryBlack,
    outline = LuxuryBorderGrey
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamic color to false by default to enforce our premium brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
