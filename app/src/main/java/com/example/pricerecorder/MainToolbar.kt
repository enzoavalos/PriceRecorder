package com.example.pricerecorder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.pricerecorder.theme.customTextFieldSelectionColors

/*Class used to generalize app bar action items*/
data class AppBarAction(
    val name:String,
    val icon:ImageVector,
    val action: () -> Unit,
    var enabled: Boolean = true
)

enum class SearchWidgetState {
    OPENED,CLOSED
}

enum class SearchState{
    SEARCHING,STARTING
}

/*Creates an app bar and receives a title and a composable content to be able to be set dynamically*/
@Composable
fun ShowTopAppBar(appBarTitle: String, actionItems:List<AppBarAction>,
                  navigationIcon: (@Composable () -> Unit)?, modifier:Modifier = Modifier){
    TopAppBar(
        title = { Text(appBarTitle) },
        navigationIcon = navigationIcon,
        backgroundColor = MaterialTheme.colors.primary,
        actions = {
              /*creates action items from the given list*/
              actionItems.forEach {
                  IconButton(
                      onClick = it.action,
                      enabled = it.enabled,
                      content = { Icon(imageVector = it.icon,contentDescription = it.name,
                          tint = if(it.enabled) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onPrimary.copy(alpha = 0.7f),
                          modifier = Modifier.alpha(1f)) }
                  )
              }
        },
        modifier = modifier.height(56.dp),
        elevation = AppBarDefaults.TopAppBarElevation
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchAppBar(text:String, onTextChange:(String) -> Unit,
                 onCloseClicked:() -> Unit){
    /*Used to manage clearing focus from the text field when onSearchClicked is called*/
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    BackHandler(enabled = true, onBack = onCloseClicked)

    CompositionLocalProvider(LocalTextSelectionColors provides MaterialTheme.customTextFieldSelectionColors()) {
        Surface(modifier = Modifier
            .fillMaxWidth(),
            elevation = AppBarDefaults.TopAppBarElevation,
            color = MaterialTheme.colors.primary) {

            OutlinedTextField(
                value = text,
                textStyle = MaterialTheme.typography.subtitle1,
                onValueChange = { onTextChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(modifier = Modifier.alpha(ContentAlpha.medium),
                        text = stringResource(id = R.string.search_view_hint),
                        color = MaterialTheme.colors.onPrimary)
                },
                singleLine = true,
                leadingIcon = {
                    IconButton(onClick = {}, modifier = Modifier.alpha(ContentAlpha.medium)) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "",
                            tint = MaterialTheme.colors.onPrimary)
                    }
                },
                trailingIcon = {
                    IconButton(onClick = {
                        /*When the close button is clicked, if the text field contains text then it is cleared,
                        * otherwise the searchBar is closed*/
                        if (text.isNotEmpty())
                            onTextChange("")
                        else
                            onCloseClicked()
                    }) {
                        Icon(imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.close_icon_desc),
                            tint = MaterialTheme.colors.onPrimary)
                    }
                },
                /*Specifies that the search action should be displayed in the keyboard*/
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                ),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    cursorColor = MaterialTheme.colors.secondary,
                    textColor = MaterialTheme.colors.onPrimary,
                    focusedIndicatorColor = Color.Transparent
                ))

            /*Suspends recomposition and request focus for the component associated to the focusRequester*/
            LaunchedEffect(key1 = null) {
                focusRequester.requestFocus()
            }
        }
    }
}