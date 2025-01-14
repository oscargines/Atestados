package com.oscar.atestados.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BlueGray800,            // Color principal para elementos destacados
    secondary = BlueGray400,          // Color secundario para elementos complementarios
    tertiary = Orange800,             // Color terciario para elementos adicionales
    onPrimary = White,                // Color del texto sobre colores principales
    onSecondary = White,              // Color del texto sobre colores secundarios
    onTertiary = White,               // Color del texto sobre colores terciarios
    surfaceVariant = BlueGray900,     // Variante del color de superficie, útil para fondos secundarios
    background = White,               // Fondo principal de la app
    onBackground = Black,             // Color del texto sobre el fondo principal
    surface = Color(0xFFEEEEEE), // Color de superficie, como tarjetas y diálogos
    onSurface = Color.Black           // Color del texto sobre las superficies


    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun AtestadosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {


    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}