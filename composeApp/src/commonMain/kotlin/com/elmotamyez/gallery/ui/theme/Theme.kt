package com.elmotamyez.gallery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette — pixel-sampled from the reference UI ────────────────────────────
//
//  #08396C  deep navy         → primary brand colour (header, FABs, active nav)
//  #05417F  navy variant      → pressed / darker primary
//  #3F769C  medium blue       → secondary / action links ("View All", chips)
//  #F0F4F8  light blue-gray   → page background
//  #FFFFFF  white             → card / surface
//  #F5F7FA  near-white        → surface variant (input bg, alternate rows)
//  #C3D3D7  steel blue-gray   → outlines, borders
//  #1A2A3A  dark navy-text    → primary text on light surfaces
//  #6B7E90  slate             → secondary / hint text
// ─────────────────────────────────────────────────────────────────────────────

// Primary — deep navy
private val Navy700          = Color(0xFF08396C)
private val Navy800          = Color(0xFF05417F)
private val NavyContainer    = Color(0xFFD0E4F7)   // light tint for chips, badges
private val OnNavyContainer  = Color(0xFF08396C)

// Secondary — medium action blue
private val Blue500          = Color(0xFF3F769C)
private val BlueContainer    = Color(0xFFDCEEFA)
private val OnBlueContainer  = Color(0xFF1D4F6E)

// Backgrounds & surfaces
private val PageBg           = Color(0xFFF0F4F8)   // ← sampled from reference
private val CardWhite        = Color(0xFFFFFFFF)
private val SurfaceVariant   = Color(0xFFF5F7FA)
private val SteelOutline     = Color(0xFFC3D3D7)
private val SubtleOutline    = Color(0xFFE2EAF0)

// Text
private val TextPrimary      = Color(0xFF000000)   // pure black — high legibility
private val TextSecondary    = Color(0xFF5A6475)   // medium grey
private val TextHint         = Color(0xFF9DAAB6)

// Error
private val Red500           = Color(0xFFE53935)
private val RedContainer     = Color(0xFFFFEDED)

// ── Light colour scheme ───────────────────────────────────────────────────────

private val AppColors = lightColorScheme(
    // Primary — deep navy
    primary              = Navy700,
    onPrimary            = Color.White,
    primaryContainer     = NavyContainer,
    onPrimaryContainer   = OnNavyContainer,

    // Secondary — action blue
    secondary            = Blue500,
    onSecondary          = Color.White,
    secondaryContainer   = BlueContainer,
    onSecondaryContainer = OnBlueContainer,

    // Tertiary — slightly lighter navy for accents
    tertiary             = Navy800,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFE8F2FC),
    onTertiaryContainer  = Navy700,

    // Backgrounds
    background           = PageBg,
    onBackground         = TextPrimary,

    // Surfaces
    surface              = CardWhite,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceVariant,
    onSurfaceVariant     = TextSecondary,

    // Outlines
    outline              = SteelOutline,
    outlineVariant       = SubtleOutline,

    // Error
    error                = Red500,
    onError              = Color.White,
    errorContainer       = RedContainer,
    onErrorContainer     = Color(0xFF8B0000),

    // Inverse
    inverseSurface       = Navy700,
    inverseOnSurface     = Color.White,
    inversePrimary       = Blue500,

    scrim                = Color(0x99000000),
)

// ── Entry-point ───────────────────────────────────────────────────────────────

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        content     = content
    )
}
