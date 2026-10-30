package de.gerry.dranbleiben.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import de.gerry.dranbleiben.data.HABIT_COLORS

// Akzentfarbe von Dranbleiben: Beere (Kopffrei Indigo, Teller Safran, Eigengewicht Orange, Microstakes Grün)
private val LightColors = lightColorScheme(
    primary = Color(0xFFB4235A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E3),
    onPrimaryContainer = Color(0xFF3E0019),
    secondary = Color(0xFF74565F),
    secondaryContainer = Color(0xFFFFD9E3),
    onSecondaryContainer = Color(0xFF2B151C),
    tertiary = Color(0xFF3F7A2E),
    background = Color(0xFFFBF7F8),
    surface = Color(0xFFFBF7F8),
    surfaceContainerLow = Color(0xFFF6EFF1),
    surfaceContainer = Color(0xFFF1E8EB),
    surfaceContainerHigh = Color(0xFFEADFE3),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB0C8),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7D2949),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = Color(0xFFE3BDC6),
    secondaryContainer = Color(0xFF5A3F47),
    onSecondaryContainer = Color(0xFFFFD9E3),
    tertiary = Color(0xFFA6D58F),
    background = Color(0xFF121012),
    surface = Color(0xFF121012),
    surfaceContainerLow = Color(0xFF1B181A),
    surfaceContainer = Color(0xFF221E20),
    surfaceContainerHigh = Color(0xFF2C2729),
)

/** Farbe einer Gewohnheit, im Dunkeln etwas heller */
@Composable
fun habitColor(index: Int): Color {
    val base = Color(HABIT_COLORS[index.coerceIn(0, HABIT_COLORS.lastIndex)])
    return if (isSystemInDarkTheme()) lerp(base, Color.White, 0.3f) else base
}

private fun lerp(a: Color, b: Color, f: Float) =
    Color(a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, 1f)

@Composable
fun DranbleibenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
