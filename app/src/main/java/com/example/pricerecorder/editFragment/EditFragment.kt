package com.example.pricerecorder.editFragment

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.pricerecorder.MainToolbar
import com.example.pricerecorder.R
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.EditFragmentBinding

class EditFragment : Fragment() {
    private lateinit var binding : EditFragmentBinding
    private lateinit var viewModel : EditViewModel

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater,R.layout.edit_fragment,container,false)

        val application: Application = requireNotNull(this.activity).application
        val dataSource = ProductDatabase.getInstance(application).productDatabaseDao
        val viewModelFactory = EditViewModelFactory(dataSource)
        viewModel = ViewModelProvider(this,viewModelFactory)[EditViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        MainToolbar.apply {
            val activity = activity as AppCompatActivity
            setUpButton(activity,true)
            setTitle(activity,"Editar Producto")
        }

        binding.productDetail.acceptButton.setOnClickListener {
            Toast.makeText(context,"Editear", Toast.LENGTH_SHORT).show()
        }
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> navigateUp()
        }
        return true
    }

    private fun navigateUp(){
        val action = EditFragmentDirections.actionEditFragmentToHomeFragment()
        Navigation.findNavController(binding.root).navigate(action)
    }
}