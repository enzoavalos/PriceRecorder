package com.example.pricerecorder.addFragment

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.pricerecorder.*
import com.example.pricerecorder.R
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.theme.PriceRecorderTheme

class AddFragment:Fragment(){
    private lateinit var viewModel: AddFragmentViewModel

    companion object {
        const val MAX_INTEGRAL_DIGITS = 6
    }

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        val application: Application = requireNotNull(this.activity).application
        val viewModelFactory = AddViewModelFactory(application)
        viewModel = ViewModelProvider(this,viewModelFactory)[AddFragmentViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                AddProductScreen(onNavigateBack = { navigateUp() })
            }
        }
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
                val productImage = ImageUtils.getModifiedBitmap(requireContext(),it,uri)
                viewModel.updateProdImage(productImage)
            }
        }
    }

    private var tempImageUri : Uri? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()
    ) { success ->
        if(success){
            ImageUtils.getBitmapFromUri(requireContext(),tempImageUri!!)?.let {
                val productImage = ImageUtils.getModifiedBitmap(requireContext(),it,tempImageUri!!)
                viewModel.updateProdImage(productImage)
            }
        }
    }

    //Creates a new Product instance and stores it in the DB
    private fun createNewProduct(){
        /*TODO("terminar de agregar caracteristicas a los productos")*/
        val desc = viewModel.prodDescription.value
        val price = viewModel.prodPrice.value.toDouble()
        val place = viewModel.prodPurchasePlace.value
        val cat = getString(R.string.uncategorized_product)
        val newProduct = Product(desc,price,place,cat,DateUtils.getCurrentDate(),viewModel.prodImage.value)

        viewModel.addProduct(newProduct)
        Toast.makeText(context,resources.getString(R.string.new_product_added,desc),Toast.LENGTH_SHORT).show()
        navigateUp()
    }

    private fun navigateUp(){
        findNavController().navigate(AddFragmentDirections.actionAddFragmentToHomeFragment())
    }

    @Composable
    fun AddProductScreen(onNavigateBack:() -> Unit){
        val fabEnabled = viewModel.fabEnabled

        PriceRecorderTheme {
            Scaffold(
                topBar = { ShowTopAppBar(appBarTitle = stringResource(id = R.string.add_fragment_title), actionItems = listOf(),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "")
                        }
                    }) },
                floatingActionButton = { AddFloatingActionButton(enabled = fabEnabled.value,
                    onClick = {
                        createNewProduct()
                    }) },
                floatingActionButtonPosition = FabPosition.Center
            ) {
                AddProductScreenContent(Modifier.padding(it))
            }
        }
    }

    @Composable
    private fun AddProductScreenContent(modifier: Modifier = Modifier){
        val image = viewModel.prodImage
        val description = viewModel.prodDescription
        val purchasePlace = viewModel.prodPurchasePlace
        val priceState = viewModel.prodPrice
        val showImageDialog = viewModel.showImageDialog

        ImagePickerCustomDialog(
            show = (showImageDialog.value and (image.value == null))) {
            viewModel.updateShowImageDialogState(false)
        }

        SelectedImageCustomDialog(
            show = showImageDialog.value,
            image = image.value,
            onDismiss = { viewModel.updateShowImageDialogState(false) },
            onDelete = {
                viewModel.updateProdImage(null)
                viewModel.updateShowImageDialogState(false)
            },
            modifier = Modifier.padding(24.dp))

        Surface(modifier = modifier
            .fillMaxSize(),
            color = MaterialTheme.colors.surface) {
            Column(modifier = Modifier
                .fillMaxSize()
                .scrollable(rememberScrollState(), orientation = Orientation.Vertical),
                horizontalAlignment = Alignment.CenterHorizontally) {
                CurrentSelectedImage(image = image.value,
                    onClick = {
                        viewModel.updateShowImageDialogState(true)
                    })

                /*Description text field*/
                RegularTextField(value = description.value,
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp)
                        .fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.description_string),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface.copy(0.6f))
                    },
                    maxLines = 2,
                    maxAllowedChars = 60,
                    onValueChange = {
                        viewModel.updateProdDescription(it)
                    },
                    leadingIcon = {
                        Icon(painter = painterResource(id = R.drawable.ic_description), contentDescription = "")
                    },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.updateProdDescription("") }) {
                            Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ))

                /*place of purchase text field*/
                RegularTextField(value = purchasePlace.value,
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp)
                        .fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.place_hint),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface.copy(0.6f))
                    },
                    maxLines = 1,
                    maxAllowedChars = 40,
                    onValueChange = {
                        viewModel.updateProdPurchasePlace(it)
                    },
                    leadingIcon = {
                        Icon(painter = painterResource(id = R.drawable.ic_place), contentDescription = "")
                    },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.updateProdPurchasePlace("") }) {
                            Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ))

                /*TODO("agregar opciones de auto completado a textfield de lugar de compra")*/
                /*TODO("agregar textfield de categorias con auto complete")*/

                /*price text field*/
                RegularTextField(value = priceState.value,
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp)
                        .width(200.dp),
                    label = {
                        Text(text = stringResource(id = R.string.price_title),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface.copy(0.6f))
                    },
                    maxLines = 1,
                    onValueChange = {
                        viewModel.updateProductPriceState(it)
                    },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.updateProductPriceState("") }) {
                            Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ))
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun SelectedImageCustomDialog(show: Boolean,
                                          image:Bitmap?,
                                          onDismiss: () -> Unit,
                                          onDelete:() -> Unit,
                                          modifier: Modifier = Modifier){
        if(!show or (image == null))
            return
        Dialog(onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(modifier = modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Image(bitmap = image!!.asImageBitmap(),
                    contentDescription = "",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop)

                Button(onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Red
                    )) {
                    Text(text = stringResource(id = R.string.delete_image_button_text),
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onError)
                }
            }
        }
    }

    /*Creates a dialog that gives the user the option to select an image from the gallery or take a picture in case it has
    * not already done it, in this case a dialog is shown with the current image where the user can delete it if wanted*/
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun ImagePickerCustomDialog(show:Boolean,
                                        onDismiss:() -> Unit){
        CustomAlertDialog(show = show,
            title = stringResource(id = R.string.add_image_dialog_title),
            msg = {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Surface(modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Start),
                        onClick = {
                            onDismiss()
                            PermissionChecker.checkForPermissions(requireContext(),android.Manifest.permission.CAMERA,
                                PermissionChecker.CAMERA_REQUEST_CODE,
                                ::takePictureFromCamera,
                                accessCameraPermission)
                        }) {
                            Text(text = stringResource(id = R.string.add_img_dialog_take_picture),
                                style = MaterialTheme.typography.subtitle1,
                                color = MaterialTheme.colors.primaryVariant)
                    }
                    Surface(modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Start),
                        onClick = {
                            onDismiss()
                            PermissionChecker.checkForPermissions(requireContext(),android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                PermissionChecker.FILE_REQUEST_CODE,
                                ::pickImageFromGallery,
                                readExternalFilesPermission)
                        }) {
                        Text(text = stringResource(id = R.string.add_img_dialog_pick_from_gallery),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.primaryVariant)
                    }
                }
            },
            confirmButtonText = stringResource(id = R.string.cancel_button_string),
            dismissButtonText = null,
            onConfirm = onDismiss,
            onDismiss = onDismiss)
    }

    //@Preview(heightDp = 800, widthDp = 360)
    @Composable
    fun AddFragmentPreview(){
        PriceRecorderTheme {
            AddProductScreen(onNavigateBack = {})
        }
    }
}