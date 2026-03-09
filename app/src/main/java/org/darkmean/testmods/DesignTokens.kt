package org.darkmean.testmods

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────
// DESIGN TOKENS
// ─────────────────────────────────────────────────────────────────────
val ColorBgDeep        = Color(0xFF0A0A0A)
val ColorBgCard        = Color(0xFF161616)
val ColorBgStroke      = Color(0xFF2A2A2A)
val ColorTextPrimary   = Color(0xFFEEEEEE)
val ColorTextSecondary = Color(0xFF888888)
val ColorTextMuted     = Color(0xFF444444)
val ColorError         = Color(0xFFFF5252)

// ─────────────────────────────────────────────────────────────────────
// APP THEME
// ─────────────────────────────────────────────────────────────────────
data class AppTheme(
    val primary:        Color,
    val primaryDark:    Color,
    val accent:         Color,
    val onPrimary:      Color,
    val bgTint:         Color,
    val cardTint:       Color,
    val avatarBg:       List<Color>,
    val gradientColors: List<Color>
)

val allThemes = listOf(
    // Blue
    AppTheme(
        primary        = Color(0xFF40C4FF),
        primaryDark    = Color(0xFF0091EA),
        accent         = Color(0xFF80D8FF),
        onPrimary      = Color(0xFF00131F),
        bgTint         = Color(0xFF40C4FF),
        cardTint       = Color(0xFF0D2A3D),
        avatarBg       = listOf(Color(0xFF0D1F30), Color(0xFF061525)),
        gradientColors = listOf(Color(0xFF0091EA), Color(0xFF40C4FF), Color(0xFF80D8FF))
    ),
    // Green
    AppTheme(
        primary        = Color(0xFF00E676),
        primaryDark    = Color(0xFF00C853),
        accent         = Color(0xFF69F0AE),
        onPrimary      = Color(0xFF001A0A),
        bgTint         = Color(0xFF00E676),
        cardTint       = Color(0xFF1A3D28),
        avatarBg       = listOf(Color(0xFF1A2F1A), Color(0xFF0D1F0D)),
        gradientColors = listOf(Color(0xFF00C853), Color(0xFF00E676), Color(0xFF69F0AE))
    ),
    // Yellow
    AppTheme(
        primary        = Color(0xFFFFD600),
        primaryDark    = Color(0xFFFFAB00),
        accent         = Color(0xFFFFFF8D),
        onPrimary      = Color(0xFF1A1200),
        bgTint         = Color(0xFFFFD600),
        cardTint       = Color(0xFF3D3000),
        avatarBg       = listOf(Color(0xFF2A2000), Color(0xFF1A1500)),
        gradientColors = listOf(Color(0xFFFFAB00), Color(0xFFFFD600), Color(0xFFFFFF8D))
    ),
    // Red
    AppTheme(
        primary        = Color(0xFFFF5252),
        primaryDark    = Color(0xFFD50000),
        accent         = Color(0xFFFF8A80),
        onPrimary      = Color(0xFF1F0000),
        bgTint         = Color(0xFFFF5252),
        cardTint       = Color(0xFF3D0D0D),
        avatarBg       = listOf(Color(0xFF2A0D0D), Color(0xFF1A0505)),
        gradientColors = listOf(Color(0xFFD50000), Color(0xFFFF5252), Color(0xFFFF8A80))
    ),
    // Purple
    AppTheme(
        primary        = Color(0xFFD500F9),
        primaryDark    = Color(0xFFAA00FF),
        accent         = Color(0xFFEA80FC),
        onPrimary      = Color(0xFF1A0020),
        bgTint         = Color(0xFFD500F9),
        cardTint       = Color(0xFF2A0040),
        avatarBg       = listOf(Color(0xFF1A0030), Color(0xFF0D0020)),
        gradientColors = listOf(Color(0xFFAA00FF), Color(0xFFD500F9), Color(0xFFEA80FC))
    ),
    // Orange
    AppTheme(
        primary        = Color(0xFFFF6D00),
        primaryDark    = Color(0xFFDD2C00),
        accent         = Color(0xFFFFAB40),
        onPrimary      = Color(0xFF1A0A00),
        bgTint         = Color(0xFFFF6D00),
        cardTint       = Color(0xFF3D1800),
        avatarBg       = listOf(Color(0xFF2A1000), Color(0xFF1A0800)),
        gradientColors = listOf(Color(0xFFDD2C00), Color(0xFFFF6D00), Color(0xFFFFAB40))
    )
)

val sessionTheme: AppTheme = allThemes[java.util.Random().nextInt(allThemes.size)]
var currentTheme: AppTheme = sessionTheme

val ThemePrimary   get() = currentTheme.primary
val ThemeDark      get() = currentTheme.primaryDark
val ThemeAccent    get() = currentTheme.accent
val ThemeOnPrimary get() = currentTheme.onPrimary
val ThemeBgTint    get() = currentTheme.bgTint
val ThemeCardTint  get() = currentTheme.cardTint
val ThemeAvatarBg  get() = currentTheme.avatarBg
val ThemeGradient  get() = currentTheme.gradientColors