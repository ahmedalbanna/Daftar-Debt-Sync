package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme =
  darkColorScheme(
    primary = TealDarkPrimary,
    secondary = EmeraldSecondary,
    tertiary = PaymentGreen,
    error = DebtRed,
    background = BackgroundDark,
    surface = SurfaceDarkCard,
    onBackground = Color.White,
    onSurface = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    tertiaryContainer = Color(0xFF00513B),
    onTertiaryContainer = Color(0xFFD1FAE5)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TealPrimary,
    secondary = EmeraldSecondary,
    tertiary = PaymentGreen,
    error = DebtRed,
    background = BackgroundLight,
    surface = SurfaceCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    errorContainer = DebtRedContainer,
    onErrorContainer = OnDebtRedContainer,
    tertiaryContainer = PaymentGreenContainer,
    onTertiaryContainer = OnPaymentGreenContainer
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
