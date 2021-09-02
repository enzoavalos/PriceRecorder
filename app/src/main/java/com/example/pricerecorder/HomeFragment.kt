package com.example.pricerecorder

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.database.ProductDatabaseDao
import com.example.pricerecorder.databinding.HomeFragmentBinding

class HomeFragment:Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding : HomeFragmentBinding = DataBindingUtil.inflate(inflater,R.layout.home_fragment,container,false)

        val application: Application = requireNotNull(this.activity).application
        val dataSource = ProductDatabase.getInstance(application).productDatabaseDao
        val viewModelFactory = HomeViewModelFactory(dataSource,application)
        val viewModel = ViewModelProvider(this, viewModelFactory)[HomeViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        val manager = LinearLayoutManager(context)
        binding.productsList.layoutManager = manager

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
}