package com.example.pricerecorder.addFragment

import android.app.Application
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.pricerecorder.MainToolbar
import com.example.pricerecorder.R
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.AddFragmentBinding

class AddFragment:Fragment() {
    private lateinit var binding : AddFragmentBinding
    private lateinit var viewModel: AddFragmentViewModel

    companion object {
        const val MAX_INTEGRAL_DIGITS = 7
        const val MAX_DECIMAL_DIGITS = 2
        const val DESCRIPTION_MAX_LENGTH = 50
        const val PLACE_MAX_LENGTH = 30
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
            setTitle(activity,"Nuevo producto")
        }

        viewModel.addButtonClicked.observe(viewLifecycleOwner,{
            if(it){
                createNewProduct()
            }
        })

        setHasOptionsMenu(true)
        setEditTextBehaviour()
        return binding.root
    }

    //Creates a new Product instance and stores it in the DB
    private fun createNewProduct(){
        val desc = binding.descriptionEditText.text.toString()
        val price = binding.priceEditText.text.toString().toDouble()
        val place = binding.placeEditText.text.toString()
        val newProduct = Product(desc,price,place)
        viewModel.addProduct(newProduct)
        Toast.makeText(context,"Agregado",Toast.LENGTH_SHORT).show()
        navigateUp()
        viewModel.onNavigated()
    }

    //Validates all edittext inputs
    private fun validateInputs(){
        binding.apply {
            if(!descriptionEditText.text.isNullOrEmpty() and !placeEditText.text.isNullOrEmpty()
                and !priceEditText.text.isNullOrEmpty()) {
                try{
                    priceEditText.text.toString().toDouble()
                    setAcceptButtonEnabled(true)
                }catch (e:Exception){
                    binding.priceEditText.error = "Valor Invalido"
                    setAcceptButtonEnabled(false)
                }
            }
            else
                setAcceptButtonEnabled(false)
        }
    }

    //Enable accept button when all editTexts inputs are valid
    private fun setAcceptButtonEnabled(enabled:Boolean){
        binding.acceptButton.apply {
            isEnabled = enabled
            alpha = if(enabled)
                1F
            else
                0.7F
        }
    }

    //Congifures the editTetxs behaviour
    private fun setEditTextBehaviour() {
        binding.descriptionEditText.apply {
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

        binding.placeEditText.apply {
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

        binding.priceEditText.apply {
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
                    currencyInputFormatter(this@apply)
                    addTextChangedListener(this)
                }
            })

            isLongClickable = false
            filters = arrayOf(InputFilter.LengthFilter(9))
        }
    }

    //Formats the price input
    private fun currencyInputFormatter(editText: EditText){
        var sequence:String = editText.text.toString()
        var cursorPosition = editText.selectionStart

        if(sequence.isNotEmpty()){
            if(sequence.startsWith(".")) {
                sequence = "0$sequence"
                cursorPosition += 1
            }

            var pattern = Regex("^0[0-9]")
            if(sequence.contains(pattern)) {
                sequence = sequence.dropWhile { it == '0' }
                if(sequence.isEmpty()) {
                    sequence = "0"
                    cursorPosition = 1
                }else{
                    cursorPosition = sequence.length
                }
            }

            pattern = Regex("\\...[0-9]+")
            if(sequence.contains(pattern)) {
                sequence = sequence.dropLast(1)
                cursorPosition-=1
            }

            editText.setText(sequence)
            editText.setSelection(cursorPosition)
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