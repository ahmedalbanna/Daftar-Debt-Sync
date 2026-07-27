package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val CairoFontFamily = FontFamily.Default

// Set of Material typography styles to start with
val Typography =
    Typography(
        displayLarge = TextStyle(fontFamily = CairoFontFamily, fontSize = 57.sp, lineHeight = 64.sp),
        displayMedium = TextStyle(fontFamily = CairoFontFamily, fontSize = 45.sp, lineHeight = 52.sp),
        displaySmall = TextStyle(fontFamily = CairoFontFamily, fontSize = 36.sp, lineHeight = 44.sp),
        headlineLarge = TextStyle(fontFamily = CairoFontFamily, fontSize = 32.sp, lineHeight = 40.sp),
        headlineMedium = TextStyle(fontFamily = CairoFontFamily, fontSize = 28.sp, lineHeight = 36.sp),
        headlineSmall = TextStyle(fontFamily = CairoFontFamily, fontSize = 24.sp, lineHeight = 32.sp),
        titleLarge = TextStyle(fontFamily = CairoFontFamily, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
        titleMedium = TextStyle(fontFamily = CairoFontFamily, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
        titleSmall = TextStyle(fontFamily = CairoFontFamily, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(fontFamily = CairoFontFamily, fontSize = 16.sp, lineHeight = 22.sp),
        bodySmall = TextStyle(fontFamily = CairoFontFamily, fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = TextStyle(fontFamily = CairoFontFamily, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelMedium = TextStyle(fontFamily = CairoFontFamily, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontFamily = CairoFontFamily, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    )
