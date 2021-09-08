package com.example.pricerecorder.homeFragment

import android.app.Application
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pricerecorder.ProductAdapter
import com.example.pricerecorder.ProductListener
import com.example.pricerecorder.R
import com.example.pricerecorder.SpacingItemDecoration
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.HomeFragmentBinding
import java.util.*

class HomeFragment:Fragment() {
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: ProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding : HomeFragmentBinding = DataBindingUtil.inflate(inflater,
            R.layout.home_fragment,container,false)

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
        initRecyclerView(binding,manager,adapter)

        viewModel.fabClicked.observe(viewLifecycleOwner,{
                it?.let {
                    when(it){
                        R.id.add_fab -> {
                            val lista = listOf("Galletitas","Aceite","Asado","Polenta","Fideos","Atun","Verdura","Gaseosa","Pollo")
                            val aux = lista.random()
                            val p = Product(aux,200.0,"Carrefour","no")
                            viewModel.addProduct(p)
                        }
                        R.id.filter_fab -> Toast.makeText(context,"Filtrar",Toast.LENGTH_SHORT).show()
                    }
                    viewModel.onNavigated()
                }
        })

        viewModel.products.observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })

        setHasOptionsMenu(true)
        return binding.root
    }

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

            override fun onQueryTextChange(newText: String?): Boolean {
                val tempList : MutableList<Product> = mutableListOf()
                val searchText = newText!!.lowercase(Locale.getDefault())

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

        /*Se ejecuta al cerrarse la barra de busqueda*/
        searchView.setOnCloseListener {
            adapter.submitList(viewModel.products.value)
            true
        }
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