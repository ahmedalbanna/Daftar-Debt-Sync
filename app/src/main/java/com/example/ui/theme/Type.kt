package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold),
    Font(R.font.cairo, FontWeight.Light),
)

// Set of Material typography styles to start with
val Typography =
    Typography(
        displayLarge = TextStyle(fontFamily = CairoFontFamily),
        displayMedium = TextStyle(fontFamily = CairoFontFamily),
        displaySmall = TextStyle(fontFamily = CairoFontFamily),
        headlineLarge = TextStyle(fontFamily = CairoFontFamily),
        headlineMedium = TextStyle(fontFamily = CairoFontFamily),
        headlineSmall = TextStyle(fontFamily = CairoFontFamily),
        titleLarge = TextStyle(fontFamily = CairoFontFamily),
        titleMedium = TextStyle(fontFamily = CairoFontFamily),
        titleSmall = TextStyle(fontFamily = CairoFontFamily),
        bodyLarge = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(fontFamily = CairoFontFamily),
        bodySmall = TextStyle(fontFamily = CairoFontFamily),
        labelLarge = TextStyle(fontFamily = CairoFontFamily),
        labelMedium = TextStyle(fontFamily = CairoFontFamily),
        labelSmall = TextStyle(fontFamily = CairoFontFamily)
    )
