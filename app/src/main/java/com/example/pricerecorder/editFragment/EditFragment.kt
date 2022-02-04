package com.example.pricerecorder.editFragment

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.example.pricerecorder.*
import com.example.pricerecorder.addFragment.AddFragment
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.EditFragmentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*

class EditFragment : Fragment() {
    private lateinit var binding: EditFragmentBinding
    private lateinit var viewModel: EditFragmentViewModel
    private  var productImage : Bitmap? = null
    private var modified = MutableLiveData(false)

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
        productImage = binding.product!!.image

        binding.includedLayout.acceptButton.setOnClickListener {
            if(it.id == binding.includedLayout.acceptButton.id)
                updateProduct()
        }

        modified.observe(viewLifecycleOwner,{
            if(it)
                validateInputs()
        })

        /*Creates a dialog that gives the user th option to select an image from the gallery or take a picture*/
        binding.includedLayout.addProductImage.setOnClickListener {
            val items = resources.getStringArray(R.array.add_image_dialog_items)
            if(binding.product!!.image == null){
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(resources.getString(R.string.add_image_dialog_title))
                    .setItems(items
                    ) { dialog, which ->
                        when(which){
                            0 -> PermissionChecker.checkForPermissions(requireContext(),android.Manifest.permission.CAMERA,
                                ImageUtils.CAMERA_REQUEST_CODE,
                                ::pickImageFromGallery,::takePictureFromCamera,
                                listOf(readExternalFilesPermission,accessCameraPermission))
                            1 -> PermissionChecker.checkForPermissions(requireContext(),android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                ImageUtils.FILE_REQUEST_CODE,
                                ::pickImageFromGallery,::takePictureFromCamera,
                                listOf(readExternalFilesPermission,accessCameraPermission))
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(resources.getString(R.string.cancel_button_string)) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }

        /* An adapter for the dropdown input editText is created with the different categories*/
        val arrayAdapter = ArrayAdapter(requireContext(),R.layout.drowpdown_item,
            resources.getStringArray(R.array.product_categories).sortedBy { it })
        binding.includedLayout.categoryAutoCompleteTextView.setAdapter(arrayAdapter)

        setHasOptionsMenu(true)
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
                binding.includedLayout.addProductImage.setImageBitmap(productImage)
                if(binding.product!!.image != productImage)
                    modified.value = true
            }
        }
    }

    private var tempImageUri : Uri? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()
    ) { success ->
        if(success){
            ImageUtils.getBitmapFromUri(requireContext(),tempImageUri!!)?.let {
                productImage = ImageUtils.getModifiedBitmap(requireContext(),it,tempImageUri!!)
                binding.includedLayout.addProductImage.setImageBitmap(productImage)
                if(binding.product!!.image != productImage)
                    modified.value = true
            }
        }
    }

    private fun updateProduct(){
        val des = binding.includedLayout.descriptionEditText.text.toString()
        val p = binding.includedLayout.placeEditText.text.toString()
        val cat = binding.includedLayout.categoryAutoCompleteTextView.text.toString()
        binding.product!!.updateData(des,p,cat,productImage)
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

        binding.includedLayout.categoryAutoCompleteTextView.let {
            it.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                if(it.adapter.getItem(position) != binding.product!!.category)
                    modified.value = true
            }
        }
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
            binding.product!!.image?.let {
                binding.includedLayout.addProductImage.setImageBitmap(it)
            }
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