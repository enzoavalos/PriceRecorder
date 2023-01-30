package com.example.pricerecorder

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.accessibility.AccessibilityManager

/*Checks if touch exploration is enabled for people visually impaired in order to not vibrate the device in such cases*/
private fun Context.isTouchExplorationEnabled() : Boolean{
    val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
    return accessibilityManager?.isTouchExplorationEnabled ?: false
}


@Suppress("DEPRECATION")
fun vibrateDevice(context: Context,duration:Long = 100){
    if(context.isTouchExplorationEnabled())
        return

    if(Build.VERSION.SDK_INT >= 26)
        context.getSystemService(Vibrator::class.java).
        vibrate(VibrationEffect.createOneShot(duration,VibrationEffect.DEFAULT_AMPLITUDE))
    else
        (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(duration)
}