package com.example.pricerecorder.editFragment

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Space
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.DefaultTintColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.pricerecorder.*
import com.example.pricerecorder.R
import com.example.pricerecorder.addFragment.AddFragment
import com.example.pricerecorder.theme.PriceRecorderTheme

class EditFragment : Fragment() {
    private lateinit var viewModel: EditFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args : EditFragmentArgs by navArgs()
        val application: Application = requireNotNull(this.activity).application
        viewModel = EditFragmentViewModel(application,args.productId)
        viewModel.setInitialStates()

        return ComposeView(requireContext()).apply {
            setContent {
                EditProductScreen(
                    onNavigateBack = { navigateUp() }
                )
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

    private fun updateProduct(){
        viewModel.apply {
            product.updateData(
                prodDescription.value,
                prodPurchasePlace.value,
                prodCategory.value,
                prodImage.value,
                prodPrice.value.toDouble()
            )
            this.updateProduct()
        }

        Toast.makeText(context,resources.getString(R.string.product_updated_string,viewModel.product.getDescription()),
            Toast.LENGTH_SHORT).show()
        navigateUp()
    }

    private fun navigateUp(){
        findNavController().navigate(EditFragmentDirections.actionEditFragmentToHomeFragment())
    }

    @Composable
    fun EditProductScreen(onNavigateBack:() -> Unit){
        val fabEnabled = viewModel.fabEnabled

        PriceRecorderTheme {
            Scaffold(
                topBar = { ShowTopAppBar(appBarTitle = stringResource(id = R.string.edit_fragment_title), actionItems = listOf(),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "")
                        }
                    }) },
                floatingActionButton = { AddFloatingActionButton(enabled = fabEnabled.value,
                    onClick = {
                        updateProduct()
                    }) },
                floatingActionButtonPosition = FabPosition.Center
            ) {
                EditProductScreenContent(
                    Modifier
                        .padding(it))
            }
        }
    }

    @Composable
    private fun EditProductScreenContent(modifier: Modifier = Modifier){
        val image = viewModel.prodImage
        val description = viewModel.prodDescription
        val purchasePlace = viewModel.prodPurchasePlace
        val priceState = viewModel.prodPrice
        val categoryState = viewModel.prodCategory
        val sizeState = viewModel.prodSize
        val quantityState = viewModel.prodQuantity
        val showImageDialog = viewModel.showImageDialog
        val priceErrorState by viewModel.priceEditError
        val placePredictions by viewModel.placesFiltered

        ImagePickerCustomDialog(
            show = (showImageDialog.value and (image.value == null)),
            onDismiss = { viewModel.updateShowImageDialogState(false) },
            title = stringResource(id = R.string.add_image_dialog_title),
            galleryPicker = {
                PermissionChecker.checkForPermissions(requireContext(),android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    PermissionChecker.FILE_REQUEST_CODE,
                    ::pickImageFromGallery,
                    readExternalFilesPermission)
            },
            pictureTaker = {
                PermissionChecker.checkForPermissions(requireContext(),
                    android.Manifest.permission.CAMERA,
                    PermissionChecker.CAMERA_REQUEST_CODE,
                    ::takePictureFromCamera,
                    accessCameraPermission)
            })

        SelectedImageCustomDialog(
            show = showImageDialog.value,
            image = image.value,
            onDismiss = { viewModel.updateShowImageDialogState(false) },
            onDelete = {
                viewModel.updateProdImage(null)
                viewModel.updateShowImageDialogState(false)
            },
            buttonText = stringResource(id = R.string.delete_image_button_text),
            modifier = Modifier.padding(32.dp))

        Surface(modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
            color = MaterialTheme.colors.surface) {
            Column(modifier = Modifier
                .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                CurrentSelectedImage(image = image.value,
                    onClick = {
                        viewModel.updateShowImageDialogState(true)
                    })

                /*Description text field*/
                CustomTextField(value = description.value,
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp)
                        .fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.description_string),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface.copy(0.6f))
                    },
                    maxLines = 2,
                    maxAllowedChars = AddFragment.DESCRIPTION_MAX_LENGTH,
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
                AutoCompleteTextField(
                    value = purchasePlace.value,
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp)
                        .fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.place_hint),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface.copy(0.6f))
                    },
                    maxAllowedChars = AddFragment.PLACE_MAX_LENGTH,
                    onValueChange = {
                        viewModel.updateProdPurchasePlace(it)
                    },
                    leadingIcon = {
                        Icon(painter = painterResource(id = R.drawable.ic_place), contentDescription = "")
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.updateProdPurchasePlace("")
                        }) {
                            Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    predictions = placePredictions,
                    itemContent = {
                        Text(text = it,
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface.copy(0.8f),
                            modifier = Modifier.padding(2.dp))
                    }
                )

                /*Category dropdown menu*/
                ExposedDropdownMenu(
                    value = categoryState.value,
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp)
                        .fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.category_input_string),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface.copy(0.6f)) },
                    onValueChange = {
                        viewModel.updateProductCategoryState(it)
                    },
                    leadingIcon = {
                        Icon(painter = painterResource(id = R.drawable.ic_category), contentDescription = "")
                    },
                    helperText = stringResource(id = R.string.helper_text_optional),
                    options = stringArrayResource(id = R.array.categories_array)
                        .toList().sorted())

                /*Quantity text field*/
                CustomTextField(value = quantityState.value,
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp)
                        .fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.product_quantity_label),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface.copy(0.6f))
                    },
                    maxAllowedChars = AddFragment.QUANTITY_MAX_LENGTH,
                    onValueChange = {
                        viewModel.updateProductQuantityState(it)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.updateProductQuantityState("")
                        }) {
                            Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    helperText = stringResource(id = R.string.helper_text_optional),
                    showCount = false)

                /*Size/Weight text field*/
                CustomTextField(value = sizeState.value,
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp)
                        .fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.product_size_label),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface.copy(0.6f))
                    },
                    maxAllowedChars = AddFragment.SIZE_MAX_LENGTH,
                    onValueChange = {
                        viewModel.updateProductSizeState(it)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.updateProductSizeState("")
                        }) {
                            Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    helperText = stringResource(id = R.string.helper_text_optional),
                    showCount = false)

                /*price text field*/
                CustomTextField(
                    value = priceState.value,
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
                            Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "",
                                tint = if(!priceErrorState) DefaultTintColor else MaterialTheme.colors.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    isError = priceErrorState)

                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.Transparent))
            }
        }
    }
}