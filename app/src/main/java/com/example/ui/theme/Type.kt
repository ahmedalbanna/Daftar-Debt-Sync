package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val CairoFont = GoogleFont("Cairo")

val CairoFontFamily = FontFamily(
    Font(googleFont = CairoFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = CairoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = CairoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = CairoFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = CairoFont, fontProvider = provider, weight = FontWeight.Bold),
)

// Set of Material typography styles to start with
val Typography =
    Typography(
        displayLarge = TextStyle(fontFamily = FontFamily.Default),
        displayMedium = TextStyle(fontFamily = FontFamily.Default),
        displaySmall = TextStyle(fontFamily = FontFamily.Default),
        headlineLarge = TextStyle(fontFamily = FontFamily.Default),
        headlineMedium = TextStyle(fontFamily = FontFamily.Default),
        headlineSmall = TextStyle(fontFamily = FontFamily.Default),
        titleLarge = TextStyle(fontFamily = FontFamily.Default),
        titleMedium = TextStyle(fontFamily = FontFamily.Default),
        titleSmall = TextStyle(fontFamily = FontFamily.Default),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default),
        bodySmall = TextStyle(fontFamily = FontFamily.Default),
        labelLarge = TextStyle(fontFamily = FontFamily.Default),
        labelMedium = TextStyle(fontFamily = FontFamily.Default),
        labelSmall = TextStyle(fontFamily = FontFamily.Default)
    )
