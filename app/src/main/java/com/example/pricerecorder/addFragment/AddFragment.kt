package com.example.pricerecorder.addFragment

import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.pricerecorder.*
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.AddFragmentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class AddFragment:Fragment(){
    private lateinit var binding : AddFragmentBinding
    private lateinit var viewModel: AddFragmentViewModel


    companion object {
        const val MAX_INTEGRAL_DIGITS = 5
        const val DESCRIPTION_MAX_LENGTH = 40
        const val PLACE_MAX_LENGTH = 25
        const val FILE_REQUEST_CODE = 101
        const val CAMERA_REQUEST_CODE = 102
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

        /*Creates a dialog that gives the user th option to select an image from the gallery or take a picture*/
        binding.addProductImage.setOnClickListener {
            val items = arrayOf("Tomar imagen desde camara","Elegir de galeria")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Agregar foto")
                .setItems(items
                ) { dialog, which ->
                    when(which){
                        0 -> checkForPermissions(android.Manifest.permission.CAMERA,CAMERA_REQUEST_CODE)
                        1 -> checkForPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            FILE_REQUEST_CODE)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        /* An adapter for the dropdown input editText is created with the different categories*/
        val arrayAdapter = ArrayAdapter(requireContext(),R.layout.drowpdown_item,Product.categories.sortedBy { it })
        binding.categoryAutoCompleteTextView.setAdapter(arrayAdapter)

        setHasOptionsMenu(true)
        setEditTextBehaviour(binding.descriptionEditText,binding.placeEditText,binding.priceEditText)
        return binding.root
    }

    /*Check if permission has been granted to either access external files or the camera, and if its not
    * then it requests for it*/
    private fun checkForPermissions(permission:String,requestCode:Int){
        fun launchActivityWithPermission(requestCode:Int){
            when(requestCode){
                FILE_REQUEST_CODE -> pickImageFromGallery()
                CAMERA_REQUEST_CODE -> takePictureFromCamera()
            }
        }

        /*Checks if the sdk version is 23 or above*/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            when(ContextCompat.checkSelfPermission(requireContext(),permission)){
                PackageManager.PERMISSION_GRANTED -> { launchActivityWithPermission(requestCode) }
                else -> {
                    /*Request for the user permission to access certain documents and features of the device*/
                    when(requestCode){
                        FILE_REQUEST_CODE -> readExternalFilesPermission.launch(permission)
                        CAMERA_REQUEST_CODE -> accessCameraPermission.launch(permission)
                    }
                }
            }
        }else{ launchActivityWithPermission(requestCode) }
    }

    /*Handles the return value of their specific request made to the user*/
    private val readExternalFilesPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        if(it){ pickImageFromGallery() }
    }

    private val accessCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        if(it){ takePictureFromCamera() }
    }

    /*Launches the activities registered below in order to get a result from them*/
    private fun pickImageFromGallery(){
        selectPictureLauncher.launch("image/*")
    }

    private fun takePictureFromCamera(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            cameraLauncher.launch(intent)
        }catch (e:Exception){
            Toast.makeText(context,"Imposible acceder a la camara",Toast.LENGTH_SHORT).show()
        }
    }

    /*Register a contract that returns a special launcher used to start an activity for result, designated by the given
    contract, in this case to select an image from the gallery or take a picture from the systems camera*/
    private val selectPictureLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if(it != null)
            binding.addProductImage.setImageURI(it)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()
    ) {
        if ((it.resultCode == RESULT_OK) and (it.data != null)) {
            val bundle = it.data!!.extras
            val bitmap = bundle!!["data"] as Bitmap
            binding.addProductImage.setImageBitmap(bitmap)
        }
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

        val newProduct = Product(desc,price,place,category,Product.setUpdateDate())
        viewModel.addProduct(newProduct)
        Toast.makeText(context,resources.getString(R.string.new_product_added),Toast.LENGTH_SHORT).show()
        navigateUp()
    }

    //Validates all edittext inputs
    private fun validateInputs(){
        binding.apply {
            if(descriptionEditText.validateTextInput() and placeEditText.validateTextInput()
                and priceEditText.validatePositiveNumericInputDouble())
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
            filters = arrayOf(InputFilter.LengthFilter(7))
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