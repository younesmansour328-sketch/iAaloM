package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MelonStudioColorScheme = darkColorScheme(
    primary = MelonCoral,
    onPrimary = TextPrimary,
    primaryContainer = MelonCoralContainer,
    onPrimaryContainer = MelonCoral,
    secondary = MelonMint,
    onSecondary = StudioBg,
    secondaryContainer = MelonMintContainer,
    onSecondaryContainer = MelonMint,
    tertiary = ResolveAmber,
    onTertiary = StudioBg,
    tertiaryContainer = ResolveAmberContainer,
    onTertiaryContainer = ResolveAmber,
    background = StudioBg,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = StudioBg,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighlight,
    outline = StudioBorder,
    outlineVariant = StudioBorder.copy(alpha = 0.5f),
    error = DangerRed,
    onError = TextPrimary
)

val MelonShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MelonEditorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MelonStudioColorScheme,
        typography = MelonTypography,
        shapes = MelonShapes,
        content = content
    )
}
