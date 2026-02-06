@file:Suppress("DEPRECATION")

package com.example.mbnui.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.mbnui.ui.theme.AppTypography

// 外部の Color.kt との衝突を避けるため、このファイルで使用する色を定義
private val ThemePurple80 = Color(0xFFE1BEE7) // More expressive purple
private val ThemePurpleGrey80 = Color(0xFFB39DDB) // Brighter secondary
private val ThemePink80 = Color(0xFFF8BBD9) // More vibrant tertiary

private val ThemePurple40 = Color(0xFF9C27B0) // Deeper primary for light
private val ThemePurpleGrey40 = Color(0xFF7B1FA2) // Richer secondary
private val ThemePink40 = Color(0xFFE91E63) // Vivid tertiary

// --- ダークモード用カラー構成 ---
private val DarkColorScheme = darkColorScheme(
    primary = ThemePurple80,
    secondary = ThemePurpleGrey80,
    tertiary = ThemePink80
)

// --- ライトモード用カラー構成 ---
private val LightColorScheme = lightColorScheme(
    primary = ThemePurple40,
    secondary = ThemePurpleGrey40,
    tertiary = ThemePink40
)

// --- カスタム形状の定義 ---
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun MbnuiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ExpressiveShapes,
        typography = AppTypography,
        content = content
    )
}
