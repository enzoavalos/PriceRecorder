package com.example.pricerecorder.addFragment

import android.app.Application
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.pricerecorder.*
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.AddFragmentBinding
import com.google.android.material.textfield.TextInputEditText

class AddFragment:Fragment(){
    private lateinit var binding : AddFragmentBinding
    private lateinit var viewModel: AddFragmentViewModel


    companion object {
        const val MAX_INTEGRAL_DIGITS = 7
        const val DESCRIPTION_MAX_LENGTH = 40
        const val PLACE_MAX_LENGTH = 25
    }

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater,R.layout.add_fragment,container,false)
        val application: Application = requireNotNull(this.activity).application
        val dataSource = ProductDatabase.getInstance(application).productDatabaseDao
        val viewModelFactory = AddViewModelFactory(dataSource)

        viewModel = ViewModelProvider(this,viewModelFactory)[AddFragmentViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        MainToolbar.apply {
            val activity = activity as AppCompatActivity
            setUpButton(activity,true)
            setTitle(activity,resources.getString(R.string.add_fragment_title))
        }

        binding.acceptButton.setOnClickListener {
            if (it.id == binding.acceptButton.id)
                createNewProduct()
        }

        /* An adapter for the dropdown input editText is created with the different categories*/
        val arrayAdapter = ArrayAdapter(requireContext(),R.layout.drowpdown_item,Product.categories.sortedBy { it })
        binding.categoryAutoCompleteTextView.setAdapter(arrayAdapter)

        setHasOptionsMenu(true)
        setEditTextBehaviour(binding.descriptionEditText,binding.placeEditText,binding.priceEditText)
        return binding.root
    }

    //Creates a new Product instance and stores it in the DB
    private fun createNewProduct(){
        val desc = binding.descriptionEditText.text.toString()
        val price = binding.priceEditText.text.toString().toDouble()
        val place = binding.placeEditText.text.toString()
        val category = if(binding.categoryAutoCompleteTextView.text.isNullOrEmpty())
            null
        else
            binding.categoryAutoCompleteTextView.text.toString()

        val newProduct = Product(desc,price,place,category)
        viewModel.addProduct(newProduct)
        Toast.makeText(context,resources.getString(R.string.new_product_added),Toast.LENGTH_SHORT).show()
        navigateUp()
    }

    //Validates all edittext inputs
    private fun validateInputs(){
        binding.apply {
            if(descriptionEditText.validateTextInput() and placeEditText.validateTextInput()
                and priceEditText.validateNumericInputDouble())
                    binding.acceptButton.setAcceptButtonEnabled(true)
            else
                binding.acceptButton.setAcceptButtonEnabled(false)
        }
    }

    //Configures the edit Texts behaviour
    private fun setEditTextBehaviour(description:TextInputEditText,place:TextInputEditText,price:EditText) {
        description.apply {
            addTextChangedListener(object:TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }
                override fun afterTextChanged(s: Editable?) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    validateInputs()
                    if(s!!.length >= DESCRIPTION_MAX_LENGTH)
                        binding.placeEditText.requestFocus()
                }
            })

            filters = arrayOf(InputFilter.LengthFilter(DESCRIPTION_MAX_LENGTH))
        }

        place.apply {
            addTextChangedListener(object:TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }
                override fun afterTextChanged(s: Editable?) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    validateInputs()
                    if(s!!.length >= PLACE_MAX_LENGTH)
                        binding.priceEditText.requestFocus()
                }
            })

            filters = arrayOf(InputFilter.LengthFilter(PLACE_MAX_LENGTH))
        }

        price.apply {
            addTextChangedListener(object:TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }
                override fun afterTextChanged(s: Editable?) {
                    validateInputs()
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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> navigateUp()
        }
        return true
    }

    private fun navigateUp(){
        val action = AddFragmentDirections.actionAddFragmentToHomeFragment()
        Navigation.findNavController(binding.root).navigate(action)
    }
}