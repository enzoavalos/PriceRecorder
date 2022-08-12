package com.example.pricerecorder

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.res.ResourcesCompat
import com.example.pricerecorder.theme.PriceRecorderShapes
import com.example.pricerecorder.theme.PriceRecorderTheme

data class ChipItem(
    val text:String,
    var onClick: () -> Unit = {},
    val leadingIcon:@Composable (() -> Unit)?
)

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
fun CustomAlertDialog(show:Boolean, title:String, msg:@Composable (()->Unit)?, confirmButtonText:String,
                      dismissButtonText:String?, onConfirm:() -> Unit, onDismiss:()->Unit,
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
            Button(onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent,
                    contentColor = MaterialTheme.colors.secondary),
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)) {
                Text(text = confirmButtonText, style = MaterialTheme.typography.h6)
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExposedDropdownMenu(
    value: String,
    label:@Composable (() -> Unit)?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon:@Composable (() -> Unit)? = null,
    helperText:String? = null,
    options:List<String>
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
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        cursorColor = MaterialTheme.colors.primaryVariant.copy(ContentAlpha.medium),
                        textColor = MaterialTheme.colors.onSurface
                    ))


                TextFieldDecorators(helperText = helperText)
            }

            ExposedDropdownMenu(expanded = (expanded and options.isNotEmpty()),
                onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = {
                        expanded = false
                        onValueChange(option)
                    }) {
                        Text(text = option)
                    }
                }
            }
        }
    }
}

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
){
    val charCount = value.length
    val showTrailingIcon by derivedStateOf { value.isNotEmpty() }
    val showPredictions = remember {
        mutableStateOf(false)
    }

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
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.primaryVariant.copy(ContentAlpha.medium),
                textColor = MaterialTheme.colors.onSurface
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
    showCount: Boolean = true
){
    val charCount = value.length
    val showTrailingIcon by derivedStateOf { value.isNotEmpty() }

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
            modifier = Modifier
                .fillMaxWidth(),
            singleLine = (maxLines == 1),
            readOnly = readOnly,
            maxLines = maxLines,
            leadingIcon = leadingIcon,
            trailingIcon = if(showTrailingIcon) trailingIcon else null,
            textStyle = MaterialTheme.typography.subtitle1,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.primaryVariant.copy(ContentAlpha.medium),
                textColor = MaterialTheme.colors.onSurface
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = isError,
            enabled = enabled)

       TextFieldDecorators(
            maxAllowedChars =  maxAllowedChars,
            helperText = helperText,
            charCount = charCount,
            showCount = showCount)
    }
}

@Composable
private fun TextFieldDecorators(
    maxAllowedChars:Int? = null,
    helperText:String? = null,
    charCount:Int? = null,
    showCount:Boolean = false){
    Row(modifier = Modifier
        .padding(end = 12.dp, bottom = 8.dp, start = 12.dp)
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CurrentSelectedImage(image: Bitmap?, onClick: () -> Unit, modifier: Modifier = Modifier){
    val imgModifier = Modifier
        .size(width = 120.dp, height = 120.dp)
        .padding(3.dp)

    Surface(modifier = modifier.padding(24.dp),
        shape = MaterialTheme.shapes.medium.copy(CornerSize(10.dp)),
        border = BorderStroke(3.dp,MaterialTheme.colors.onSurface),
        onClick = onClick) {
        if(image != null){
            Image(bitmap = image.asImageBitmap(), contentDescription = "",
                modifier = imgModifier, contentScale = ContentScale.Crop)
        }else{
            Image(painter = painterResource(id = R.drawable.ic_image), contentDescription = "",
                modifier = imgModifier, contentScale = ContentScale.Crop)
        }
    }
}

/*Adds a fab with the option to be disabled*/
@Composable
fun AddFloatingActionButton(enabled:Boolean, onClick:() -> Unit){
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
    buttonText:String,
      modifier: Modifier = Modifier){
    if(!show or (image == null))
        return
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Image(bitmap = image!!.asImageBitmap(),
                contentDescription = "",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center)

            Button(onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Red
                )) {
                Text(text = buttonText,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onError)
            }
        }
    }
}

/*Creates a dialog that gives the user the option to select an image from the gallery or take a picture in case it has
* not already done it, in this case a dialog is shown with the current image where the user can delete it if wanted*/
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ImagePickerCustomDialog(
    show:Boolean,
    onDismiss:() -> Unit,
    title: String,
    galleryPicker:() -> Unit,
    pictureTaker:() -> Unit){
    CustomAlertDialog(show = show,
        title = title,
        msg = {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Surface(modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start),
                    onClick = {
                        onDismiss()
                        galleryPicker()
                    }) {
                    Text(text = stringResource(id = R.string.add_img_dialog_pick_from_gallery),
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.primaryVariant)
                }
                Surface(modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start),
                    onClick = {
                        onDismiss()
                        pictureTaker()
                    }) {
                    Text(text = stringResource(id = R.string.add_img_dialog_take_picture),
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.primaryVariant)
                }
            }
        },
        confirmButtonText = stringResource(id = R.string.cancel_button_string),
        dismissButtonText = null,
        onConfirm = onDismiss,
        onDismiss = onDismiss)
}

/*Receives a drawable, creates a bitmap from it and if successful creates an Image with it*/
@Composable
fun AdaptiveIconImage(
    adaptiveDrawable:Int,
    @DrawableRes drawable:Int,
    modifier: Modifier = Modifier){

    ResourcesCompat.getDrawable(
        LocalContext.current.resources,
        adaptiveDrawable,
        LocalContext.current.theme
    )?.let {
        val bitmap = ImageUtils.createBitmapFromDrawable(it)
        val canvas = android.graphics.Canvas(bitmap)
        it.setBounds(0,0,canvas.width,canvas.height)
        it.draw(canvas)

        Image(bitmap = bitmap.asImageBitmap(),
            contentDescription = "",
            modifier = modifier)
        return
    }

    Image(painter = painterResource(id = drawable),
        contentDescription = "",
        modifier = modifier)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CustomDoubleSelectionSlider(
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    values : ClosedFloatingPointRange<Float> = range.start..range.endInclusive,
    steps: Int = 0,
    enabled: Boolean = true
){
    RangeSlider(
        values = values,
        valueRange = range,
        steps = steps,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = modifier,
        colors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colors.secondary.copy(0.8f),
            inactiveTrackColor = MaterialTheme.colors.background,
            activeTickColor = MaterialTheme.colors.secondary.copy(0.8f),
            inactiveTickColor = MaterialTheme.colors.background,
            thumbColor = MaterialTheme.colors.secondary
        ))
}

@Preview(widthDp = 360, showBackground = true)
@Composable
fun CustomDoubleSelectionSliderPreview(){
    PriceRecorderTheme {
        CustomDoubleSelectionSlider(
            onValueChange = {},
            steps = 10,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )
    }
}