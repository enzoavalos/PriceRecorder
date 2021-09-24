package com.example.pricerecorder.homeFragment

import android.app.Application
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pricerecorder.*
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.HomeFragmentBinding
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

        val manager = LinearLayoutManager(context)
        adapter = ProductAdapter(ProductListener {
            viewModel.deleteProduct(it)
        })

        viewModel.fabClicked.observe(viewLifecycleOwner,{
                it?.let {
                    when(it){
                        R.id.add_fab -> Navigation.findNavController(binding.root).navigate(R.id.action_homeFragment_to_addFragment)
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

    /*Configura comportamiento de los botones flotantes al ser scrolleada la pantalla*/
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

        //Sets the functionality for the searchview in the toolbar
        val menuItem = menu.findItem(R.id.op_search)
        val searchView = menuItem.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                Toast.makeText(context,"Buscar $query",Toast.LENGTH_SHORT).show()
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
            //Invoked when the searchview is attached to the screen(search bar is opened)
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
                viewModel.clear()
                Toast.makeText(context, "Elementos eliminados correctamente", Toast.LENGTH_SHORT).show()
            }
            R.id.op_settings -> Toast.makeText(context,"Settings", Toast.LENGTH_SHORT).show()
        }
        return true
    }
}