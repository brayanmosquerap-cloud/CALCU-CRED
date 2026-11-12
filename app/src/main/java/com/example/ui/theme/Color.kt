package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Base / Shared Clinical Neutral Colors
val AppBackground = Color(0xFFEEF2F7)
val CardSurface = Color(0xFFFFFFFF)
val SoftGrayBg = Color(0xFFF3F6FA)

val DarkText = Color(0xFF1E2638)
val SlateText = Color(0xFF5A687C)
val MutedText = Color(0xFF64748B)
val BorderSubtleGray = Color(0xFFE2E8F0)

// Clinical Semantic Colors
// Normal / Eutrófico (Semáforo Verde)
val ClinicalGreen = Color(0xFF15803D)
val DarkGreenPill = Color(0xFF14532D)
val SoftGreenBg = Color(0xFFE7F8ED)
val SoftGreenPill = Color(0xFFDCFCE7)
val BorderGreen = Color(0xFF22C55E)
val BorderSoftGreen = Color(0xFF86EFAC)

// Alerta / Sobrepeso / Rezago (Semáforo Amarillo)
val WarningAmber = Color(0xFFD97706)
val WarningText = Color(0xFF92400E)
val SoftAmberBg = Color(0xFFFFFBEB)
val SoftAmberPill = Color(0xFFFEF3C7)
val BorderAmber = Color(0xFFFCD34D)

// Severo / Riesgo / Alarma (Semáforo Rojo)
val AlertRed = Color(0xFFDC2626)
val DarkRedText = Color(0xFF991B1B)
val SoftRedBg = Color(0xFFFEF2F2)
val BorderSoftRed = Color(0xFFFECACA)

val FemalePink = Color(0xFF9B4062)
val PrimaryBlue = Color(0xFF1E2638)
val DeepBlueCard = Color(0xFF1E2638)
val NavyBlue = Color(0xFF1E2638)

@Immutable
data class GenderThemePalette(
    val appBackground: Color,
    val softBg: Color,
    val softTint: Color,
    val borderLight: Color,
    val borderMedium: Color,
    val primary: Color,
    val deepCard: Color,
    val navy: Color,
    val headerTitle: Color,
    val sliderActiveTrack: Color,
    val sliderInactiveTrack: Color,
    val fabGradientStart: Color,
    val fabGradientEnd: Color,
    val chartGrid: Color,
    val chartBg: Color,
    val chartBorder: Color,
    val divider: Color,
    val onDeepCardSubtext: Color,
    val deepCardPillBg: Color,
    val deepCardPillBorder: Color,
    val deepCardDivider: Color,
    val deepCardBadgeBg: Color,
    val ctaSecondaryHighlight: Color,
    val ctaSecondaryBorder: Color
)

// Niño Palette (Matches calculadora_cb36dc18.png cool slate-blue & deep navy aesthetic)
val BoyPastelBluePalette = GenderThemePalette(
    appBackground = Color(0xFFEEF2F7),
    softBg = Color(0xFFF3F6FA),
    softTint = Color(0xFFE6EEF8),
    borderLight = Color(0xFFE2E8F0),
    borderMedium = Color(0xFFCBD5E1),
    primary = Color(0xFF0B57D0),
    deepCard = Color(0xFF1E2638),
    navy = Color(0xFF1E2638),
    headerTitle = Color(0xFF1E2638),
    sliderActiveTrack = Color(0xFFD8E0EC),
    sliderInactiveTrack = Color(0xFFE8EEF5),
    fabGradientStart = Color(0xFF334155),
    fabGradientEnd = Color(0xFF1E2638),
    chartGrid = Color(0xFFC9DDF8),
    chartBg = Color(0xFFEAF3FF),
    chartBorder = Color(0xFFB6D2F6),
    divider = Color(0xFFE2E8F0),
    onDeepCardSubtext = Color(0xFF94A3B8),
    deepCardPillBg = Color(0xFF334155),
    deepCardPillBorder = Color(0xFF475569),
    deepCardDivider = Color(0xFF334155),
    deepCardBadgeBg = Color(0xFF334155),
    ctaSecondaryHighlight = Color(0xFF334155),
    ctaSecondaryBorder = Color(0xFF475569)
)

// Niña Palette (Preserves exact calculadora.png UI chrome while applying WHO Pink to growth charts)
val GirlPastelPinkPalette = GenderThemePalette(
    appBackground = Color(0xFFEEF2F7),
    softBg = Color(0xFFF3F6FA),
    softTint = Color(0xFFE6EEF8),
    borderLight = Color(0xFFE2E8F0),
    borderMedium = Color(0xFFCBD5E1),
    primary = Color(0xFFD81B60),
    deepCard = Color(0xFF1E2638),
    navy = Color(0xFF1E2638),
    headerTitle = Color(0xFF1E2638),
    sliderActiveTrack = Color(0xFFD8E0EC),
    sliderInactiveTrack = Color(0xFFE8EEF5),
    fabGradientStart = Color(0xFF334155),
    fabGradientEnd = Color(0xFF1E2638),
    chartGrid = Color(0xFFF5C6D8),
    chartBg = Color(0xFFFDECF3),
    chartBorder = Color(0xFFF0B1C9),
    divider = Color(0xFFE2E8F0),
    onDeepCardSubtext = Color(0xFF94A3B8),
    deepCardPillBg = Color(0xFF334155),
    deepCardPillBorder = Color(0xFF475569),
    deepCardDivider = Color(0xFF334155),
    deepCardBadgeBg = Color(0xFF334155),
    ctaSecondaryHighlight = Color(0xFF334155),
    ctaSecondaryBorder = Color(0xFF475569)
)

val LocalGenderPalette = staticCompositionLocalOf { BoyPastelBluePalette }
