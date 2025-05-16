package com.g2.chatroom.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

//Primary colors
val PrimaryBlue = Color(0xFF212832)
val PrimaryLightBlue = Color(0xFF14FFEC)
val PrimaryPurple = Color(0xFF6650a4)
val BackgroundWhite = Color(0xFF393D46)
val TextWhite = Color.White

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,//set
    onPrimary = PrimaryLightBlue, //set
    background = BackgroundWhite, //set

    surface = BackgroundWhite,
    secondary = PrimaryLightBlue,
    tertiary = PrimaryPurple,
    onSecondary =Color(0xFF0D7377) ,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,//set
    onPrimary = PrimaryLightBlue, //set
    background = BackgroundWhite, //set

    surface = BackgroundWhite,
    secondary = PrimaryLightBlue,
    tertiary = PrimaryPurple,
    onSecondary =Color(0xFF0D7377) ,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
)


@Composable
fun ChatRoomTheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}