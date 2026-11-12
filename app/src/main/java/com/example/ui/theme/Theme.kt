package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.model.SexoBiologico

@Composable
fun MyApplicationTheme(
    sexo: SexoBiologico = SexoBiologico.NINO,
    content: @Composable () -> Unit
) {
    val palette = if (sexo == SexoBiologico.NINO) {
        BoyPastelBluePalette
    } else {
        GirlPastelPinkPalette
    }

    val colorScheme = lightColorScheme(
        primary = palette.primary,
        onPrimary = Color.White,
        primaryContainer = palette.softTint,
        onPrimaryContainer = palette.navy,
        secondary = ClinicalGreen,
        onSecondary = Color.White,
        secondaryContainer = SoftGreenPill,
        onSecondaryContainer = DarkGreenPill,
        tertiary = WarningAmber,
        background = palette.appBackground,
        onBackground = DarkText,
        surface = CardSurface,
        onSurface = DarkText,
        surfaceVariant = palette.softBg,
        onSurfaceVariant = SlateText,
        outline = palette.borderMedium,
        outlineVariant = palette.borderLight
    )

    CompositionLocalProvider(LocalGenderPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
