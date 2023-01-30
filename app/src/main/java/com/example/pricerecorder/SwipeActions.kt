package com.example.pricerecorder

import android.view.MotionEvent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*Class that extends the capabilities of the default Swipe to dismiss composable*/
data class SwipeActionsConfig(
    val threshold:Float,
    val icon:ImageVector,
    val iconTint: Color,
    val background: Color,
    val stayDismissed: Boolean,
    val onDismiss: () -> Unit
    )

val DefaultSwipeActionsConfig = SwipeActionsConfig(
    threshold = 0.2f,
    icon = Icons.Default.Menu,
    iconTint = Color.Transparent,
    background = Color.Transparent,
    stayDismissed = false,
    onDismiss = {},
)

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class)
@Composable
fun SwipeActions(
    modifier: Modifier = Modifier,
    startActionsConfig: SwipeActionsConfig = DefaultSwipeActionsConfig,
    endActionsConfig: SwipeActionsConfig = DefaultSwipeActionsConfig,
    showTutorial: Boolean = false,
    dismissTutorial:() -> Unit = {},
    vibrateOnActionTriggered:() -> Unit = {},
    content: @Composable (DismissState) -> Unit
) = BoxWithConstraints(modifier) {
    val width = constraints.maxWidth.toFloat()

    /*state used to know if the users current swipe will cause an action. It*/
    var willDismissDirection: DismissDirection? by remember {
        mutableStateOf(null)
    }

    /*Function called when user swipes pass the threshold. First its checked if willDismissDirection state
    * corresponds to the one passed to the function. If so, the onDismiss function is called and then
    * stayDismissed boolean is returned, true if the item must disappear*/
    val state = rememberDismissState(
        initialValue = DismissValue.Default,
        confirmStateChange = {
            if (willDismissDirection == DismissDirection.StartToEnd
                && it == DismissValue.DismissedToEnd
            ) {
                startActionsConfig.onDismiss()
                startActionsConfig.stayDismissed
            } else if (willDismissDirection == DismissDirection.EndToStart &&
                it == DismissValue.DismissedToStart
            ) {
                endActionsConfig.onDismiss()
                endActionsConfig.stayDismissed
            } else {
                false
            }
        }
    )

    /*Variable used to keep track of what item shows the swipe tutorial, and to reset its state through an animation
    * when the tutorial animation is dismissed*/
    var finishAnimation by remember {
        mutableStateOf(showTutorial)
    }

    /*Tutorial shown to the user to indicate that the item can be swiped. The animation is shown repeatedly
    * until the user swipes the element*/
    if (showTutorial) {
        val infiniteTransition = rememberInfiniteTransition()
        val x by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if(startActionsConfig != DefaultSwipeActionsConfig) (width * (startActionsConfig.threshold) / 1.5f)
                else -(width * (startActionsConfig.threshold) / 1.5f),
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing, delayMillis = 1000),
                repeatMode = RepeatMode.Reverse
            )
        )

        LaunchedEffect(key1 = x, block = {
            state.performDrag(x - state.offset.value)
        })
    }else if (finishAnimation){
        val x by animateFloatAsState(
            targetValue = -state.offset.value,
            animationSpec = tween(500, easing = FastOutSlowInEasing))
        LaunchedEffect(key1 = null, block = {
            state.performDrag(x - state.offset.value)
        })
        finishAnimation = false
    }

    /*Haptic feedback refers to sensations delivered to users through the sense of touch, in this case
    * it vibrates the device if tha action is triggered*/
    LaunchedEffect(key1 = willDismissDirection, block = {
        if (willDismissDirection != null) {
            vibrateOnActionTriggered()
        }
    })

    /*Contains the action the user has triggered or null if it has not reached the threshold.
    * This allows to change the UI based on the current position and state of the swipe*/
    LaunchedEffect(key1 = Unit, block = {
        snapshotFlow { state.offset.value }
            .collect {
                willDismissDirection = when {
                    it > width * startActionsConfig.threshold -> DismissDirection.StartToEnd
                    it < -width * endActionsConfig.threshold -> DismissDirection.EndToStart
                    else -> null
                }
            }
    })

    val dismissDirections by remember(startActionsConfig, endActionsConfig) {
        derivedStateOf {
            mutableSetOf<DismissDirection>().apply {
                if (startActionsConfig != DefaultSwipeActionsConfig) add(DismissDirection.StartToEnd)
                if (endActionsConfig != DefaultSwipeActionsConfig) add(DismissDirection.EndToStart)
            }
        }
    }

    SwipeToDismiss(
        state = state,
        /*inter op filter provides access to the underlying MotionEvent's dispatched*/
        modifier = Modifier.pointerInteropFilter {
            /*Event action down refers to when a pressed gesture has started and its used to disable the tutorial
            * shown for the swipe element*/
            if (it.action == MotionEvent.ACTION_DOWN) {
                dismissTutorial()
            }
            false
        },
        directions = dismissDirections,
        dismissThresholds = {
            if(it == DismissDirection.StartToEnd)
                FractionalThreshold(startActionsConfig.threshold)
            else
                FractionalThreshold(endActionsConfig.threshold)
        },
        background = {
            /*The animated content component inverts the colors of the icon and the background using a circle
            * reveal animation*/
            AnimatedContent(
                targetState = Pair(state.dismissDirection, willDismissDirection != null),
                transitionSpec = {
                    fadeIn(
                        tween(0),
                        initialAlpha = if (targetState.second) 1f else 0f,
                    ) with fadeOut(
                        tween(0),
                        targetAlpha = if (targetState.second) .7f else 0f,
                    )
                }
            ){ (direction,willDismiss) ->
                val revealSize = remember { Animatable(if (willDismiss) 0f else 1f) }
                val iconSize = remember { Animatable(if (willDismiss) .8f else 1f) }
                /*Bounce animation for the background icon*/
                LaunchedEffect(key1 = Unit, block = {
                    if (willDismiss) {
                        revealSize.snapTo(0f)
                        launch {
                            revealSize.animateTo(1f, animationSpec = tween(400))
                        }
                        iconSize.snapTo(.8f)
                        iconSize.animateTo(
                            1.45f,
                            spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                            )
                        )
                        iconSize.animateTo(
                            1f,
                            spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                            )
                        )
                    }
                })

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CirclePath(
                            revealSize.value,
                            direction == DismissDirection.StartToEnd
                        ))
                        .background(
                            color = when (direction) {
                                DismissDirection.StartToEnd -> if (willDismiss) startActionsConfig.background else startActionsConfig.iconTint
                                DismissDirection.EndToStart -> if (willDismiss) endActionsConfig.background else endActionsConfig.iconTint
                                else -> Color.Transparent
                            },
                        )
                ) {
                    Box(modifier = Modifier
                        .align(
                            when (direction) {
                                DismissDirection.StartToEnd -> Alignment.CenterStart
                                else -> Alignment.CenterEnd
                            }
                        )
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .scale(iconSize.value)
                        .offset { IntOffset(x = 0, y = (10 * (1f - iconSize.value)).roundToInt()) },
                        contentAlignment = Alignment.Center
                    ) {
                        when (direction) {
                            DismissDirection.StartToEnd -> {
                                Image(
                                    painter = rememberVectorPainter(image = startActionsConfig.icon),
                                    colorFilter = ColorFilter.tint(if (willDismiss) startActionsConfig.iconTint else startActionsConfig.background),
                                    contentDescription = null
                                )
                            }
                            DismissDirection.EndToStart -> {
                                Image(
                                    painter = rememberVectorPainter(image = endActionsConfig.icon),
                                    colorFilter = ColorFilter.tint(if (willDismiss) endActionsConfig.iconTint else endActionsConfig.background),
                                    contentDescription = null
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }){
        content(state)
    }
}

class CirclePath(private val progress: Float, private val start: Boolean) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val origin = Offset(
            x = if (start) 0f else size.width,
            y = size.center.y,
        )

        val radius = (sqrt(
            size.height * size.height + size.width * size.width
        ) * 1f) * progress

        return Outline.Generic(
            Path().apply {
                addOval(
                    Rect(
                        center = origin,
                        radius = radius,
                    )
                )
            }
        )
    }
}