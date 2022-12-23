package com.example.pricerecorder

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

object ThemeUtils{
    const val KEY_NIGHT_MODE = "nightMode"

    /*Verifies if the user has previously specified a preference for dark theme*/
    private fun isUserPreferenceDarkTheme(context: Context):Boolean {
        val appSettingPrefs = context.getSharedPreferences("AppSettingPrefs", Context.MODE_PRIVATE)
        return (appSettingPrefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                == AppCompatDelegate.MODE_NIGHT_YES)
    }

    /*Gets the user preference previously set regarding app theming and applies it*/
    fun setUserPreferredThemeMode(context: Context){
        val appSettingPrefs = context.getSharedPreferences("AppSettingPrefs", Context.MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(appSettingPrefs.getInt(KEY_NIGHT_MODE,AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
    }

    @Composable
    fun systemInDarkTheme(context: Context? = null):Boolean{
        return when(context){
            null -> isSystemInDarkTheme()
            else -> isSystemInDarkTheme() or isUserPreferenceDarkTheme(context)
        }
    }
}