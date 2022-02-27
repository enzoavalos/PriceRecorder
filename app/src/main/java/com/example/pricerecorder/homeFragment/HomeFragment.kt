package com.example.pricerecorder.homeFragment

import android.app.Application
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.*
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pricerecorder.*
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.AddPriceDialogBinding
import com.example.pricerecorder.databinding.DetailFragmentBinding
import com.example.pricerecorder.databinding.FilterMenuDialogBinding
import com.example.pricerecorder.databinding.HomeFragmentBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.DateFormat
import java.util.*

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
        binding = DataBindingUtil.inflate(inflater,
            R.layout.home_fragment,container,false)

        MainToolbar.show(activity as AppCompatActivity,getString(R.string.app_name),false)

        val application: Application = requireNotNull(this.activity).application
        val dataSource = ProductDatabase.getInstance(application).productDatabaseDao
        val viewModelFactory = HomeViewModelFactory(dataSource,application)
        viewModel = ViewModelProvider(this, viewModelFactory)[HomeViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

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
                if(!searchView.isIconified)
                    collapseSearchView()
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
        viewModel.fabClicked.observe(viewLifecycleOwner,{
                it?.let {
                    when(it){
                        R.id.add_fab -> navigateToAddFragment()
                        R.id.filter_fab -> {
                            if(!filterDialogDisplayed and (filterOptionSelected == null))
                                createCustomFilterDialog()
                        }
                    }
                    viewModel.onNavigated()
                }
        })

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
                adapter.submitList(viewModel.products.value)
                binding.filterFab.show()
            }
        }

        viewModel.products.observe(viewLifecycleOwner, {
            binding.filterFab.visibility = if(!it.isNullOrEmpty() and (filterOptionSelected == null)) View.VISIBLE else View.GONE
            showProgressBar(false)
            showEmptyLayout(it.isNullOrEmpty())
            filterOptionSelected = null
            binding.filterByView.visibility = View.GONE
            adapter.submitList(it)
        })

        initRecyclerView(binding,manager,adapter)
        setHasOptionsMenu(true)
        setFabOnScrollBehaviour()
        return binding.root
    }

    private fun onFilterProducts(filterBinding:FilterMenuDialogBinding):Boolean{
        when(filterOptionSelected){
            filterBinding.placeSwitch -> {
                filterBy = filterBinding.placeAutoComplete.text.toString()
                if(filterBy.isNullOrEmpty())
                    return false
                viewModel.filterByPlace(filterBy!!)
                adapter.submitList(viewModel.filteredList)
            }
            filterBinding.categorySwitch -> {
                filterBy = filterBinding.categoryAutoComplete.text.toString()
                if(filterBy.isNullOrEmpty())
                    return false
                if(filterBy == resources.getString(R.string.option_uncategorized))
                    viewModel.filterByCategory("")
                else
                    viewModel.filterByCategory(filterBy)
                adapter.submitList(viewModel.filteredList)
            }
            filterBinding.dateSwitch -> {
                viewModel.filterByDate(filterBy!!)
                adapter.submitList(viewModel.filteredList)
            }
            filterBinding.priceSwitch -> {
                val values = filterBinding.priceSliderView.values
                filterBy = "$${values[0].toInt()} - $${values[1].toInt()}"
                viewModel.filterByPriceRange(values[0],values[1])
                adapter.submitList(viewModel.filteredList)
            }
        }
        return true
    }

    /*Receives a date represented as a Long value and returns the date in string format*/
    private fun formatDate(date:Long) : String{
        return DateFormat.getDateInstance().format(Date(date))
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
                        val today = formatDate(Calendar.getInstance().timeInMillis)
                        filterBinding.dateInput.setText(today)
                        filterBy = today
                        filterBinding.dateInput.visibility = View.VISIBLE
                        filterBinding.dateInput.setOnClickListener {
                            val datePicker = MaterialDatePicker.Builder.datePicker()
                                .setTitleText(getString(R.string.date_picker_title))
                                .setTheme(R.style.CustomDatePicker)
                                .build()
                            datePicker.addOnPositiveButtonClickListener {
                                filterBy = formatDate(it)
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

    private fun navigateToAddFragment(){
        Navigation.findNavController(binding.root).navigate(HomeFragmentDirections.actionHomeFragmentToAddFragment())
    }

    private fun navigateToEditFragment(productId : Long){
        Navigation.findNavController(binding.root).navigate(HomeFragmentDirections.actionHomeFragmentToEditFragment(productId))
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
        viewModel.products.observe(viewLifecycleOwner,{
            deleteItem.isEnabled = !it.isNullOrEmpty()
        })

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
                showNoResultsFoundLayout((resultList.isNullOrEmpty() and searchText.isNotEmpty() and
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
                        if(filterOptionSelected == null)
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
            R.id.op_settings -> Toast.makeText(context,"Settings", Toast.LENGTH_SHORT).show()
        }
        return true
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
            priceToDateText.text = resources.getString(R.string.current_price_string,product!!.updateDate)
            val increase = viewModel.getPriceIncrease(product!!)
            priceIncreaseTextview.text = resources.getString(R.string.price_increase_string,increase.second)
            priceIncreaseNumeric.text = resources.getString(R.string.price_increase_numeric,increase.first)
            categoryTextview.isVisible = product!!.category.isNotEmpty()
            product!!.image?.let { productDetailImg.setImageBitmap(it) }

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
                navigateToEditFragment(product!!.productId)
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
            if(!searchView.isIconified)
                collapseSearchView()
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
}