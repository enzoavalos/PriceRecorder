package com.example.pricerecorder.homeFragment

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.pricerecorder.*
import com.example.pricerecorder.R
import com.example.pricerecorder.addFragment.AddFragment
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.FilterState
import com.example.pricerecorder.theme.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class HomeFragment:Fragment() {
    private val viewModel: HomeViewModel by viewModels { HomeViewModel.factory }
    private lateinit var permissionChecker : PermissionChecker
    private lateinit var barcodeScanner: BarcodeScanner

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        createNotificationChannel(requireContext())

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                HomeScreen()
            }
        }
    }

    private fun navigateToSettingsFragment(){
        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToSettingsFragment())
    }

    private fun navigateToAddFragment(){
        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToAddFragment())
    }

    private fun navigateToEditFragment(productId : Long){
        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToEditFragment(productId))
    }

    @Composable
    fun HomeScreen() {
        /*Observed states from the viewModel*/
        val searchWidgetState by viewModel.searchWidgetState
        val searchTextState by viewModel.searchTextState
        val scaffoldState = rememberScaffoldState()
        val coroutineScope = rememberCoroutineScope()
        val filterState = viewModel.isFiltering
        val list by viewModel.products.observeAsState(listOf())
        /*Used to remember the state of the products list through recompositions*/
        val lazyListState = rememberLazyListState()
        /*Hoisted state used to disable the home app bar actions such as search and filter when there are
        * no elements stored*/
        val appBarActionsEnabled by remember {
            derivedStateOf { list.isNotEmpty() }
        }

        PriceRecorderTheme(
            context = requireContext()
        ) {
            Scaffold(
                scaffoldState = scaffoldState,
                backgroundColor = MaterialTheme.colors.background,
                snackbarHost = {
                    /*Host for snack bars to be used in scaffold, and specially customize its appearance*/
                    SnackbarHost(hostState = it){ data ->
                        Snackbar(
                            actionColor = if(ThemeUtils.systemInDarkTheme(requireContext()))
                                MaterialTheme.colors.surface
                            else SnackbarDefaults.primaryActionColor,
                            snackbarData = data
                        )
                    }
                },
                topBar = { MainAppBar(searchWidgetState,searchTextState,appBarActionsEnabled) },
                floatingActionButton = { AddFloatingActionButton(enabled = true,
                    onClick = { navigateToAddFragment() }) },
                floatingActionButtonPosition = FabPosition.Center) {
                Column(modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(visible = filterState.value) {
                        FiltersAppliedScreen(
                            show = filterState.value,
                            onCancelClick = {
                                viewModel.updateFilterState(FilterState.IDLE)
                                viewModel.resetFilters()
                                viewModel.resetSearchState()
                            },
                            modifier = Modifier.fillMaxWidth())
                    }

                    ProductsList(showSnackBar = { msg,
                                                  actionLabel,
                                                  onActionPerformed ->
                        coroutineScope.launch {
                            /*Snack bar is shown on screen and when the action is clicked, onActionPerformed is invoked*/
                            when (scaffoldState.snackbarHostState.showSnackbar(message = msg,
                                actionLabel = actionLabel)) {
                                SnackbarResult.ActionPerformed -> onActionPerformed()
                                else -> {}
                            }
                        }
                    },
                        list = list,
                        lazyListState = lazyListState,
                        modifier = Modifier.padding(it))
                }
            }
        }
    }

    /*Determines the app bar to be displayed, either the default appbar or a search app bar*/
    @Composable
    private fun MainAppBar(
        searchWidgetState: SearchWidgetState,
        searchTextState: String,
        appBarActionsEnabled: Boolean
        ){
        var showDeleteAllDialog by remember {
            mutableStateOf(false)
        }
        var showFilterDialog by remember {
            mutableStateOf(false)
        }

        FilterProductsDialog(
            show = showFilterDialog,
            onConfirm = {
                showFilterDialog = false
                viewModel.filterProducts()
            },
            onDismiss = {
                showFilterDialog = false
                viewModel.resetFilters() })

        CustomAlertDialog(show = showDeleteAllDialog,
            title = stringResource(id = R.string.delete_all_dialog_title),
            msg ={
                Text(text = stringResource(id = R.string.delete_all_dialog_msg),
                    color = MaterialTheme.colors.onSurface)
            },
            confirmButtonText = stringResource(id = R.string.accept_button_string),
            dismissButtonText = stringResource(id = R.string.cancel_button_string),
            onConfirm = {
                showDeleteAllDialog = false
                viewModel.clear()
                Toast.makeText(context,resources.getString(R.string.delete_all_success_msg), Toast.LENGTH_SHORT).show()
                },
            onDismiss = {
                showDeleteAllDialog = false
            })

        when(searchWidgetState){
            SearchWidgetState.CLOSED -> {
                HomeAppBar(onSearchClick = { viewModel.updateSearchWidgetState(SearchWidgetState.OPENED) },
                    onFilterClick = { showFilterDialog = true },
                    onDeleteAllClicked = { showDeleteAllDialog = true },
                    onSettingsClicked = { navigateToSettingsFragment() },
                    onSearchWithBarcode = {
                        if(!::permissionChecker.isInitialized)
                            permissionChecker = PermissionChecker(requireContext(),requireActivity().activityResultRegistry)
                        if(!::barcodeScanner.isInitialized)
                            barcodeScanner = BarcodeScanner(requireContext(),requireActivity().activityResultRegistry)
                        barcodeScanner.scanCode(
                            permissionChecker,
                            onSuccessCallback = {
                                viewModel.updateBarcodeFilter(it)
                            }
                        )
                    },
                    appBarActionsEnabled)
            }
            else -> {
                SearchAppBar(text = searchTextState,
                    onTextChange = {
                        viewModel.updateSearchTextState(it)
                    },
                    onCloseClicked = {
                        viewModel.resetSearchState() })
            }
        }
    }

    /*List displayed in the home screen with all stored products*/
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun ProductsList(
        showSnackBar: (String, String, () -> Unit) -> Unit,
        list: List<Product>,
        lazyListState: LazyListState,
        modifier: Modifier = Modifier
    ){
        if(list.isNotEmpty()){
            Box(modifier = modifier) {
                val coroutineScope = rememberCoroutineScope()
                /*Used to remember whether the button should be visible or not*/
                val showScrollToTopButton by remember {
                    derivedStateOf {
                        lazyListState.firstVisibleItemIndex > 0
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxWidth(),
                    state = lazyListState, contentPadding = PaddingValues(bottom = 44.dp)){
                    itemsIndexed(list, key = { _,item -> item.getId() }){ index,product ->
                        SwipeActions(
                            endActionsConfig = SwipeActionsConfig(
                                threshold = 0.30f,
                                icon = Icons.Default.Delete,
                                iconTint = Color.Red,
                                background = MaterialTheme.colors.background,
                                stayDismissed = true,
                                onDismiss = {
                                    viewModel.deleteProduct(productId = product.getId())
                                    showSnackBar(getString(R.string.product_deleted_msg),getString(R.string.undo_action_msg)){
                                        viewModel.addProduct(product)
                                    }
                                }
                            ),
                            /*Tutorial for swipe content shown only on the first element*/
                            //showTutorial = (index == 0)
                        ) { dismissState ->
                            /*State that determines whether the corners of the card must be animated*/
                            val animateCorners by remember {
                                derivedStateOf {
                                    dismissState.offset.value.absoluteValue > 40
                                }
                            }
                            val endCorners by animateDpAsState(
                                targetValue = when {
                                    dismissState.dismissDirection == DismissDirection.EndToStart && animateCorners -> 8.dp
                                    else -> 0.dp
                                }
                            )
                            val elevation by animateDpAsState(
                                targetValue = when {
                                    animateCorners -> 8.dp
                                    else -> 2.dp
                                })

                            Card(modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(
                                    topEnd = endCorners,
                                    bottomEnd = endCorners
                                ),
                                elevation = elevation) {
                                ListItemProduct(product = product, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                /*launches a coroutine to scroll to the first item in the list with animation*/
                AnimatedVisibility(visible = showScrollToTopButton,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd)) {
                    ScrollToTopButton {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    }
                }
            }
        }else{
            when(viewModel.searching.value){
                false -> HomeEmptyBackgroundScreen(
                    drawableRes = R.mipmap.ic_no_elements,
                    R.string.no_elements_string)
                true -> HomeEmptyBackgroundScreen(
                    drawableRes = R.mipmap.ic_no_results_found,
                    R.string.no_results_found_desc)
            }

        }
    }

    /*Button to scroll to top of list, enabled when the first item of the list is no longer shown in screen*/
    @Composable
    private fun ScrollToTopButton(onClick: () -> Unit){
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
            contentAlignment = Alignment.BottomEnd){
            IconButton(onClick = onClick,
                modifier = Modifier
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary)) {
                Icon(imageVector = Icons.Default.KeyboardDoubleArrowUp, contentDescription = "",
                    tint = MaterialTheme.colors.onSurface)
            }
        }
    }

    /*Layout shown when there are no elements stored yet*/
    @Composable
    private fun HomeEmptyBackgroundScreen(
        drawableRes: Int,
        @StringRes stringRes:Int, modifier: Modifier = Modifier){
        Column(modifier = modifier
            .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            AdaptiveIconImage(
                adaptiveDrawable = drawableRes,
                drawable = R.drawable.ic_sentiment_dissatisfied,
                Modifier
                    .widthIn(120.dp)
                    .heightIn(120.dp))

            Text(text = stringResource(id = stringRes),
                modifier
                    .padding(top = 8.dp, start = 32.dp, end = 32.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.h5,
                color = if(!ThemeUtils.systemInDarkTheme(requireContext())) PetrolBlue else SilverGrey)
        }
    }

    @OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
    @Composable
    private fun ListItemProduct(product: Product, modifier:Modifier = Modifier){
        /*Keeps track of a state used to determine when the dialog should be shown*/
        var showDetailDialog by remember {
            mutableStateOf(false)
        }
        if(showDetailDialog) {
            val onDismiss = {
                showDetailDialog = false
                viewModel.updatePriceEditTextState("")
            }
            Dialog(properties = DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = onDismiss,
                content = {
                    ProductDetail(product = product, onDismiss = onDismiss)
                })
        }

        Surface(onClick = {showDetailDialog = true}, modifier = modifier,
            color = MaterialTheme.colors.surface) {
            Column {
                Row(modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)) {
                        Text(text = product.getDescription(),
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.onSurface,
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                            maxLines = 2)
                        Text(text = product.getPlaceOfPurchase(),
                            style = MaterialTheme.typography.subtitle2,
                            color = if(!ThemeUtils.systemInDarkTheme(requireContext())) PetrolBlue else SilverGrey,
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                            maxLines = 1)
                    }
                    Text(text = "$${product.getPrice()}",
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.secondary,
                        modifier = Modifier.padding(end = 8.dp))
                }

                Divider(thickness = 2.dp,
                    color = MaterialTheme.colors.secondary.copy(0.7f),
                    modifier = Modifier
                        .paddingFromBaseline(top = 8.dp)
                        .padding(start = 30.dp, end = 30.dp))
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun DetailPriceSection(product: Product, editPrice:Boolean,
                                   onEditCompleted:() -> Unit){
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val textState by viewModel.priceEditTextState
        val errorState by viewModel.priceEditError

        val onDoneClicked = {
            keyboardController?.hide()
            focusManager.clearFocus()
            if(textState.isNotEmpty()){
                product.updatePrice(textState.toDouble())
                viewModel.updateProduct(product)
                Toast.makeText(context,resources.getString(R.string.price_updated_msg),Toast.LENGTH_SHORT).show()
            }
            onEditCompleted()
        }

        Row(modifier = Modifier
            .padding(top = 4.dp, start = 16.dp, end = 16.dp)
            .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start) {
            Text(text = stringResource(id = R.string.price_label), modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.subtitle1,color = MaterialTheme.colors.primaryVariant)
            if(!editPrice){
                Text(text = "$${product.getPrice()}",
                    style = MaterialTheme.typography.h6,color = MaterialTheme.colors.secondary)
            }else{
                OutlinedTextField(value = textState,
                    isError = errorState,
                    placeholder = {
                        Text(text = "$${product.getPrice()}",
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.secondary.copy(alpha = 0.8f))
                    },
                    onValueChange = { viewModel.updatePriceEditTextState(it) },
                    modifier = Modifier
                        .weight(1.5f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { onDoneClicked() }, enabled = !errorState) {
                            Icon(imageVector = Icons.Default.Done, contentDescription = "",
                            tint = if(!errorState) MaterialTheme.colors.secondary
                                else MaterialTheme.colors.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Decimal),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if(!errorState)
                                onDoneClicked()
                        }
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        cursorColor = MaterialTheme.colors.primaryVariant.copy(ContentAlpha.medium),
                        textColor = MaterialTheme.colors.secondary
                    ),
                    textStyle = MaterialTheme.typography.h6,
                    visualTransformation = PrefixVisualTransformation("$ "))
            }
        }
    }

    /*Section that displays a property of the current value and its value*/
    @Composable
    private fun DetailPropertySection(
        description:String,
        value:String,
        modifier: Modifier = Modifier
    ){
        if(value.isNotEmpty()){
            Row(modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start) {
                Text(text = description, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.subtitle1,color = MaterialTheme.colors.primaryVariant)
                Text(text = value,
                    style = MaterialTheme.typography.subtitle1,color = MaterialTheme.colors.onSurface)
            }
        }
    }

    /*Defines the content for a dialog that displays the details of a selected product*/
    @Composable
    private fun ProductDetail(product:Product, onDismiss: () -> Unit, modifier: Modifier = Modifier){
        var editPrice by remember {
            mutableStateOf(false)
        }

        Box(modifier = modifier
            .background(Color.Transparent)
            .padding(24.dp)
            .fillMaxWidth()) {
            Surface(modifier = Modifier
                .padding(top = 45.dp)
                .fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(3.dp,MaterialTheme.colors.onSurface),
                color = MaterialTheme.colors.surface){
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(45.0.dp))
                    Text(text = product.getDescription(), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp),
                        style = MaterialTheme.typography.h6,color = MaterialTheme.colors.onSurface)
                    Text(text = product.getPlaceOfPurchase(), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp),
                        style = MaterialTheme.typography.subtitle1,color = MaterialTheme.colors.primaryVariant)
                    if(!product.getCategory().isNullOrEmpty()){
                        Text(text = product.getCategory()!!, modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                            style = MaterialTheme.typography.subtitle1,color = MaterialTheme.colors.primaryVariant)
                    }
                    
                    Divider(thickness = 3.dp,
                        color = MaterialTheme.colors.secondary,
                        modifier = Modifier
                            .paddingFromBaseline(top = 6.dp)
                            .padding(start = 8.dp, end = 8.dp)
                            .fillMaxWidth())

                    DetailPriceSection(product = product, editPrice = editPrice,
                        onEditCompleted = {
                            editPrice = false
                            viewModel.updatePriceEditTextState("")
                        })

                    DetailPropertySection(
                        description = stringResource(id = R.string.modified_date_desc),
                        value = DateUtils.formatDate(product.getUpdateDate()),
                        modifier = Modifier
                            .padding(top = 4.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth())

                    DetailPropertySection(
                        description = stringResource(id = R.string.product_size_label),
                        value = product.getSize(),
                        modifier = Modifier
                            .padding(top = 4.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth())

                    DetailPropertySection(
                        description = stringResource(id = R.string.product_quantity_label),
                        value = product.getQuantity(),
                        modifier = Modifier
                            .padding(top = 4.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth())

                    DetailDialogBottomActionBar(product,onDismiss,
                        onEditPriceClicked = {
                            editPrice = !editPrice
                        })
                }
            }
            Surface(modifier = Modifier
                .height(90.dp)
                .width(90.dp)
                .align(Alignment.TopCenter), shape = MaterialTheme.shapes.medium,
                border = BorderStroke(3.dp,MaterialTheme.colors.onSurface)) {
                val imgModifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .align(Alignment.Center)
                DetailImageSmall(
                    product = product,imgModifier)
            }
        }
    }
    
    @Composable
    private fun DetailImageSmall(product: Product,modifier: Modifier = Modifier){
        product.getImage()?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = "",
                modifier = modifier,
                contentScale = ContentScale.Crop)
            return
        }
        Image(imageVector = Icons.Default.Image, contentDescription = "",
            modifier = modifier,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface))
    }

    @Composable
    private fun DetailDialogBottomActionBar(product: Product, onDismiss: () -> Unit,
                                            onEditPriceClicked:() -> Unit){
        Row(modifier = Modifier
            .padding(top = 8.dp, bottom = 3.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.primary),
            horizontalArrangement = Arrangement.End) {

            val actions = listOf(
                AppBarAction(stringResource(id = R.string.share_product_button_desc), icon = Icons.Outlined.Share,
                    action = {
                        /*An intent is created to send data of type plain text*/
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT,product.formatToPlainText(requireContext()))
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(intent,null)
                        startActivity(shareIntent)
                    }),
                AppBarAction(stringResource(id = R.string.edit_fragment_title), icon = Icons.Outlined.Edit, action = {
                    onDismiss()
                    navigateToEditFragment(product.getId())
                }),
                AppBarAction(stringResource(id = R.string.update_price_button_desc),
                    icon = ImageUtils.createImageVector(drawableRes = R.drawable.ic_add_price), action = {
                        onEditPriceClicked()
                    })
            )

            actions.forEach {
                IconButton(onClick = it.action,
                    modifier = Modifier
                        .padding(end = 8.dp, bottom = 4.dp, top = 4.dp)
                        .background(MaterialTheme.colors.primary)) {
                    Icon(imageVector = it.icon!!, contentDescription = it.name,
                        tint = MaterialTheme.colors.onSurface, modifier = Modifier
                            .width(44.dp)
                            .height(44.dp))
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun FilterProductsDialog(
        show:Boolean,
        onConfirm:() -> Unit,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier){
        if(!show)
            return

        val placeState = viewModel.placeFilter
        val categoryState = viewModel.categoryFilter
        val filterEnabled = viewModel.filterEnabled
        val placePredictions by viewModel.placesFiltered
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
                border = BorderStroke(3.dp,MaterialTheme.colors.onSurface),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colors.surface) {
                Column(horizontalAlignment = Alignment.CenterHorizontally){
                    Text(text = stringResource(id = R.string.filter_dialog_title),
                        style = MaterialTheme.typography.h6, modifier = Modifier.padding(16.dp))

                    /*Category dropdown menu*/
                    ExposedDropdownMenu(
                        value = categoryState.value,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp)
                            .fillMaxWidth(),
                        label = {
                            Text(text = stringResource(id = R.string.category_label),
                                style = MaterialTheme.typography.subtitle1,
                                color = MaterialTheme.colors.onSurface.copy(0.6f)) },
                        onValueChange = {
                            viewModel.updateCategoryFilter(it)
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Category, contentDescription = "")
                        },
                        options = viewModel.getListOfCategories(),
                        darkThemeEnabled = ThemeUtils.systemInDarkTheme(requireContext()))

                    /*place of purchase text field*/
                    AutoCompleteTextField(
                        value = placeState.value,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                            .fillMaxWidth(),
                        label = {
                            Text(text = stringResource(id = R.string.place_hint),
                                style = MaterialTheme.typography.subtitle1,
                                color = MaterialTheme.colors.onSurface.copy(0.6f))
                        },
                        maxAllowedChars = AddFragment.PLACE_MAX_LENGTH,
                        onValueChange = {
                            viewModel.updatePlaceFilter(it)
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Place, contentDescription = "")
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.updatePlaceFilter("")
                            }) {
                                Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "")
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        predictions = placePredictions,
                        itemContent = {
                            Text(text = it,
                                style = MaterialTheme.typography.subtitle1.copy(fontSize = 18.sp),
                                color = MaterialTheme.colors.onSurface.copy(0.8f),
                                modifier = Modifier.padding(2.dp))
                        },
                        darkThemeEnabled = ThemeUtils.systemInDarkTheme(requireContext())
                    )

                    Row(modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End) {
                        Button(onClick = onConfirm,
                            enabled = filterEnabled.value,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Transparent,
                                contentColor = MaterialTheme.colors.secondary,
                                disabledBackgroundColor = Color.Transparent),
                            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)) {
                            Text(text = stringResource(id = R.string.button_apply_label)
                                , style = MaterialTheme.typography.h6)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun FiltersAppliedScreen(
        show: Boolean,
        onCancelClick:() -> Unit,
        modifier: Modifier = Modifier
    ){
        if(!show)
            return

        /*Filter is cancelled when the system back press key is pressed*/
        BackPressHandler(onBackPressed = onCancelClick)
        Row(modifier = modifier
            .background(MaterialTheme.colors.primaryVariant.copy(0.8f))
            .fillMaxWidth()
            .padding(bottom = 4.dp, top = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically) {

            val chipItems = mutableListOf<ChipItem>()
            if(viewModel.categoryFilter.value.isNotEmpty()){
                chipItems.add(
                    ChipItem(
                        text = viewModel.categoryFilter.value,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Category, contentDescription = "",
                                modifier = Modifier.clip(CircleShape))
                        }
                    )
                )
            }
            if(viewModel.placeFilter.value.isNotEmpty()){
                chipItems.add(
                    ChipItem(
                        text = viewModel.placeFilter.value,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Place, contentDescription = "",
                                modifier = Modifier.clip(CircleShape))
                        }
                    )
                )
            }
            if(viewModel.barcodeFilter.value.isNotEmpty()){
                chipItems.add(
                    ChipItem(
                        text = viewModel.barcodeFilter.value,
                        leadingIcon = {
                            Icon(painter = painterResource(id = R.drawable.ic_barcode),
                                contentDescription = "",
                                modifier = Modifier.clip(CircleShape))
                        }
                    )
                )
            }

            CustomChipList(
                items = chipItems.toList(),
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()))

            IconButton(onClick = onCancelClick) {
                Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "",
                    tint = MaterialTheme.colors.onPrimary)
            }
        }
    }

    @Composable
    fun HomeAppBar(
        onSearchClick: () -> Unit, onFilterClick: () -> Unit, onDeleteAllClicked: () -> Unit,
        onSettingsClicked: () -> Unit,
        onSearchWithBarcode: () -> Unit,
        appBarActionsEnabled: Boolean
    ){
        ShowTopAppBar(stringResource(R.string.app_name),
            navigationIcon = null
            , actionItems = listOf(
                AppBarAction(stringResource(id = R.string.search_view_hint),
                    Icons.Filled.Search, onSearchClick, enabled = appBarActionsEnabled),
                AppBarAction(stringResource(id = R.string.filter_dialog_title),
                    Icons.Filled.FilterList,onFilterClick,enabled = (appBarActionsEnabled and !viewModel.isFiltering.value)),
                AppBarAction(stringResource(id = R.string.delete_all_menu_option),
                    null,onDeleteAllClicked,enabled = appBarActionsEnabled),
                AppBarAction(stringResource(id = R.string.scan_barcode_prompt),
                    ImageUtils.createImageVector(drawableRes = R.drawable.ic_barcode),
                    onSearchWithBarcode,enabled = (appBarActionsEnabled and !viewModel.isFiltering.value)),
                AppBarAction(stringResource(id = R.string.setting_fragment_title),null,onSettingsClicked)
            ))
    }
}