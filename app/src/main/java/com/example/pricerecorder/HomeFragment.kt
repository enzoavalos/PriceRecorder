package com.example.pricerecorder

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.pricerecorder.databinding.HomeFragmentBinding

class HomeFragment:Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding : HomeFragmentBinding = DataBindingUtil.inflate(inflater,R.layout.home_fragment,container,false)
        val viewModel = ViewModelProvider(this)[(HomeViewModel::class.java)]
        val manager = GridLayoutManager(activity,2)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this
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