package com.example.pricerecorder

import android.app.Application
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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
        var p1 = Product("Aceite",200.0,"Carrefour","no","2021")
        var p2 = Product("Fernet",1000.0,"Monarca","no","1982")
        var p3 = Product("Asado",1000.0,"Monarca","no","1982")
        var p4 = Product("Carbon",1000.0,"Monarca","no","1982")
        var p5 = Product("Milanesas",1000.0,"Monarca","no","1982")
        var p6 = Product("Yogur",1000.0,"Monarca","no","1982")
        var p7 = Product("Masitas",1000.0,"Monarca","no","1982")
        var p8 = Product("Harina 0000",1000.0,"Monarca","no","1982")
        var lista : List<Product> = listOf(p1,p2,p3,p4,p5,p6,p7,p8)

        initRecyclerView(binding,lista)

        viewModel.fabClicked.observe(viewLifecycleOwner,{
                it?.let {
                    when(it){
                        R.id.add_fab -> Toast.makeText(context,"Agregar",Toast.LENGTH_SHORT).show()
                        R.id.filter_fab -> Toast.makeText(context,"Filtrar",Toast.LENGTH_SHORT).show()
                    }
                    viewModel.onNavigated()
                }
        })

        return binding.root
    }

    private fun initRecyclerView(binding: HomeFragmentBinding, lista: List<Product>){
        binding.productRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SpacingItemDecoration(8))
            val prodAdapter = ProductAdapter()
            prodAdapter.submitList(lista)
            adapter = prodAdapter
        }
    }
}