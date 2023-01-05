package com.example.pricerecorder

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityManager

fun View.vibrate() = customPerformHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

private fun View.customPerformHapticFeedback(feedbackConstant: Int ){
    if(context.isTouchExplorationEnabled())
        return

    isHapticFeedbackEnabled = true
    performHapticFeedback(feedbackConstant,HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
}

/*Checks if touch exploration is enabled for people visually impaired in order to not vibrate the device in such cases*/
private fun Context.isTouchExplorationEnabled() : Boolean{
    val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
    return accessibilityManager?.isTouchExplorationEnabled ?: false
}