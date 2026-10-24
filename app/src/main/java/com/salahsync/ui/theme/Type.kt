package com.salahsync.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.salahsync.R

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int, style: FontStyle = FontStyle.Normal) = Font(
    resId = res,
    weight = FontWeight(weight),
    style = style,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Display serif for headings and prayer names. */
val Cormorant = FontFamily(
    variable(R.font.cormorant_garamond, 500),
    variable(R.font.cormorant_garamond, 600),
    variable(R.font.cormorant_garamond_italic, 500, FontStyle.Italic),
)

/** Clean sans for body text and times. */
val Manrope = FontFamily(
    variable(R.font.manrope, 400),
    variable(R.font.manrope, 500),
    variable(R.font.manrope, 600),
)

/** Mushaf script for Quranic and Arabic text. */
val AmiriQuran = FontFamily(Font(R.font.amiri_quran))

val SalahTypography = Typography(
    displayLarge = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight(500), fontSize = 52.sp, lineHeight = 54.sp),
    displayMedium = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight(500), fontSize = 40.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight(500), fontSize = 34.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight(500), fontSize = 28.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight(500), fontSize = 24.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight(500), fontSize = 21.sp, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(400), fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(400), fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(600), fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(500), fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(500), fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.18.em),
)

/** Uppercase eyebrow label, as used above cards. */
val Eyebrow = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(500), fontSize = 11.sp, letterSpacing = 0.18.em)

val ArabicStyle = TextStyle(fontFamily = AmiriQuran, fontSize = 20.sp, lineHeight = 36.sp, textDirection = TextDirection.Rtl)

val TabularTime = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(500), fontSize = 16.sp, fontFeatureSettings = "tnum")

/** Nastaliq for Urdu translation; it needs generous line height for its descenders. */
val NotoNastaliqUrdu = FontFamily(Font(R.font.noto_nastaliq_urdu))

val UrduStyle = TextStyle(fontFamily = NotoNastaliqUrdu, fontSize = 18.sp, lineHeight = 42.sp, textDirection = TextDirection.Rtl)

/** Quran text in the reader, at the reader's chosen size. */
fun quranStyle(sizeSp: Int) = TextStyle(
    fontFamily = AmiriQuran,
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * 2.05f).sp,
    textDirection = TextDirection.Rtl,
)
