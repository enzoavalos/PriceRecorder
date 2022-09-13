package com.example.pricerecorder.editFragment

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.DefaultTintColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.pricerecorder.*
import com.example.pricerecorder.R
import com.example.pricerecorder.addFragment.AddFragment
import com.example.pricerecorder.theme.PriceRecorderTheme
import kotlinx.coroutines.launch

class EditFragment : Fragment() {
    private lateinit var imageHandler : ImageUtils
    private lateinit var permissionChecker : PermissionChecker
    private lateinit var barcodeScanner: BarcodeScanner

    private val args : EditFragmentArgs by navArgs()
    private val viewModel: EditFragmentViewModel by viewModels(
        factoryProducer = {
            EditViewModelFactory(args.productId)
        })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        imageHandler = ImageUtils(requireContext(),requireActivity().activityResultRegistry)
        permissionChecker = PermissionChecker(requireContext(),requireActivity().activityResultRegistry)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                EditProductScreen(
                    onNavigateBack = { navigateUp() }
                )
            }
        }
    }

    private fun updateProduct(showSnackbar:(String)->Unit){
        viewModel.apply {
            if(productAlreadyRegistered(prodDescription.value,prodPurchasePlace.value,product.getId())){
                showSnackbar(getString(R.string.product_already_exists_msg))
                return
            }

            product.updateData(
                prodDescription.value,
                prodPurchasePlace.value,
                prodCategory.value,
                prodImage.value,
                prodPrice.value.toDouble(),
                prodSize.value,
                prodQuantity.value,
                barCode.value
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
        val scaffoldState = rememberScaffoldState()
        val coroutineScope = rememberCoroutineScope()

        PriceRecorderTheme {
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = { ShowTopAppBar(appBarTitle = stringResource(id = R.string.edit_fragment_title), actionItems = listOf(),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "")
                        }
                    }) },
                floatingActionButton = { AddFloatingActionButton(enabled = fabEnabled.value,
                    onClick = {
                        updateProduct(
                            showSnackbar = { msg ->
                                coroutineScope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar(message = msg)
                                }
                            }
                        )
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
        val barcodeState by viewModel.barCode

        ImagePickerCustomDialog(
            show = (showImageDialog.value and (image.value == null)),
            onDismiss = { viewModel.updateShowImageDialogState(false) },
            title = stringResource(id = R.string.add_image_dialog_title),
            galleryPicker = {
                permissionChecker.checkForPermissions(
                    PermissionChecker.READ_EXTERNAL_FILES_PERMISSION,
                    PermissionChecker.FILE_REQUEST_CODE
                ) { imageHandler.pickImageFromGallery { viewModel.updateProdImage(it) } }
            },
            pictureTaker = {
                permissionChecker.checkForPermissions(
                    PermissionChecker.CAMERA_ACCESS_PERMISSION,
                    PermissionChecker.CAMERA_REQUEST_CODE
                ) { imageHandler.takePictureFromCamera { viewModel.updateProdImage(it) } }
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
                .padding(start = 24.dp, end = 24.dp)
                .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                CurrentSelectedImage(image = image.value,
                    onClick = {
                        viewModel.updateShowImageDialogState(true)
                    })

                /*Description text field*/
                CustomTextField(value = description.value,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
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
                        .padding(bottom = 8.dp)
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
                            style = MaterialTheme.typography.subtitle1.copy(fontSize = 18.sp),
                            color = MaterialTheme.colors.onSurface.copy(0.8f),
                            modifier = Modifier.padding(2.dp))
                    }
                )

                /*Category dropdown menu*/
                ExposedDropdownMenu(
                    value = categoryState.value,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
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
                        .padding(bottom = 8.dp)
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
                        .padding(bottom = 8.dp)
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

                BarcodeScanSection(
                    barcodeState = barcodeState,
                    onValueChange = { viewModel.updateBarCodeState(it) },
                    onCancelClicked = { viewModel.updateBarCodeState("") },
                    onScanCodeClicked = {
                        barcodeScanner = BarcodeScanner(
                            requireContext(),
                            requireActivity().activityResultRegistry
                        )
                        barcodeScanner.scanCode(permissionChecker) {
                            viewModel.updateBarCodeState(it)
                        }
                    },
                    maxAllowedChars = AddFragment.BARCODE_MAX_LENGTH,
                    helperText = stringResource(id = R.string.optional_helper_text))

                /*price text field*/
                CustomTextField(
                    value = priceState.value,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
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