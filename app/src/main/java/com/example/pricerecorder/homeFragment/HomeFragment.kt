package com.example.pricerecorder.homeFragment

import android.app.Application
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pricerecorder.ProductAdapter
import com.example.pricerecorder.R
import com.example.pricerecorder.SpacingItemDecoration
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.HomeFragmentBinding

class HomeFragment:Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding : HomeFragmentBinding = DataBindingUtil.inflate(inflater,
            R.layout.home_fragment,container,false)

        val application: Application = requireNotNull(this.activity).application
        val dataSource = ProductDatabase.getInstance(application).productDatabaseDao
        val viewModelFactory = HomeViewModelFactory(dataSource,application)
        val viewModel = ViewModelProvider(this, viewModelFactory)[HomeViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val manager = LinearLayoutManager(context)
        val adapter = ProductAdapter()
        initRecyclerView(binding,manager,adapter)

        viewModel.fabClicked.observe(viewLifecycleOwner,{
                it?.let {
                    when(it){
                        R.id.add_fab -> {
                            val lista = listOf("Galletitas","Aceite","Asado","Polenta","Fideos","Atun","Verdura")
                            val aux = lista.random()
                            val p = Product(aux,200.0,"Carrefour","no","6/9/2021")
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

        return binding.root
    }

    private fun initRecyclerView(binding: HomeFragmentBinding,
                                 manager: LinearLayoutManager,
                                 adapter: ProductAdapter
    ){
        binding.productRecyclerView.apply {
            layoutManager = manager
            addItemDecoration(SpacingItemDecoration(4))
            this.adapter = adapter
        }
    }
}