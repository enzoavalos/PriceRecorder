package com.example.pricerecorder.homeFragment

import android.app.Application
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.*
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
import com.example.pricerecorder.databinding.HomeFragmentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.*

class HomeFragment:Fragment() {
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: ProductAdapter
    private lateinit var binding: HomeFragmentBinding
    private lateinit var mainMenu: Menu

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
            createCustomDetailDialog(it)
        })

        //Callback created to implement swipe to delete behaviour
        val swipeDelete = object : SwipeToDeleteCallback(requireContext()) {
            //Called when a viewHolder is swiped by the user
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val swipedItem = adapter.getItemProduct(viewHolder as ProductAdapter.ProductViewHolder)
                viewModel.deleteProduct(swipedItem!!)
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
                        R.id.filter_fab -> Toast.makeText(context,"Filtrar",Toast.LENGTH_SHORT).show()
                    }
                    viewModel.onNavigated()
                }
        })

        viewModel.products.observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })

        initRecyclerView(binding,manager,adapter)
        setHasOptionsMenu(true)
        setFabOnScrollBehaviour()
        return binding.root
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
                    binding.apply {
                        addFab.shrink()
                        filterFab.hide()
                    }
                }else{
                    binding.apply {
                        addFab.extend()
                        filterFab.show()
                    }
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
            addItemDecoration(SpacingItemDecoration(4))
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
        val searchView = menuItem.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            //Invoked when the searchView text is changed
            override fun onQueryTextChange(newText: String?): Boolean {
                val tempList : MutableList<Product> = mutableListOf()
                val searchText = newText!!.lowercase(Locale.getDefault())

                //Creates a temporary list with the elements that match with the search
                if(searchText.isNotEmpty()){
                    viewModel.products.value!!.forEach {
                        if(it.description.lowercase(Locale.getDefault()).contains(searchText)){
                            tempList.add(it)
                        }
                    }
                    val resultList : List<Product> = tempList
                    adapter.submitList(resultList)
                }else{
                    adapter.submitList(viewModel.products.value)
                }
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
        val dialogBinding : DetailFragmentBinding = DetailFragmentBinding.inflate(layoutInflater)
        dialogBinding.product = p
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root).create()
        onCreateCustomDetailDialog(dialogBinding,dialog)
        dialog.apply {
            show()
            window?.setBackgroundDrawableResource(R.color.transparent)
        }
    }

    /*Sets the content of the views and certain behaviours of the detail fragment used as a custom dialog box*/
    private fun onCreateCustomDetailDialog(b:DetailFragmentBinding, detailDialog: AlertDialog){
        b.apply {
            priceToDateText.text = resources.getString(R.string.current_price_string,product!!.updateDate)
            val increase = viewModel.getPriceIncrease(product!!)
            priceIncreaseTextview.text = resources.getString(R.string.price_increase_string,increase.first)
            priceIncreaseNumeric.text = resources.getString(R.string.price_increase_numeric,increase.second)
            categoryTextview.isVisible = !product!!.category.isNullOrEmpty()

            buttonAddPrice.setOnClickListener {
                createCustomPriceDialog(detailDialog,b)
            }

            buttonDeleteProduct.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
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
                    .show()
            }

            buttonEditProduct.setOnClickListener {
                detailDialog.dismiss()
                navigateToEditFragment(product!!.productId)
            }
        }
    }

    /*Creates a custom dialog box for the users to update the price of a given product*/
    private fun createCustomPriceDialog(detailDialog: AlertDialog,detailFragmentBinding: DetailFragmentBinding){
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
            val newPrice = priceDialogBinding.addPriceEdittext.text.toString().toDouble()
            priceDialog.dismiss()
            detailFragmentBinding.product!!.updatePrice(newPrice)
            viewModel.updateProduct(detailFragmentBinding.product!!)
            detailDialog.dismiss()
            Toast.makeText(context,resources.getString(R.string.price_updated_msg),Toast.LENGTH_SHORT).show()
        }

        priceDialog.show()
        priceDialog.window?.setBackgroundDrawableResource(R.color.transparent)
    }
}