package com.example.mbnui.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 外部の Color.kt との衝突を避けるため、このファイルで使用する色を定義
private val ThemePurple80 = Color(0xFFD0BCFF)
private val ThemePurpleGrey80 = Color(0xFFCCC2DC)
private val ThemePink80 = Color(0xFFEFB8C8)

private val ThemePurple40 = Color(0xFF6650a4)
private val ThemePurpleGrey40 = Color(0xFF625b71)
private val ThemePink40 = Color(0xFF7D5260)

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
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

