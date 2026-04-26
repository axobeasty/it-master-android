package ru.itmaster.schedule.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BluePrimary = Color(0xFF1565C0)
private val BlueSecondary = Color(0xFF5C92D2)

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
)

@Composable
fun ItMasterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
