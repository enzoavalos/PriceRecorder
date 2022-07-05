package com.example.pricerecorder.addFragment

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.pricerecorder.*
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.AddFragmentBinding
import com.example.pricerecorder.databinding.DialogProductImageBigBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class AddFragment:Fragment(){
    private lateinit var binding : AddFragmentBinding
    private lateinit var viewModel: AddFragmentViewModel
    private  var productImage : Bitmap? = null

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

        /*Creates a dialog that gives the user th option to select an image from the gallery or take a picture in case it has
        * not already done it, in this case a dialog is shown with the current image where the user can delete it if wanted*/
        binding.addProductImage.setOnClickListener {
            val items = resources.getStringArray(R.array.add_image_dialog_items)
            if(productImage == null){
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(resources.getString(R.string.add_image_dialog_title))
                    .setItems(items
                    ) { dialog, which ->
                        when(which){
                            0 -> PermissionChecker.checkForPermissions(requireContext(),android.Manifest.permission.CAMERA,
                                PermissionChecker.CAMERA_REQUEST_CODE,
                                ::takePictureFromCamera,
                                accessCameraPermission)
                            1 -> PermissionChecker.checkForPermissions(requireContext(),android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                PermissionChecker.FILE_REQUEST_CODE,
                                ::pickImageFromGallery,
                                readExternalFilesPermission)
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(resources.getString(R.string.cancel_button_string)) { dialog, _ -> dialog.dismiss() }
                    .show()
            }else{
                val dialogBinding = DialogProductImageBigBinding.inflate(layoutInflater)
                dialogBinding.dialogImageView.setImageBitmap(productImage)
                val dialog = AlertDialog.Builder(requireContext())
                    .setView(dialogBinding.root).create()
                dialogBinding.buttonDeleteImage.setOnClickListener {
                    productImage = null
                    binding.addProductImage.setImageResource(R.drawable.ic_add_photo_alternate)
                    Toast.makeText(context,getString(R.string.image_deleted_succes),Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                dialog.show()
            }
        }

        /* An adapter for the dropdown input editText is created with the different categories*/
        val arrayAdapter = ArrayAdapter(requireContext(),R.layout.drowpdown_item,
            resources.getStringArray(R.array.product_categories).sortedBy { it })
        binding.categoryAutoCompleteTextView.setAdapter(arrayAdapter)

        setHasOptionsMenu(true)
        setEditTextBehaviour(binding.descriptionEditText,binding.placeEditText,binding.priceEditText)
        return binding.root
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
        tempImageUri = FileProvider.getUriForFile(requireContext(),
            "com.example.pricerecorder.provider",ImageUtils.createTemporaryImageFile(requireContext()))
        try {
            cameraLauncher.launch(tempImageUri)
        }catch (e:Exception){
            Toast.makeText(context,resources.getString(R.string.camera_access_error),Toast.LENGTH_SHORT).show()
        }
    }

    /*Register a contract that returns a special launcher used to start an activity for result, designated by the given
    contract, in this case to select an image from the gallery or take a picture from the systems camera*/
    private val selectPictureLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri != null){
            ImageUtils.getBitmapFromUri(requireContext(),uri)?.let {
                productImage = ImageUtils.getModifiedBitmap(requireContext(),it,uri)
                binding.addProductImage.setImageBitmap(productImage)
            }
        }
    }

    private var tempImageUri : Uri? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()
    ) { success ->
        if(success){
            ImageUtils.getBitmapFromUri(requireContext(),tempImageUri!!)?.let {
                productImage = ImageUtils.getModifiedBitmap(requireContext(),it,tempImageUri!!)
                binding.addProductImage.setImageBitmap(productImage)
            }
        }
    }

    //Creates a new Product instance and stores it in the DB
    private fun createNewProduct(){
        val desc = binding.descriptionEditText.text.toString()
        val price = binding.priceEditText.text.toString().toDouble()
        val place = binding.placeEditText.text.toString()
        val category = binding.categoryAutoCompleteTextView.text.toString()

        val newProduct = Product(desc,price,place,category,DateUtils.getCurrentDate(),productImage)
        productImage = null
        viewModel.addProduct(newProduct)
        Toast.makeText(context,resources.getString(R.string.new_product_added,desc),Toast.LENGTH_SHORT).show()
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
            filters = arrayOf(InputFilter.LengthFilter(8))
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

    companion object {
        const val MAX_INTEGRAL_DIGITS = 6
        const val DESCRIPTION_MAX_LENGTH = 40
        const val PLACE_MAX_LENGTH = 25
    }
}