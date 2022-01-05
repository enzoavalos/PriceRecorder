package com.example.pricerecorder.editFragment

import android.app.Application
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.example.pricerecorder.MainToolbar
import com.example.pricerecorder.R
import com.example.pricerecorder.addFragment.AddFragment
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.EditFragmentBinding
import com.example.pricerecorder.setAcceptButtonEnabled
import com.example.pricerecorder.validateTextInput
import kotlinx.coroutines.*

class EditFragment : Fragment() {
    private lateinit var binding: EditFragmentBinding
    private lateinit var viewModel: EditFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        MainToolbar.apply {
            val activity = activity as AppCompatActivity
            setUpButton(activity,true)
            setTitle(activity,resources.getString(R.string.edit_fragment_title))
        }

        val args : EditFragmentArgs by navArgs()
        val application: Application = requireNotNull(this.activity).application
        val dataSource = ProductDatabase.getInstance(application).productDatabaseDao
        binding = DataBindingUtil.inflate(inflater,R.layout.edit_fragment,container,false)
        viewModel = EditFragmentViewModel(dataSource)

        /*Launches a coroutine and blocks the current thread until it is completed. It is designed to bridge regular
        * blocking code to libraries written in suspending style*/
        runBlocking {
            binding.product = viewModel.getProductById(args.productId)
        }
        setViewsContent()
        setLayoutBehaviour()

        binding.includedLayout.acceptButton.setOnClickListener {
            if(it.id == binding.includedLayout.acceptButton.id)
                updateProduct()
        }

        /* An adapter for the dropdown input editText is created with the different categories*/
        val arrayAdapter = ArrayAdapter(requireContext(),R.layout.drowpdown_item, Product.categories.sortedBy { it })
        binding.includedLayout.categoryAutoCompleteTextView.setAdapter(arrayAdapter)

        setHasOptionsMenu(true)
        return binding.root
    }

    private fun updateProduct(){
        val des = binding.includedLayout.descriptionEditText.text.toString()
        val p = binding.includedLayout.placeEditText.text.toString()
        val cat = binding.includedLayout.categoryAutoCompleteTextView.text.toString()
        binding.product!!.updateData(des,p,cat)
        viewModel.updateProduct(binding.product!!)
        Toast.makeText(context,"${binding.product!!.description} fue actualizado",Toast.LENGTH_SHORT).show()
        navigateUp()
    }

    /*Validates the inputs of each edit text*/
    private fun validateInputs(){
        binding.includedLayout.apply {
            if(descriptionEditText.validateTextInput() and placeEditText.validateTextInput()
                and (descriptionEditText.text!!.length <= AddFragment.DESCRIPTION_MAX_LENGTH)
                and (placeEditText.text!!.length <= AddFragment.PLACE_MAX_LENGTH))
                binding.includedLayout.acceptButton.setAcceptButtonEnabled(true)
            else
                binding.includedLayout.acceptButton.setAcceptButtonEnabled(false)
        }
    }

    /*Sets the behaviour of all the edit texts of the layout*/
    private fun setLayoutBehaviour(){
        binding.includedLayout.descriptionEditText.apply {
            addTextChangedListener(object : TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    validateInputs()
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })
        }

        binding.includedLayout.placeEditText.apply {
            addTextChangedListener(object : TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    validateInputs()
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })
        }

        binding.includedLayout.categoryAutoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, _, _ -> binding.includedLayout.acceptButton.setAcceptButtonEnabled(true) }
    }

    /*Sets the content of the different views of the root layout*/
    private fun setViewsContent(){
        binding.includedLayout.apply {
            priceEditText.setText(binding.product!!.price.toString())
            priceEditText.isEnabled = false
            priceEditText.isFocusable = false
            descriptionEditText.setText(binding.product!!.description)
            placeEditText.setText(binding.product!!.placeOfPurchase)
            if(!binding.product!!.category.isNullOrEmpty())
                categoryAutoCompleteTextView.setText(binding.product!!.category)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> navigateUp()
        }
        return true
    }

    private fun navigateUp(){
        Navigation.findNavController(binding.root).navigate(EditFragmentDirections.actionEditFragmentToHomeFragment())
    }
}