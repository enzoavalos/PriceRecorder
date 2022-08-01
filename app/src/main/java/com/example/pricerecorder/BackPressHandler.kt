package com.example.pricerecorder

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**/
@Composable
fun BackPressHandler(
    backPressedDispatcher:OnBackPressedDispatcher? = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher,
    onBackPressed:() -> Unit){
    /*Used for side effects that should not cause a recomposition and to ensure that they are correctly used with their
    * last updated value*/
    val currentOnBackPressed = rememberUpdatedState(newValue = onBackPressed)

    /*A callback is created and added to the dispatcher that controls the dispatching system back presses
    * This callback is enabled each time the composable is recomposed, disabling other callbacks responsible for
    * back press handling*/
    val backCallback = remember {
        object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                currentOnBackPressed.value()
            }
        }
    }

    /*Disposable effects is used for side effects that need to be cleaned up when the key changes or the composable
    * leaves the composition*/
    DisposableEffect(key1 = backPressedDispatcher){
        backPressedDispatcher?.addCallback(backCallback)
        /*Callback is removed on dispose*/
        onDispose{
            backCallback.remove()
        }
    }
}