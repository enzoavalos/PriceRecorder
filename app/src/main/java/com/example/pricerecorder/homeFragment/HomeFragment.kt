package com.example.pricerecorder.homeFragment

import android.app.Application
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.*
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pricerecorder.*
import com.example.pricerecorder.R
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.AddPriceDialogBinding
import com.example.pricerecorder.databinding.DetailFragmentBinding
import com.example.pricerecorder.databinding.FilterMenuDialogBinding
import com.example.pricerecorder.databinding.HomeFragmentBinding
import com.example.pricerecorder.theme.*
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.ceil

class HomeFragment:Fragment() {
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: ProductAdapter
    private lateinit var binding: HomeFragmentBinding
    private lateinit var mainMenu: Menu
    private lateinit var searchView: SearchView
    /*Used to prevent multiple dialogs from appearing*/
    private var detailDialogDisplayed = false
    private var priceDialogDisplayed = false
    private var filterDialogDisplayed = false

    private var filterOptionSelected : CompoundButton? = null
    private var filterBy : String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        //binding = DataBindingUtil.inflate(inflater,R.layout.home_fragment,container,false)

        val application: Application = requireNotNull(this.activity).application
        val dataSource = ProductDatabase.getInstance(application).productDatabaseDao
        val viewModelFactory = HomeViewModelFactory(dataSource,application)
        viewModel = ViewModelProvider(this, viewModelFactory)[HomeViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                PriceRecorderTheme {
                    homeScreen()
                }
            }
        }

        /*binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.mainToolbar.setContent {
            PriceRecorderTheme {
                homeAppBar(onSearchClick = { Toast.makeText(requireContext(),"Buscar",Toast.LENGTH_SHORT).show() },
                    onFilterClick = { createCustomFilterDialog() },
                    onDeleteAllClicked = { createDeleteAllDialog() },
                    onSettingsClicked = { navigateToSettingsFragment() })
            }
        }

        //Recycler view adapter, a click listener is passed as a lambda expression
        val manager = LinearLayoutManager(context)
        adapter = ProductAdapter(ProductListener {
            if(!detailDialogDisplayed)
                createCustomDetailDialog(it)
        })

        //Callback created to implement swipe to delete behaviour
        val swipeDelete = object : SwipeToDeleteCallback(requireContext()) {
            //Called when a viewHolder is swiped by the user
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val swipedItem = adapter.getItemProduct(viewHolder as ProductAdapter.ProductViewHolder)
                viewModel.deleteProduct(swipedItem!!)
                /*if(!searchView.isIconified)
                    collapseSearchView()*/
                Snackbar.make(view!!,resources.getString(R.string.product_deleted_msg),Snackbar.LENGTH_SHORT)
                    .setAction(resources.getString(R.string.undo_action_msg)){
                        viewModel.addProduct(swipedItem)
                    }
                    .show()
            }
        }
        val touchHelper = ItemTouchHelper(swipeDelete)
        touchHelper.attachToRecyclerView(binding.productRecyclerView)

        //Observers of both fab buttons of the home page
        viewModel.fabClicked.observe(viewLifecycleOwner) {
            it?.let {
                when (it) {
                    R.id.add_fab -> navigateToAddFragment()
                    R.id.filter_fab -> {
                        if (!filterDialogDisplayed and (filterOptionSelected == null))
                            createCustomFilterDialog()
                    }
                }
                viewModel.onNavigated()
            }
        }

        binding.cancelFilterButton.setOnClickListener {
            binding.filterByView.visibility = View.GONE
            viewModel.filteredList = null
            filterOptionSelected = null
            if(!searchView.isIconified){
                if(!searchView.query.isNullOrEmpty()){
                    val queryResult = viewModel.filterByUserSearch(searchView.query.toString())
                    showNoResultsFoundLayout(queryResult.isEmpty())
                    adapter.submitList(queryResult)
                }else{
                    showNoResultsFoundLayout(viewModel.products.value.isNullOrEmpty())
                    adapter.submitList(viewModel.products.value)
                }
            }
            else{
                showNoResultsFoundLayout(viewModel.products.value.isNullOrEmpty())
                adapter.submitList(viewModel.products.value)
                binding.filterFab.show()
            }
        }

        viewModel.products.observe(viewLifecycleOwner) {
            binding.filterFab.visibility =
                if (!it.isNullOrEmpty() and (filterOptionSelected == null)) View.VISIBLE else View.GONE
            showProgressBar(false)
            showEmptyLayout(it.isNullOrEmpty())
            filterOptionSelected = null
            binding.filterByView.visibility = View.GONE
            adapter.submitList(it)
        }

        initRecyclerView(binding,manager,adapter)
        setFabOnScrollBehaviour()
        return binding.root*/
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
        adapter.submitList(viewModel.filteredList)
        showNoResultsFoundLayout(viewModel.filteredList.isNullOrEmpty())
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
                        f.progressBar.visibility = View.VISIBLE
                        val arrayAdapter = ArrayAdapter(requireContext(),R.layout.drowpdown_item,
                            viewModel.getListOfCategories(resources))
                        f.categoryAutoComplete.setAdapter(arrayAdapter)
                        f.progressBar.visibility = View.GONE
                        f.categoryInput.visibility = View.VISIBLE
                    }

                    if(selected != f.placeSwitch) {
                        f.placeSwitch.isChecked = false
                        f.placeInput.visibility = View.GONE
                    }else{
                        f.progressBar.visibility = View.VISIBLE
                        val arrayAdapter = ArrayAdapter(requireContext(),R.layout.drowpdown_item,
                            viewModel.getListOfPlaces())
                        f.placeAutoComplete.setAdapter(arrayAdapter)
                        f.progressBar.visibility = View.GONE
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
                        binding.filterByTextview.text = resources.getString(R.string.filter_by_string,filterBy)
                        binding.filterFab.hide()
                        binding.filterByView.visibility = View.VISIBLE
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

    /* Configures the behaviour of the floating buttons when the screen is scrolled*/
    private fun setFabOnScrollBehaviour() {
        binding.nestedScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            if(binding.addFab.isVisible){
                if(scrollY != 0){
                    binding.addFab.shrink()
                }else{
                    binding.addFab.extend()
                }
            }
        })
    }

    // Initializes the recyclerview
    private fun initRecyclerView(binding: HomeFragmentBinding,
                                 manager: LinearLayoutManager,
                                 adapter: ProductAdapter){
        binding.productRecyclerView.apply {
            layoutManager = manager
            addItemDecoration(SpacingItemDecoration(3))
            this.adapter = adapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.overflow_menu,menu)
        mainMenu = menu

        /* Disables the option to delete all elements in the menu, when there are no elements.
        * Is automatically updated whenever there is a change in the list of products*/
        val deleteItem = menu.findItem(R.id.op_delete_all)
        viewModel.products.observe(viewLifecycleOwner) {
            deleteItem.isEnabled = !it.isNullOrEmpty()
        }

        //Sets the functionality for the search view in the toolbar
        val menuItem = menu.findItem(R.id.op_search)
        searchView = menuItem.actionView as SearchView
        searchView.queryHint = resources.getString(R.string.search_view_hint)
        searchView.maxWidth = Int.MAX_VALUE

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            //Invoked when the searchView text is changed
            override fun onQueryTextChange(newText: String?): Boolean {
                showProgressBar(true)
                val searchText = newText!!.lowercase(Locale.getDefault())
                var resultList = mutableListOf<Product>()

                //Creates a temporary list with the elements that match with the search
                if(searchText.isNotEmpty()){
                    resultList = viewModel.filterByUserSearch(searchText)
                    showProgressBar(false)
                    adapter.submitList(resultList)
                }else{
                    showProgressBar(false)
                    if(viewModel.filteredList != null)
                        adapter.submitList(viewModel.filteredList)
                    else
                        adapter.submitList(viewModel.products.value)
                }
                showNoResultsFoundLayout((resultList.isEmpty() and searchText.isNotEmpty() and
                        !viewModel.products.value.isNullOrEmpty()))
                return true
            }
        })

        searchView.addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener{
            //Invoked when the search view is attached to the screen(search bar is opened)
            override fun onViewAttachedToWindow(v: View?) {
                binding.apply {
                    addFab.hide()
                    filterFab.hide()
                    mainMenu.setGroupVisible(R.id.menu_group,false)
                }
            }

            //Invoked when the searchView is detached from the screen
            override fun onViewDetachedFromWindow(v: View?) {
                mainMenu.setGroupVisible(R.id.menu_group,true)
                binding.addFab.show()
                if(binding.nestedScrollView.scrollY == 0) {
                    binding.apply {
                        addFab.extend()
                        if((filterOptionSelected == null)and(!viewModel!!.products.value.isNullOrEmpty()))
                            filterFab.show()
                    }
                }
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.op_delete_all -> {
                if(!viewModel.products.value.isNullOrEmpty()){
                    //Creates a dialog box that gives you the opportunity to cancel the operation
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(resources.getString(R.string.delete_all_dialog_title))
                        .setMessage(resources.getString(R.string.delete_all_dialog_msg))
                        .setNegativeButton(resources.getString(R.string.button_cancel_string))
                        { dialog, _ -> dialog!!.dismiss() }
                        .setPositiveButton(resources.getString(R.string.button_accept_string)) { dialog, _ ->
                            viewModel.clear()
                            Toast.makeText(context,resources.getString(R.string.delete_success_msg), Toast.LENGTH_SHORT).show()
                            dialog!!.dismiss()
                        }
                        .show()
                }
            }
            R.id.op_settings -> navigateToSettingsFragment()
        }
        return true
    }

    private fun createDeleteAllDialog(){
        if(!viewModel.products.value.isNullOrEmpty()){
            //Creates a dialog box that gives you the opportunity to cancel the operation
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.delete_all_dialog_title))
                .setMessage(resources.getString(R.string.delete_all_dialog_msg))
                .setNegativeButton(resources.getString(R.string.button_cancel_string))
                { dialog, _ -> dialog!!.dismiss() }
                .setPositiveButton(resources.getString(R.string.button_accept_string)) { dialog, _ ->
                    viewModel.clear()
                    Toast.makeText(context,resources.getString(R.string.delete_success_msg), Toast.LENGTH_SHORT).show()
                    dialog!!.dismiss()
                }
                .show()
        }
    }

    /*Creates a custom dialog that displays the details of the product associated*/
    private fun createCustomDetailDialog(p:Product){
        detailDialogDisplayed = true
        val dialogBinding : DetailFragmentBinding = DetailFragmentBinding.inflate(layoutInflater)
        dialogBinding.product = p
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root).create()
        onCreateCustomDetailDialog(dialogBinding,dialog)
        dialog.apply {
            show()
            window?.setBackgroundDrawableResource(R.color.transparent)
        }

        dialog.setOnDismissListener { detailDialogDisplayed = false }
    }

    /*Sets the content of the views and certain behaviours of the detail fragment used as a custom dialog box*/
    private fun onCreateCustomDetailDialog(b:DetailFragmentBinding, detailDialog: AlertDialog){
        var deleteDialogDisplayed = false
        b.apply {
            priceToDateText.text = resources.getString(R.string.current_price_string,DateUtils.formatDate(product!!.getUpdateDate()))
            categoryTextview.isVisible = product!!.getCategory().isNotEmpty()
            product!!.getImage()?.let { productDetailImg.setImageBitmap(it) }

            buttonAddPrice.setOnClickListener {
                if(!priceDialogDisplayed)
                    createCustomPriceDialog(detailDialog, b)
            }

            buttonDeleteProduct.setOnClickListener {
                if(deleteDialogDisplayed)
                    return@setOnClickListener
                val deleteDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(resources.getString(R.string.delete_product_string))
                    .setMessage(resources.getString(R.string.delete_product_dialog_msg))
                    .setNegativeButton(resources.getString(R.string.button_cancel_string))
                    { dialog, _ -> dialog!!.dismiss() }
                    .setPositiveButton(resources.getString(R.string.button_accept_string)) { dialog, _ ->
                        viewModel.deleteProduct(b.product!!)
                        dialog!!.dismiss()
                        detailDialog.dismiss()
                        Toast.makeText(context,resources.getString(R.string.delete_success_msg), Toast.LENGTH_SHORT).show()
                    }
                    .create()
                deleteDialogDisplayed = true
                deleteDialog.show()

                deleteDialog.setOnDismissListener { deleteDialogDisplayed = false }
            }

            buttonEditProduct.setOnClickListener {
                detailDialog.dismiss()
                navigateToEditFragment(product!!.getProductId())
            }
        }
    }

    /*Creates a custom dialog box for the users to update the price of a given product*/
    private fun createCustomPriceDialog(detailDialog: AlertDialog,detailFragmentBinding: DetailFragmentBinding){
        priceDialogDisplayed = true
        val priceDialogBinding = AddPriceDialogBinding.inflate(layoutInflater)
        val priceDialog = AlertDialog.Builder(requireContext())
            .setView(priceDialogBinding.root).create()

        priceDialogBinding.addPriceEdittext.apply {
            addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }
                override fun afterTextChanged(s: Editable?) {
                    if(this@apply.validatePositiveNumericInputDouble())
                        priceDialogBinding.acceptButton.setAcceptButtonEnabled(true)
                    else
                        priceDialogBinding.acceptButton.setAcceptButtonEnabled(false)
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!error.isNullOrEmpty())
                        error = null
                    removeTextChangedListener(this)
                    CurrencyFormatter.formatInput(this@apply)
                    addTextChangedListener(this)
                }
            })

            isLongClickable = false
            filters = arrayOf(InputFilter.LengthFilter(9))
        }

        priceDialogBinding.acceptButton.setOnClickListener {
            /*if(!searchView.isIconified)
                collapseSearchView()*/
            val newPrice = priceDialogBinding.addPriceEdittext.text.toString().toDouble()
            priceDialog.dismiss()
            detailFragmentBinding.product!!.updatePrice(newPrice)
            viewModel.updateProduct(detailFragmentBinding.product!!)
            detailDialog.dismiss()
            Toast.makeText(context,resources.getString(R.string.price_updated_msg),Toast.LENGTH_SHORT).show()
        }

        priceDialog.setOnDismissListener { priceDialogDisplayed = false }

        priceDialog.show()
        priceDialog.window?.setBackgroundDrawableResource(R.color.transparent)
    }

    /*Called when the searchView is attached to the window in order to collapse it*/
    private fun collapseSearchView(){
        mainMenu.findItem(R.id.op_search).collapseActionView()
    }

    /*Sets the visibility for the layout shown when there are no elements to show in the recycler view*/
    private fun showEmptyLayout(show:Boolean){
        binding.emptyLayout.visibility = if(show) View.VISIBLE else View.GONE
    }

    /*Sets the visibility for the layout shown when there are no matches for the user search*/
    private fun showNoResultsFoundLayout(show: Boolean){
        binding.emptySearchLayout.visibility = if(show) View.VISIBLE else View.GONE
    }

    /*Sets the visibility for the progress bar shown whenever there are operations running on the data used by
    the recycler view, such as loading and filtering*/
    private fun showProgressBar(show: Boolean){
        binding.progressBar.visibility = if(show) View.VISIBLE else View.GONE
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
    fun homeScreen() {
        val searchWidgetState by viewModel.searchWidgetState
        val searchTextState by viewModel.searchTextState

        PriceRecorderTheme {
            Scaffold(backgroundColor = MaterialTheme.colors.background
                ,topBar = { mainAppBar(searchWidgetState,searchTextState) },
                floatingActionButton = { addFloatingActionButton {navigateToAddFragment()} },
                floatingActionButtonPosition = FabPosition.Center) {
                productsList()
            }
        }
    }

    /*Determines the app bar to be displayed, either the deafult appbar or a search app bar*/
    @Composable
    private fun mainAppBar(searchWidgetState: SearchWidgetState,searchTextState:String){
        when(searchWidgetState){
            SearchWidgetState.CLOSED -> {
                homeAppBar(onSearchClick = { viewModel.updateSearchWidgetState(SearchWidgetState.OPENED) },
                    onFilterClick = {createCustomFilterDialog()},
                    onDeleteAllClicked = {createDeleteAllDialog()},
                    onSettingsClicked = {navigateToSettingsFragment()})
            }
            else -> {
                searchAppBar(text = searchTextState,
                    onTextChange = {},
                    onCloseClicked = { viewModel.updateSearchWidgetState(SearchWidgetState.CLOSED) },
                    onSearchClicked = {})
            }
        }
    }

    /*List displayed in the home screen with all stored products*/
    @Composable
    private fun productsList(modifier: Modifier = Modifier){
        val items by viewModel.products.observeAsState(listOf())

        if(items.isNotEmpty()){
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
                    items(items, key = {it.getId()}){
                        listItemProduct(product = it, modifier = Modifier.fillMaxWidth())
                    }
                }

                /*launches a coroutine to scroll to the first item in the list with animation*/
                AnimatedVisibility(visible = showScrollToTopButton,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd)) {
                    scrollToTopButton {
                        coroutineScope.launch {
                            state.animateScrollToItem(0)
                        }
                    }
                }
            }
        }else
            noElementsToShowScreen()
    }

    /*Button to scroll to top of list, enabled when the first item of the list is no longer shown in screen*/
    @Composable
    private fun scrollToTopButton(onClick: () -> Unit){
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
    //@Preview(widthDp = 360, heightDp = 720, showBackground = true)
    @Composable
    private fun noElementsToShowScreen(modifier: Modifier = Modifier){
        Column(modifier = modifier
            .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = R.drawable.ic_connection_error), contentDescription = "",
                modifier
                    .widthIn(120.dp)
                    .heightIn(120.dp))
            Text(text = stringResource(id = R.string.no_elements_string),modifier.padding(8.dp),
                style = MaterialTheme.typography.h5,
                color = if(!isSystemInDarkTheme()) PetrolBlue else SilverGrey)
        }
    }

    @Composable
    private fun addFloatingActionButton(onClick: () -> Unit){
        FloatingActionButton(onClick = onClick, contentColor = MaterialTheme.colors.secondary) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null,
                tint = MaterialTheme.colors.onSecondary)
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun listItemProduct(product: Product, modifier:Modifier = Modifier){
        /*Keeps track of a state used to determine when the dialog should be shown*/
        var showDetailDialog by remember {
            mutableStateOf(false)
        }
        if(showDetailDialog)
            showCustomDetailDialog(product = product) {
                showDetailDialog = false
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
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp))
                        Text(text = product.getPlaceOfPurchase(),
                            style = MaterialTheme.typography.subtitle2,
                            color = if(!isSystemInDarkTheme()) PetrolBlue else SilverGrey,
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp))
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

    /*Creates a dialog with the product details*/
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun showCustomDetailDialog(product: Product,onDismiss:() -> Unit){
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            productDetail(product = product, onDismiss = onDismiss)
        }
    }

    @Composable
    private fun productDetail(product:Product,onDismiss: () -> Unit, modifier: Modifier = Modifier){
        Box(modifier = modifier
            .background(White.copy(0f))
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
                    
                    Row(modifier = Modifier
                        .padding(top = 4.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(id = R.string.price_title), modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.subtitle1,color = MaterialTheme.colors.primaryVariant)
                        Text(text = "$${product.getPrice()}",
                            style = MaterialTheme.typography.h6,color = MaterialTheme.colors.secondary)
                    }
                    
                    Row(modifier = Modifier
                        .padding(top = 4.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(id = R.string.modified_date_desc), modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.subtitle1,color = MaterialTheme.colors.primaryVariant)
                        Text(text = DateUtils.formatDate(product.getUpdateDate()),
                            style = MaterialTheme.typography.subtitle1,color = MaterialTheme.colors.onSurface)
                    }
                    
                    detailDialogBottomActionBar(product,onDismiss)
                }
            }
            Surface(modifier = Modifier
                .height(90.dp)
                .width(90.dp)
                .align(Alignment.TopCenter), shape = MaterialTheme.shapes.medium,
                border = BorderStroke(3.dp,MaterialTheme.colors.onSurface)) {
                Image(painter = painterResource(id = R.drawable.ic_connection_error), contentDescription = "",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .align(Alignment.Center))
            }
        }
    }

    @Composable
    private fun detailDialogBottomActionBar(product: Product,onDismiss: () -> Unit){
        /*Creates a dialog that provides the option to delete the current selected product*/
        var showDeleteProductDialog by remember {
            mutableStateOf(false)
        }
        if(showDeleteProductDialog){
            customAlertDialog(title = stringResource(id = R.string.delete_product_string),
                msg = stringResource(id = R.string.delete_product_dialog_msg),
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
        }

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
                        onDismiss()
                        navigateToAddFragment()
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

    @Composable
    private fun customAlertDialog(title:String, msg:String?,confirmButtonText:String,
          dismissButtonText:String?,onConfirm:() -> Unit, onDismiss:()->Unit){
        AlertDialog(
            onDismissRequest = {},
            shape = MaterialTheme.shapes.medium.copy(CornerSize(10.dp)),
            title = {
                Text(text = title,
                    color = MaterialTheme.colors.onSurface)
            },
            text = {
                msg?.let {
                    return@let Text(text = msg,
                        color = MaterialTheme.colors.onSurface)
                }
            },
            backgroundColor = MaterialTheme.colors.surface,
            confirmButton = {
                Button(onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary,
                    contentColor = MaterialTheme.colors.onSecondary)) {
                Text(text = confirmButtonText)
            }},
            dismissButton = {
                dismissButtonText?.let {
                    Button(onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error,
                            contentColor = MaterialTheme.colors.onError)) {
                        Text(text = dismissButtonText)
                    }
                }
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        )
    }

    @Preview
    @Composable
    private fun deleteProductPreview(){
        PriceRecorderTheme {
            customAlertDialog(title = stringResource(id = R.string.delete_product_string),
                msg = stringResource(id = R.string.delete_product_dialog_msg),
                confirmButtonText = stringResource(id = R.string.accept_button_string),
                dismissButtonText = stringResource(id = R.string.cancel_button_string),
                onConfirm = {},
                onDismiss = {})
        }
    }

    //@Preview(heightDp = 450, widthDp = 360, uiMode = UI_MODE_NIGHT_YES)
    @Composable
    fun productDetailPreview(){
        PriceRecorderTheme {
            productDetail(product = Product("Leche La Serenisima",250.0,"Carrefour Market",
                "Lacteos",DateUtils.getCurrentDate()),{})
        }
    }

    //@Preview(widthDp = 360)
    @Composable
    private fun listItemProductPreview(){
        PriceRecorderTheme {
            listItemProduct(product = Product("Leche La Serenisima",250.0,"Carrefour Market",
                "",DateUtils.getCurrentDate())
            )
        }
    }

    //@Preview(showBackground = true)
    @Composable
    private fun scrollToTopButtonPreview(){
        PriceRecorderTheme {
            scrollToTopButton {}
        }
    }
}