package com.example.pricerecorder

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

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

@Composable
fun RegularTextField(
    value: String,
    label:@Composable (() -> Unit)?,
    maxLines:Int,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxAllowedChars:Int? = null,
    leadingIcon:@Composable (() -> Unit)? = null,
    trailingIcon:@Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    keyboardActions: KeyboardActions = KeyboardActions(),
    isError: Boolean = false
){
    val charCount = value.length

    Column(modifier = modifier
        .background(Color.Transparent),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.End) {
        OutlinedTextField(value = value, onValueChange = {
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
            maxLines = maxLines,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            textStyle = MaterialTheme.typography.subtitle1,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.primaryVariant.copy(ContentAlpha.medium),
                textColor = MaterialTheme.colors.onSurface
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = isError)

        maxAllowedChars?.let {
            Text(text = "${charCount}/$it", style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface.copy(0.7f),
                modifier = Modifier.padding(end = 12.dp, bottom = 8.dp))
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