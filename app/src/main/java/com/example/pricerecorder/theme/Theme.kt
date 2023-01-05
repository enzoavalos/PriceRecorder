package com.example.pricerecorder.theme

import android.content.Context
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.*
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

/*Custom theme to be applied to text fields*/
@Composable
fun MaterialTheme.customTextFieldColors(
    darkThemeEnabled : Boolean = false
) : TextFieldColors = TextFieldDefaults.textFieldColors(
    backgroundColor = colors.background,
    cursorColor = colors.secondary.copy(ContentAlpha.medium),
    textColor = colors.onSurface,
    focusedIndicatorColor = colors.secondary.copy(ContentAlpha.high),
    unfocusedIndicatorColor = colors.onSurface.copy(ContentAlpha.medium),
    leadingIconColor = if(darkThemeEnabled) colors.onSurface.copy(alpha = 0.8f) else colors.primaryVariant,
    unfocusedLabelColor = if(darkThemeEnabled) colors.onSurface.copy(alpha = 0.8f) else colors.primaryVariant,
    trailingIconColor = if(darkThemeEnabled) colors.onSurface.copy(alpha = 0.8f) else colors.primaryVariant,
    focusedLabelColor = colors.secondary.copy(ContentAlpha.high)
)

@Composable
fun MaterialTheme.customTextFieldSelectionColors() = TextSelectionColors(
    handleColor = colors.secondary,
    backgroundColor = colors.secondary.copy(alpha = 0.5f)
)

private val LightColors = lightColors(
    primary = LightBlue,
    primaryVariant = PetrolBlue,
    secondary = LightPurple,
    background = SiberiaGey,
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