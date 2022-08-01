package com.example.pricerecorder.homeFragment

import android.app.Application
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.*
import android.widget.CompoundButton
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.pricerecorder.*
import com.example.pricerecorder.R
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.databinding.*
import com.example.pricerecorder.theme.*
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.ceil

class HomeFragment:Fragment() {
    private lateinit var viewModel: HomeViewModel

    private lateinit var binding: HomeFragmentBinding
    /*Used to prevent multiple dialogs from appearing*/
    private var filterDialogDisplayed = false

    private var filterOptionSelected : CompoundButton? = null
    private var filterBy : String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val application: Application = requireNotNull(this.activity).application
        val viewModelFactory = HomeViewModelFactory(application)
        viewModel = ViewModelProvider(this, viewModelFactory)[HomeViewModel::class.java]
        createNotificationChannel(requireContext())

        return ComposeView(requireContext()).apply {
            setContent {
                HomeScreen()
            }
        }
    }

    private fun onFilterProducts(filterBinding:FilterMenuDialogBinding):Boolean{
        when(filterOptionSelected){
            filterBinding.placeSwitch -> {
                filterBy = filterBinding.placeAutoComplete.text.toString()
                if(filterBy.isNullOrEmpty())
                    return false
                viewModel.filterByPlace(filterBy!!)
            }
            filterBinding.categorySwitch -> {
                filterBy = filterBinding.categoryAutoComplete.text.toString()
                if(filterBy.isNullOrEmpty())
                    return false
                if(filterBy == resources.getString(R.string.option_uncategorized))
                    viewModel.filterByCategory("")
                else
                    viewModel.filterByCategory(filterBy)
            }
            filterBinding.dateSwitch -> {
                if(filterBy.isNullOrEmpty())
                    return false
                viewModel.filterByDate(filterBy!!)
            }
            filterBinding.priceSwitch -> {
                val values = filterBinding.priceSliderView.values
                filterBy = "$${values[0].toInt()} - $${values[1].toInt()}"
                viewModel.filterByPriceRange(values[0],values[1])
            }
        }
        //adapter.submitList(viewModel.filteredList)
        return true
    }

    /*Implements an OnCheckedChangeListener applied to every and each one of the switches of the filter dialog*/
    private fun changeChecker(filterBinding: FilterMenuDialogBinding) : CompoundButton.OnCheckedChangeListener{
        return CompoundButton.OnCheckedChangeListener { selected, isChecked ->
            filterBinding.also { f ->
                if(isChecked){
                    if(selected != f.categorySwitch) {
                        f.categorySwitch.isChecked = false
                        f.categoryInput.visibility = View.GONE
                    }else{
                        //f.progressBar.visibility = View.VISIBLE
                        //val arrayAdapter = ArrayAdapter(requireContext(),R.layout.drowpdown_item,
                            //viewModel.getListOfCategories(resources))
                        //f.categoryAutoComplete.setAdapter(arrayAdapter)
                        //f.progressBar.visibility = View.GONE
                        f.categoryInput.visibility = View.VISIBLE
                    }

                    if(selected != f.placeSwitch) {
                        f.placeSwitch.isChecked = false
                        f.placeInput.visibility = View.GONE
                    }else{
                        //f.progressBar.visibility = View.VISIBLE
                        //val arrayAdapter = ArrayAdapter(requireContext(),R.layout.drowpdown_item,
                            //viewModel.getListOfPlaces())
                        //f.placeAutoComplete.setAdapter(arrayAdapter)
                       // f.progressBar.visibility = View.GONE
                        f.placeInput.visibility = View.VISIBLE
                    }

                    if(selected != f.dateSwitch){
                        f.dateSwitch.isChecked = false
                        f.dateInput.visibility = View.GONE
                    }else{
                        val today = DateUtils.formatDate(Calendar.getInstance().timeInMillis)
                        filterBinding.dateInput.setText(today)
                        filterBy = today
                        filterBinding.dateInput.visibility = View.VISIBLE
                        filterBinding.dateInput.setOnClickListener {
                            val datePicker = MaterialDatePicker.Builder.datePicker()
                                .setTitleText(getString(R.string.date_picker_title))
                                .setTheme(R.style.CustomDatePicker)
                                .build()
                            datePicker.addOnPositiveButtonClickListener {
                                filterBy = DateUtils.formatDate(it)
                                filterBinding.dateInput.setText(filterBy)
                            }
                            datePicker.show(parentFragmentManager,null)
                        }
                    }

                    if(selected != f.priceSwitch){
                        f.priceSwitch.isChecked = false
                        f.priceSliderView.visibility = View.GONE
                    }else{
                        f.priceSliderView.apply {
                            valueFrom = 0f
                            valueTo = viewModel.getMaxPrice()
                            values = mutableListOf(valueFrom,valueTo)
                            stepSize = adaptStepSize(valueTo)
                            setLabelFormatter { value -> return@setLabelFormatter "$${value.toInt()}" }
                            visibility = View.VISIBLE
                        }
                    }
                }else{
                    when(selected){
                        f.categorySwitch -> f.categoryInput.visibility = View.GONE
                        f.placeSwitch -> f.placeInput.visibility = View.GONE
                        f.dateSwitch -> f.dateInput.visibility = View.GONE
                        f.priceSwitch -> f.priceSliderView.visibility = View.GONE
                    }
                }
            }
        }
    }

    /*Adapts the range slider step size according to its maximum value*/
    private fun adaptStepSize(value: Float): Float {
        return ceil(value * 0.01f)
    }

    /*Creates a dialog box that allows users to filter from all products based on determined criteria*/
    private fun createCustomFilterDialog(){
        filterDialogDisplayed = true
        val filterBinding = FilterMenuDialogBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(filterBinding.root)
            .setNegativeButton(resources.getString(R.string.cancel_button_string)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(resources.getString(R.string.accept_button_string)) {dialog,_ ->
                filterOptionSelected = getFilterCheckedSwitch(filterBinding)
                if(filterOptionSelected != null){
                    if(onFilterProducts(filterBinding)){
                        //binding.filterByTextview.text = resources.getString(R.string.filter_by_string,filterBy)
                        //binding.filterFab.hide()
                        //binding.filterByView.visibility = View.VISIBLE
                    }else
                        filterOptionSelected = null
                }
                dialog.dismiss()}
            .create()

        changeChecker(filterBinding).also {
            filterBinding.categorySwitch.setOnCheckedChangeListener(it)
            filterBinding.placeSwitch.setOnCheckedChangeListener(it)
            filterBinding.priceSwitch.setOnCheckedChangeListener(it)
            filterBinding.dateSwitch.setOnCheckedChangeListener(it)
        }

        dialog.setOnDismissListener { filterDialogDisplayed = false }
        dialog.show()
    }

    /*Returns the switch checked by the user in the filter dialog*/
    private fun getFilterCheckedSwitch(f:FilterMenuDialogBinding) : CompoundButton?{
        when{
            f.placeSwitch.isChecked -> return f.placeSwitch
            f.categorySwitch.isChecked -> return f.categorySwitch
            f.dateSwitch.isChecked -> return f.dateSwitch
            f.priceSwitch.isChecked -> return f.priceSwitch
        }
        return null
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

        PriceRecorderTheme {
            Scaffold(scaffoldState = scaffoldState,
                backgroundColor = MaterialTheme.colors.background
                ,topBar = { MainAppBar(searchWidgetState,searchTextState) },
                floatingActionButton = { AddFloatingActionButton(enabled = true,
                    onClick = { navigateToAddFragment() }) },
                floatingActionButtonPosition = FabPosition.Center) {
                ProductsList(showSnackbar = { msg, actionLabel, onActionPerformed ->
                    coroutineScope.launch {
                        /*Snackbar is shown on screen and when the action is clicked, onActionPerformed is invoked*/
                        when(scaffoldState.snackbarHostState.showSnackbar(message = msg,actionLabel = actionLabel)){
                            SnackbarResult.ActionPerformed -> onActionPerformed()
                            else -> {}
                        }
                    }
                },
                modifier = Modifier.padding(it))
            }
        }
    }

    /*Determines the app bar to be displayed, either the default appbar or a search app bar*/
    @Composable
    private fun MainAppBar(searchWidgetState: SearchWidgetState, searchTextState:String){
        var showDeleteAllDialog by remember {
            mutableStateOf(false)
        }

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
                    onFilterClick = { createCustomFilterDialog() },
                    onDeleteAllClicked = { showDeleteAllDialog = true },
                    onSettingsClicked = { navigateToSettingsFragment() })
            }
            else -> {
                SearchAppBar(text = searchTextState,
                    onTextChange = {
                        viewModel.updateSearchTextState(it)
                    },
                    onCloseClicked = {
                        viewModel.updateSearchTextState("")
                        viewModel.updateSearchWidgetState(SearchWidgetState.CLOSED) })
            }
        }
    }

    /*List displayed in the home screen with all stored products*/
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun ProductsList(showSnackbar:(String, String, () -> Unit) -> Unit, modifier: Modifier = Modifier){
        val list by viewModel.products.observeAsState(listOf())

        if(list.isNotEmpty()){
            Box(modifier = modifier) {
                /*Used to remember the state of the list through recompositions*/
                val state = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()
                /*Used to remember whether the button should be visible or not*/
                val showScrollToTopButton by remember {
                    derivedStateOf {
                        state.firstVisibleItemIndex > 0
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxWidth(),
                    state = state, contentPadding = PaddingValues(bottom = 44.dp)){
                    items(list, key = {it.getId()}){ product ->
                        val dismissState = rememberDismissState(
                            confirmStateChange = {
                                /*If item was swiped to start, then its deleted from the DB*/
                                if(it == DismissValue.DismissedToStart){
                                    viewModel.deleteProduct(product = product)
                                    showSnackbar(getString(R.string.product_deleted_msg),getString(R.string.undo_action_msg)){
                                        viewModel.addProduct(product)
                                    }
                                }
                                true
                            }
                        )

                        SwipeToDismiss(state = dismissState,
                            directions = setOf(DismissDirection.EndToStart),
                            background = {
                                val color by animateColorAsState(
                                    targetValue = when(dismissState.targetValue){
                                        DismissValue.DismissedToStart -> Color.Red
                                        else -> MaterialTheme.colors.background
                                    })
                                val icon = Icons.Default.Delete
                                /*Used to animate the icon size as the item is being swiped*/
                                val scale by animateFloatAsState(
                                    targetValue = if(dismissState.targetValue == DismissValue.Default) 0.8f else 1.2f)
                                val alignment = Alignment.CenterEnd

                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(start = 12.dp, end = 12.dp),
                                    contentAlignment = alignment) {
                                    Icon(imageVector = icon, contentDescription = "",
                                        modifier = Modifier.scale(scale))
                                }
                            },
                            /*swipe fraction limit where the action is confirmed*/
                            dismissThresholds = { FractionalThreshold(0.3f) },
                            dismissContent = {
                                /*swipeable content*/
                                Card(modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium.copy(CornerSize(0.dp)),
                                    /*Adds elevation to item as its beign swiped*/
                                    elevation = animateDpAsState(
                                        targetValue = if(dismissState.dismissDirection != null) 6.dp else 0.dp).value) {
                                    ListItemProduct(product = product, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        )
                    }
                }

                /*launches a coroutine to scroll to the first item in the list with animation*/
                AnimatedVisibility(visible = showScrollToTopButton,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd)) {
                    ScrollToTopButton {
                        coroutineScope.launch {
                            state.animateScrollToItem(0)
                        }
                    }
                }
            }
        }else{
            when(viewModel.searching.value){
                false -> HomeEmptyBackgroundScreen(drawableRes = R.drawable.ic_connection_error,
                    R.string.no_elements_string)
                true -> HomeEmptyBackgroundScreen(drawableRes = R.drawable.ic_broken_image,
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
                Icon(painter = painterResource(id = R.drawable.ic_keyboard_double_arrow_up), contentDescription = "",
                    tint = MaterialTheme.colors.onSurface)
            }
        }
    }

    /*Layout shown when there are no elements stored yet*/
    @Composable
    private fun HomeEmptyBackgroundScreen(@DrawableRes drawableRes: Int,
                                          @StringRes stringRes:Int, modifier: Modifier = Modifier){
        Column(modifier = modifier
            .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = drawableRes), contentDescription = "",
                modifier
                    .widthIn(120.dp)
                    .heightIn(120.dp))
            Text(text = stringResource(id = stringRes),
                modifier
                    .padding(top = 8.dp, start = 32.dp, end = 32.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.h5,
                color = if(!isSystemInDarkTheme()) PetrolBlue else SilverGrey)
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
                            color = if(!isSystemInDarkTheme()) PetrolBlue else SilverGrey,
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
            Text(text = stringResource(id = R.string.price_title), modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.subtitle1,color = MaterialTheme.colors.primaryVariant)
            if(!editPrice){
                Text(text = "$${product.getPrice()}",
                    style = MaterialTheme.typography.h6,color = MaterialTheme.colors.secondary)
            }else{
                /*TODO("asegurar que teclado muestra punto o coma, deshabilitar click largo")*/
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
                            tint = if(!errorState) MaterialTheme.colors.primaryVariant
                                else MaterialTheme.colors.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Number),
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
                    textStyle = MaterialTheme.typography.h6)
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
                    if(product.getCategory().isNotEmpty()){
                        Text(text = product.getCategory(), modifier = Modifier.padding(start = 16.dp, end = 16.dp),
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
                    
                    Row(modifier = Modifier
                        .padding(top = 4.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start) {
                        Text(text = stringResource(id = R.string.modified_date_desc), modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.subtitle1,color = MaterialTheme.colors.primaryVariant)
                        Text(text = DateUtils.formatDate(product.getUpdateDate()),
                            style = MaterialTheme.typography.subtitle1,color = MaterialTheme.colors.onSurface)
                    }
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
                product.getImage()?.let {
                    Image(bitmap = it.asImageBitmap(), contentDescription = "",
                        modifier = imgModifier, contentScale = ContentScale.Crop)
                    return@Surface
                }
                Image(painter = painterResource(id = R.drawable.ic_image), contentDescription = "",
                    modifier = imgModifier, contentScale = ContentScale.Crop)
            }
        }
    }

    @Composable
    private fun DetailDialogBottomActionBar(product: Product, onDismiss: () -> Unit,
                                            onEditPriceClicked:() -> Unit){
        /*Creates a dialog that provides the option to delete the current selected product*/
        var showDeleteProductDialog by remember {
            mutableStateOf(false)
        }
        CustomAlertDialog(show = showDeleteProductDialog,
            title = stringResource(id = R.string.delete_product_string),
            msg = {
                Text(text = stringResource(id = R.string.delete_product_dialog_msg),
                    color = MaterialTheme.colors.onSurface)
            },
            confirmButtonText = stringResource(id = R.string.accept_button_string),
            dismissButtonText = stringResource(id = R.string.cancel_button_string),
            onConfirm = {
                showDeleteProductDialog = false
                onDismiss()
                viewModel.deleteProduct(product)
                Toast.makeText(requireContext(),getString(R.string.delete_success_msg,product.getDescription()),
                    Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showDeleteProductDialog = false
            })

        Row(modifier = Modifier
            .padding(top = 8.dp, bottom = 3.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.primary),
            horizontalArrangement = Arrangement.End) {

            val actions = listOf(
                AppBarAction(stringResource(id = R.string.share_product_button_desc), icon = Icons.Outlined.Share,{}),
                AppBarAction(stringResource(id = R.string.edit_fragment_title), icon = Icons.Outlined.Edit, action = {
                    onDismiss()
                    navigateToEditFragment(product.getId())
                }),
                AppBarAction(stringResource(id = R.string.delete_product_string), icon = Icons.Outlined.Delete, action = {
                    showDeleteProductDialog = true
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
                        tint = Black, modifier = Modifier
                            .width(44.dp)
                            .height(44.dp))
                }
            }
        }
    }

    /*@Composable
    private fun filterProductsDialog(show:Boolean,onConfirm:(Double) -> Unit,onDismiss: () -> Unit,
                                     modifier: Modifier = Modifier){
        if(!show)
            return

        var categorySwitchState by remember { mutableStateOf(false) }

        Surface(modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
            border = BorderStroke(3.dp,MaterialTheme.colors.onSurface),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colors.surface) {
            Column(horizontalAlignment = Alignment.CenterHorizontally){
                Text(text = stringResource(id = R.string.filter_dialog_title),
                    style = MaterialTheme.typography.h6, modifier = Modifier.padding(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
                    Text(text = stringResource(id = R.string.category_input_string),
                        style = MaterialTheme.typography.subtitle1,color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.weight(1f))
                    Switch(checked = categorySwitchState, onCheckedChange = { categorySwitchState = it })
                }
                Divider(thickness = 2.dp,
                    color = MaterialTheme.colors.secondary.copy(0.7f),
                    modifier = Modifier.padding(start = 30.dp, end = 30.dp))
            }
        }
    }

    @Preview(heightDp = 450, widthDp = 360)
    @Composable
    fun filterDialogPreview(){
        PriceRecorderTheme {
            filterProductsDialog(show = true, onConfirm = {}, onDismiss = {})
        }
    }*/

    @Preview(heightDp = 450, widthDp = 360, uiMode = UI_MODE_NIGHT_YES)
    @Composable
    fun ProductDetailPreview(){
        PriceRecorderTheme {
            ProductDetail(product = Product("Leche La Serenisima",250.0,"Carrefour Market",
                "Lacteos",DateUtils.getCurrentDate()),{})
        }
    }

    //@Preview(widthDp = 360)
    @Composable
    private fun ListItemProductPreview(){
        PriceRecorderTheme {
            ListItemProduct(product = Product("Leche La Serenisima",250.0,"Carrefour Market",
                "",DateUtils.getCurrentDate())
            )
        }
    }
}