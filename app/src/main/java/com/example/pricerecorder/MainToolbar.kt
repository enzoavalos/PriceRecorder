package com.example.pricerecorder

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.pricerecorder.theme.PriceRecorderShapes
import com.example.pricerecorder.theme.PriceRecorderTheme

/*Class used to generalize app bar action items*/
data class AppBarAction(
    val name:String,
    val icon:ImageVector?,
    val action: () -> Unit,
    var enabled:Boolean = true
)

/*Creates an app bar and receives a title and a composable content to be able to be set dynamically*/
@Composable
fun showTopAppBar(appBarTitle: String, actionItems:List<AppBarAction>,
                  navigationIcon: (@Composable () -> Unit)?, modifier:Modifier = Modifier){
    TopAppBar(
        title = { Text(appBarTitle) },
        navigationIcon = navigationIcon,
        backgroundColor = MaterialTheme.colors.primary,
        actions = {
            /*Divides the items that will be actions presented in the appbar from those which will be
            * options of the overflow menu*/
            val (actions,options) = actionItems.partition { it.icon != null }
                  /*creates action items from the given list*/
                  actions.forEach {
                      IconButton(
                          onClick = it.action,
                          content = { Icon(imageVector = it.icon!!,contentDescription = it.name) },
                          modifier = Modifier.alpha(1f)
                      )
                  }

            if(options.isNotEmpty()) {
                /*State declared to keep track of the menu visibility*/
                val isExpanded = remember { mutableStateOf(false) }
                overflowMenu(isExpanded = isExpanded.value,
                    setExpanded = {
                        isExpanded.value = it
                    }, options = options)
            }
        },
        modifier = modifier
    )
}

@Composable
fun searchAppBar(text:String,onTextChange:(String) -> Unit,
    onCloseClicked:() -> Unit,onSearchClicked:(String) -> Unit){
    Surface(modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
        elevation = AppBarDefaults.TopAppBarElevation,
        color = MaterialTheme.colors.primary) {
        TextField(value = text, onValueChange = { onTextChange(it) }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(modifier = Modifier.alpha(ContentAlpha.medium),
                text = stringResource(id = R.string.search_view_hint),
                color = MaterialTheme.colors.onPrimary) },
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
                    if(text.isNotEmpty())
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
                onSearch = { onSearchClicked(text) }
            ),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.onPrimary.copy(ContentAlpha.medium)
            ))
    }
}

/*Creates an overflow menu with the options given*/
@Composable
private fun overflowMenu(isExpanded:Boolean,setExpanded:(Boolean) -> Unit,options:List<AppBarAction>){
    IconButton(onClick = { setExpanded(true) }) {
        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = stringResource(id = R.string.overflow_menu_description))
    }
    MaterialTheme(shapes = PriceRecorderShapes.copy(medium = RoundedCornerShape(0.dp))) {
        DropdownMenu(expanded = isExpanded, onDismissRequest = { setExpanded(false) },
            offset = DpOffset(x = 0.dp, y = (-8).dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    option.action()
                    setExpanded(false)
                }) {
                    Text(text = option.name)
                }
            }
        }
    }
}

@Composable
fun homeAppBar(onSearchClick: () -> Unit, onFilterClick: () -> Unit,onDeleteAllClicked:() -> Unit,
               onSettingsClicked:() -> Unit){
    showTopAppBar(stringResource(R.string.app_name),
        navigationIcon = null
        , actionItems = listOf(
            AppBarAction(stringResource(id = R.string.search_view_hint),
                Icons.Filled.Search, onSearchClick),
            AppBarAction(stringResource(id = R.string.filter_dialog_title),
                Icons.Filled.FilterList,onFilterClick),
            AppBarAction(stringResource(id = R.string.delete_all_menu_option),null,onDeleteAllClicked),
            AppBarAction(stringResource(id = R.string.setting_fragment_title),null,onSettingsClicked)
        ))
}

@Preview
@Composable
private fun homeAppBarPreview(){
    PriceRecorderTheme {
        showTopAppBar(stringResource(R.string.app_name),
            navigationIcon = null,
            actionItems = listOf(
                AppBarAction(stringResource(id = R.string.search_view_hint),Icons.Filled.Search,{}),
                AppBarAction(stringResource(id = R.string.filter_dialog_title),Icons.Filled.FilterList,{}),
                AppBarAction(stringResource(id = R.string.delete_all_menu_option),null,{}),
                AppBarAction(stringResource(id = R.string.setting_fragment_title),null,{}),
            ))
    }
}

@Preview
@Composable
private fun searchAppBarPreview(){
    PriceRecorderTheme {
        searchAppBar(text = "Lorem ipsun", onTextChange = {}, onCloseClicked = {}, onSearchClicked = {})
    }
}