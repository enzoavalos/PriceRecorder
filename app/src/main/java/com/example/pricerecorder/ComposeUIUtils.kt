package com.example.pricerecorder

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pricerecorder.theme.PriceRecorderShapes
import com.example.pricerecorder.theme.PriceRecorderTheme
import com.example.pricerecorder.theme.customTextFieldColors
import com.example.pricerecorder.theme.customTextFieldSelectionColors

data class ChipItem(
    val text:String,
    var onClick: () -> Unit = {},
    val leadingIcon:@Composable (() -> Unit)?
)

data class RadioButtonItem(
    val text:String,
    val onClick: () -> Unit = {},
    var selected: Boolean = false
)

data class FabItem(
    val label:String,
    val onClick: () -> Unit,
    val icon: ImageVector,
    var enabled: Boolean = true
)

enum class MultiFloatingButtonState {
    Expanded,Collapsed
}

/*Implements a horizontal scrollable chip list*/
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CustomChipList(items:List<ChipItem>,
    modifier: Modifier = Modifier){
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, chipItem ->
            if(index < items.size){
                Spacer(modifier = Modifier.width(8.dp))
            }

            Chip(
                onClick = chipItem.onClick,
                border = BorderStroke(
                    color = MaterialTheme.colors.secondary,
                    width = 2.dp
                ),
                leadingIcon = chipItem.leadingIcon,
                colors = ChipDefaults.chipColors(
                    backgroundColor = MaterialTheme.colors.primaryVariant.copy(0.8f),
                    contentColor = MaterialTheme.colors.onPrimary
                )) {
                Text(text = chipItem.text,
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.onPrimary)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CustomAlertDialog(show:Boolean, title:String, onConfirm:() -> Unit, onDismiss:()->Unit,
                      msg:@Composable (()->Unit)?, confirmButtonText:String? = null,
                      dismissButtonText:String? = null,
                      useDefaultWidth:Boolean = true){
    if(!show)
        return
    AlertDialog(
        onDismissRequest = { onDismiss() },
        shape = MaterialTheme.shapes.medium.copy(CornerSize(10.dp)),
        title = {
            Text(text = title,
                color = MaterialTheme.colors.onSurface)
        },
        text = msg,
        backgroundColor = MaterialTheme.colors.surface,
        confirmButton = {
            confirmButtonText?.let{
                Button(onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent,
                        contentColor = MaterialTheme.colors.secondary),
                    elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)) {
                    Text(text = confirmButtonText, style = MaterialTheme.typography.h6)
                }
            }},
        dismissButton = {
            dismissButtonText?.let {
                Button(onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent,
                        contentColor = Color.Red),
                    elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)) {
                    Text(text = dismissButtonText, style = MaterialTheme.typography.h6)
                }
            }
        },
        properties = DialogProperties(dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = useDefaultWidth)
    )
}

/*Menu that displays a set of options for the user to select*/
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExposedDropdownMenu(
    value: String,
    options:List<String>,
    label:@Composable (() -> Unit)?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon:@Composable (() -> Unit)? = null,
    helperText:String? = null,
    darkThemeEnabled: Boolean = false
){
    var expanded by remember { mutableStateOf(false) }

    MaterialTheme(shapes = PriceRecorderShapes.copy(medium = RoundedCornerShape(0.dp))){
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = modifier) {
            Column(modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start) {
                OutlinedTextField(value = value,
                    onValueChange = {
                        onValueChange(it)
                    },
                    readOnly = true,
                    label = label,
                    modifier = Modifier
                        .fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = leadingIcon,
                    trailingIcon = {
                        if(value.isEmpty())
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        else
                            IconButton(onClick = {
                                onValueChange("")
                                expanded = false }) {
                                Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "")
                            }
                    },
                    textStyle = MaterialTheme.typography.subtitle1,
                    colors = MaterialTheme.customTextFieldColors(
                        darkThemeEnabled = darkThemeEnabled
                    ))

                TextFieldDecorators(helperText = helperText)
            }

            ExposedDropdownMenu(expanded = (expanded and options.isNotEmpty()),
                onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index,option ->
                    Column {
                        DropdownMenuItem(onClick = {
                            expanded = false
                            onValueChange(option)
                        }) {
                            Text(text = option,
                                style = MaterialTheme.typography.subtitle2)
                        }

                        if(index < options.size-1){
                            Divider(thickness = 1.dp,
                                color = MaterialTheme.colors.secondary.copy(0.7f),
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

/*Implements a text field that shows predictions menu based on the user input*/
@SuppressLint("UnrememberedMutableState")
@Composable
fun AutoCompleteTextField(
    value: String,
    label:@Composable (() -> Unit)?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxAllowedChars:Int? = null,
    leadingIcon:@Composable (() -> Unit)? = null,
    trailingIcon:@Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    keyboardActions: KeyboardActions = KeyboardActions(),
    isError: Boolean = false,
    predictions:List<String>,
    itemContent:@Composable (String) -> Unit,
    darkThemeEnabled: Boolean = false
){
    val charCount = value.length
    val showTrailingIcon by derivedStateOf { value.isNotEmpty() }
    val showPredictions = remember {
        mutableStateOf(false)
    }

    CompositionLocalProvider(LocalTextSelectionColors provides MaterialTheme.customTextFieldSelectionColors()){
        Column(modifier = modifier
            .background(Color.Transparent),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Top) {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    var newValue = it
                    maxAllowedChars?.let { limit ->
                        if(it.length > limit)
                            newValue = it.dropLast(it.length - limit)
                    }
                    onValueChange(newValue)
                    showPredictions.value = true
                },
                label = label,
                modifier = Modifier
                    .fillMaxWidth(),
                singleLine = true,
                leadingIcon = leadingIcon,
                trailingIcon = if(showTrailingIcon) trailingIcon else null,
                textStyle = MaterialTheme.typography.subtitle1,
                colors = MaterialTheme.customTextFieldColors(
                    darkThemeEnabled = darkThemeEnabled
                ),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                isError = isError)

            Box(modifier = Modifier.fillMaxWidth()) {
                TextFieldDecorators(
                    maxAllowedChars = maxAllowedChars,
                    charCount = charCount,
                    showCount = true
                )

                if((showPredictions.value) and (predictions.isNotEmpty())
                    and (value.isNotEmpty())){
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, bottom = 8.dp, end = 6.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(topEnd = 0.dp, topStart = 0.dp, bottomStart = 5.dp, bottomEnd = 5.dp)) {
                        LazyColumn(
                            state = rememberLazyListState(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 100.dp)
                                .padding(start = 4.dp)){
                            items(predictions){ prediction ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onValueChange(prediction)
                                            showPredictions.value = false
                                        }
                                ){
                                    itemContent(prediction)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/*Fully customized text field*/
@SuppressLint("UnrememberedMutableState")
@Composable
fun CustomTextField(
    value: String,
    label:@Composable (() -> Unit)?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLines:Int = 1,
    readOnly:Boolean = false,
    maxAllowedChars:Int? = null,
    helperText:String? = null,
    leadingIcon:@Composable (() -> Unit)? = null,
    trailingIcon:@Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    keyboardActions: KeyboardActions = KeyboardActions(),
    isError: Boolean = false,
    enabled: Boolean = true,
    showCount: Boolean = true,
    showTrailingIcon : Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    darkThemeEnabled:Boolean = false
){
    val charCount = value.length

    CompositionLocalProvider(LocalTextSelectionColors provides MaterialTheme.customTextFieldSelectionColors()){
        Column(modifier = modifier
            .background(Color.Transparent),
            horizontalAlignment = Alignment.End) {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    var newValue = it
                    maxAllowedChars?.let { limit ->
                        if(it.length > limit)
                            newValue = it.dropLast(it.length - limit)
                    }
                    onValueChange(newValue)
                },
                label = label,
                modifier = Modifier.fillMaxWidth(),
                singleLine = (maxLines == 1),
                readOnly = readOnly,
                maxLines = maxLines,
                leadingIcon = leadingIcon,
                trailingIcon = if(showTrailingIcon or value.isNotEmpty()) trailingIcon else null,
                textStyle = MaterialTheme.typography.subtitle1,
                colors = MaterialTheme.customTextFieldColors(
                    darkThemeEnabled = darkThemeEnabled
                ),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                isError = isError,
                enabled = enabled,
                visualTransformation = visualTransformation)

            TextFieldDecorators(
                maxAllowedChars =  maxAllowedChars,
                helperText = helperText,
                charCount = charCount,
                showCount = showCount)
        }
    }
}

/*Creates a series of decorators to be used alongside text fields such as a helper text or current character count*/
@Composable
private fun TextFieldDecorators(
    maxAllowedChars:Int? = null,
    helperText:String? = null,
    charCount:Int? = null,
    showCount:Boolean = false){
    Row(modifier = Modifier
        .padding(end = 12.dp, start = 12.dp)
        .fillMaxWidth(),
        horizontalArrangement = if(helperText != null) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically) {
        helperText?.let {
            Text(text = it, style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface.copy(0.7f),
                modifier = Modifier.weight(1f))
        }

        /*Creates a text composable to be used as a char counter as a complement for text fields*/
        maxAllowedChars?.let { limit ->
            if(showCount){
                Text(text = "${charCount}/$limit", style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.onSurface.copy(0.7f))
            }
        }
    }
}

/*Shows the image the user has selected or taken, and otherwise shows a default image*/
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CurrentSelectedImage(image: Bitmap?, onClick: () -> Unit, modifier: Modifier = Modifier){
    val imgModifier = Modifier
        .size(width = 120.dp, height = 120.dp)
        .padding(3.dp)

    Surface(modifier = modifier.padding(24.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(3.dp,MaterialTheme.colors.onSurface),
        onClick = onClick) {
        if(image != null){
            Image(bitmap = image.asImageBitmap(), contentDescription = "",
                modifier = imgModifier, contentScale = ContentScale.Crop)
        }else{
            Image(imageVector = Icons.Default.Image, contentDescription = "",
                modifier = imgModifier, contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface))
        }
    }
}

/*Adds a fab with the option to be disabled*/
@Composable
fun AddFloatingActionButton(
    enabled:Boolean,
    onClick:() -> Unit){
    var modifier = Modifier.size(56.dp)
    modifier = if(enabled) modifier.shadow(6.dp, CircleShape) else modifier

    Button(onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            disabledBackgroundColor = MaterialTheme.colors.secondary.copy(0.6f)),
        shape = CircleShape,
        modifier = modifier) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null,
            tint = MaterialTheme.colors.onSecondary)
    }
}

/*Creates a button for google sign in*/
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GoogleSignInButton(onClick:() -> Unit, modifier: Modifier = Modifier){
    Surface(onClick = onClick,
        shape = MaterialTheme.shapes.medium.copy(CornerSize(4.dp)),
        border = BorderStroke(width = 2.dp, color = MaterialTheme.colors.onSurface),
        color = MaterialTheme.colors.surface,
        modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
            Icon(painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "",
                tint = Color.Unspecified)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.google_sign_in_button_text))
        }
    }
}

/*Creates a dialog that displays the image associated to a product and offers the user the possibility to delete it*/
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SelectedImageCustomDialog(
    show: Boolean,
    image:Bitmap?,
    onDismiss: () -> Unit,
    onDelete:() -> Unit,
    orientation:Int,
    modifier: Modifier = Modifier){
    if(!show or (image == null))
        return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = modifier.background(Color.Red)) {
            Surface(color = Color.Red,
                modifier = Modifier.align(Alignment.Center)) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Red)) {
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = "")
                }

                val imgModifier = if(orientation == Configuration.ORIENTATION_PORTRAIT) Modifier.fillMaxWidth()
                    else Modifier.fillMaxHeight()
                Surface(
                    modifier = Modifier.padding(2.dp),
                    shape = MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), topEnd = CornerSize(70.dp))) {
                    Image(bitmap = image!!.asImageBitmap(),
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        modifier = imgModifier)
                }
            }
        }
    }
}

/*Creates a bottom sheet that gives the user the option to select an image from the gallery or take a picture in case it has
* not already done it, in this case a dialog is shown with the current image where the user can delete it if wanted*/
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ImagePickerBottomSheetContent(
    onDismiss:() -> Unit,
    title: String,
    galleryPicker:() -> Unit,
    pictureTaker:() -> Unit,
    isDarkThemeEnabled:Boolean = false
){
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(text = title,
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.primaryVariant,
            modifier = Modifier.padding(bottom = 8.dp))
        Surface(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.Start),
            onClick = {
                onDismiss()
                galleryPicker()
            }) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Image,
                    contentDescription = "",
                    modifier = Modifier.padding(end = 16.dp))
                Text(text = stringResource(id = R.string.add_img_dialog_pick_from_gallery),
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onSurface)
            }
        }
        Surface(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.Start),
            onClick = {
                onDismiss()
                pictureTaker()
            }) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.CameraAlt,
                    contentDescription = "",
                    modifier = Modifier.padding(end = 16.dp))
                Text(text = stringResource(id = R.string.add_img_dialog_take_picture),
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onSurface)
            }
        }
        Surface(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.Start),
            onClick = {
                onDismiss()
            }) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.HighlightOff,
                    contentDescription = "",
                    modifier = Modifier.padding(end = 16.dp),
                    tint = if(isDarkThemeEnabled) MaterialTheme.colors.onSurface
                            else MaterialTheme.colors.error)
                Text(text = stringResource(id = R.string.cancel_button_string),
                    style = MaterialTheme.typography.subtitle1,
                    color = if(isDarkThemeEnabled) MaterialTheme.colors.onSurface
                        else MaterialTheme.colors.error)
            }
        }
    }
}

/*Receives a drawable, creates a bitmap from it and if successful creates an Image with it*/
@Composable
fun AdaptiveIconImage(
    adaptiveDrawable:Int,
    @DrawableRes drawable:Int,
    modifier: Modifier = Modifier){
    val bitmap = ImageUtils.createBitmapFromDrawable(drawableRes = adaptiveDrawable)

    if(bitmap != null)
        Image(bitmap = bitmap.asImageBitmap(),
            contentDescription = "",
            modifier = modifier)
    else
        Image(painter = painterResource(id = drawable),
            contentDescription = "",
            modifier = modifier)
}

/*Creates a text field that offers the opportunity to scan a barcode to set it as its value*/
@Composable
fun BarcodeScanSection(
    barcodeState:String,
    onValueChange:(String) -> Unit,
    onCancelClicked:() -> Unit,
    onScanCodeClicked:() -> Unit,
    maxAllowedChars:Int? = null,
    helperText: String? = null,
    darkThemeEnabled: Boolean = false){
    CompositionLocalProvider(LocalTextSelectionColors provides MaterialTheme.customTextFieldSelectionColors()){
        CustomTextField(value = barcodeState,
            modifier = Modifier
                .fillMaxWidth(),
            label = {
                Text(text = stringResource(id = R.string.product_barcode_label),
                    style = MaterialTheme.typography.subtitle1)
            },
            maxAllowedChars = maxAllowedChars,
            onValueChange = {
                onValueChange(it)
            },
            trailingIcon = {
                if(barcodeState.isNotEmpty()){
                    IconButton(onClick = onCancelClicked) {
                        Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "")
                    }
                } else {
                    IconButton(onClick = onScanCodeClicked) {
                        Icon(imageVector = ImageUtils.createImageVector(drawableRes = R.drawable.ic_barcode),
                            contentDescription = "")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            showCount = false,
            showTrailingIcon = true,
            darkThemeEnabled = darkThemeEnabled)

        TextFieldDecorators(helperText = helperText)
    }
}

/*Creates a group a radio buttons where only one can be selected at any given time*/
@Composable
fun SingleSelectableRadioButtons(
    radioOptions:List<RadioButtonItem> = listOf()){
    Column(horizontalAlignment = Alignment.Start,
        modifier = Modifier.selectableGroup()) {
        radioOptions.forEach { 
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .selectable(
                    selected = it.selected,
                    onClick = it.onClick,
                    role = Role.RadioButton
                ), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = it.selected, onClick = it.onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary))
                Text(text = it.text,
                    color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal, fontSize = 18.sp))
            }
        }
    }
}

/*Creates an expandable floating action button that shows a series of smaller buttons when pressed*/
@Composable
fun MultiFloatingButton(
    items: List<FabItem>,
    multiFabState:MultiFloatingButtonState,
    onMultiFabStateChange:(MultiFloatingButtonState)->Unit){
    /*Transition used to rotate the fab when clicked*/
    val transition = updateTransition(targetState = multiFabState, label = "fab rotation transition")
    val rotateAnim by transition.animateFloat(
        label = "rotate",
        transitionSpec = { tween(durationMillis = 500) }) {
        if(it == MultiFloatingButtonState.Expanded) 315f else 0f
    }

    Column(
        horizontalAlignment = Alignment.End
    ) {
        items.forEach {
            if(!it.enabled)
                return@forEach
            AnimatedVisibility(
                visible = (transition.currentState == MultiFloatingButtonState.Expanded),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
                ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            backgroundColor = MaterialTheme.colors.secondary,
                            modifier = Modifier.alpha(alpha = 0.9f)
                        ) {
                            Text(
                                text = it.label,
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.onSecondary,
                                modifier = Modifier
                                    .background(Color.Transparent)
                                    .padding(vertical = 6.dp, horizontal = 10.dp))
                        }

                        Spacer(modifier = Modifier.size(10.dp))

                        FloatingActionButton(
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                            backgroundColor = MaterialTheme.colors.primary,
                            onClick = it.onClick) {
                            Icon(imageVector = it.icon, contentDescription = null,
                                tint = MaterialTheme.colors.onPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.size(10.dp))
                }
            }
        }

        FloatingActionButton(
            onClick = {
                onMultiFabStateChange(
                    if(transition.currentState == MultiFloatingButtonState.Expanded)
                        MultiFloatingButtonState.Collapsed
                    else
                        MultiFloatingButtonState.Expanded
                )
            },
            modifier = Modifier.size(56.dp),
            backgroundColor = MaterialTheme.colors.secondary,
            shape = CircleShape) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null,
                tint = MaterialTheme.colors.onSecondary,
                modifier = Modifier.rotate(rotateAnim))
        }
    }
}

@Composable
fun NavDrawerHeader(
    backgroundColors : List<Color> = listOf(
        MaterialTheme.colors.primaryVariant,
        MaterialTheme.colors.secondary
    )
){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.horizontalGradient(
                colors = backgroundColors)),
        contentAlignment = Alignment.Center) {
        Row(modifier = Modifier.padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                modifier = Modifier
                    .height(80.dp)
                    .width(80.dp),
                border = BorderStroke(2.dp, Color.Black.copy(alpha = 0.8f)),
                elevation = 8.dp) {
                AdaptiveIconImage(
                    adaptiveDrawable = R.mipmap.launcher_icon_round,
                    drawable = R.drawable.ic_account_circle,
                    modifier = Modifier
                        .clip(CircleShape)
                        .sizeIn(minHeight = 64.dp, minWidth = 64.dp)
                        .background(color = Color.Transparent))
            }
            Text(text = stringResource(id = R.string.app_name),
                modifier = Modifier.padding(start = 20.dp),
                style = MaterialTheme.typography.h5.copy(
                    shadow = Shadow(
                        color = MaterialTheme.colors.background,
                        offset = Offset(5f,5f),
                        blurRadius = 6f
                    )
                ),
                color = MaterialTheme.colors.onPrimary)
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun NavDrawerHeaderPreview(){
    PriceRecorderTheme {
        NavDrawerHeader(backgroundColors =
        listOf(
            MaterialTheme.colors.primary,
            MaterialTheme.colors.secondary
        ))
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
fun NavDrawerHeaderPreview2(){
    PriceRecorderTheme {
        NavDrawerHeader(backgroundColors =
        listOf(
            MaterialTheme.colors.primaryVariant,
            MaterialTheme.colors.secondary
        )
        )
    }
}

@Composable
fun NavDrawerBody(
    options:List<AppBarAction>,
    settingsOption:AppBarAction,
    modifier: Modifier = Modifier){
    val lazyListState = rememberLazyListState()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)){
            items(options){ item ->  
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = item.action)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically){
                    Icon(imageVector = item.icon, contentDescription = item.name)
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(text = item.name, color = MaterialTheme.colors.onSurface,
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp),
                        modifier = Modifier
                            .alpha(1f))
                }
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Divider(thickness = 1.dp,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .alpha(0.8f))
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = settingsOption.action)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically){
            Icon(imageVector = settingsOption.icon, contentDescription = settingsOption.name)
            Spacer(modifier = Modifier.size(12.dp))
            Text(text = settingsOption.name, color = MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp),
                modifier = Modifier.alpha(1f))
        }
    }
}