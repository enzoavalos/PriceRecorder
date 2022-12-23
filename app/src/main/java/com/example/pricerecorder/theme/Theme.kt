package com.example.pricerecorder.theme

import android.content.Context
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import com.example.pricerecorder.ThemeUtils

/*Custom composable element that sets ups a MaterialTheme, admitting other composable elements as content.
* darkTheme parameter added to automatically determine if dark theme should be used depending the global
* configuration of the device and the user preferences*/
@Composable
fun PriceRecorderTheme(
    context:Context? = null,
    content:@Composable () -> Unit){
    val darkTheme = ThemeUtils.systemInDarkTheme(context)
    MaterialTheme(content = content, colors = if(!darkTheme) LightColors else DarkColors,
        typography = PriceRecorderTypography, shapes = PriceRecorderShapes)
}

private val LightColors = lightColors(
    primary = LightBlue,
    primaryVariant = PetrolBlue,
    secondary = LightPurple,
    background = ArticGrey,
    surface = White,
    error = OpaqueRed,
    onPrimary = White,
    onSecondary = White,
    onSurface = Black,
    onError = White
)

private val DarkColors = darkColors(
    primary = SteelBlue,
    primaryVariant = LightBlue,
    secondary = BrightOrange,
    background = WolfGrey,
    surface = OxfordBlue,
    error = OpaqueRed,
    onPrimary = White,
    onSecondary = White,
    onSurface = SilverGrey,
    onError = White
)