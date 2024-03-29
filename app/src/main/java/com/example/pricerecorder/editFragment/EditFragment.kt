package com.example.pricerecorder.editFragment

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
        imageHandler = ImageUtils(requireContext(),
            requireActivity().activityResultRegistry){
            viewModel.updateProdImage(it)
            }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            imageHandler.setTempUri(savedInstanceState?.getParcelable("file_uri",Uri::class.java))
        else
            @Suppress("Deprecation")
            imageHandler.setTempUri(savedInstanceState?.get("file_uri") as Uri?)
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

    private fun updateProduct(showSnackBar:(String)->Unit){
        viewModel.apply {
            if(productAlreadyRegistered(prodDescription.value,prodPurchasePlace.value,product.getId())){
                showSnackBar(getString(R.string.product_already_exists_msg))
                return
            }

            product.updateData(
                prodDescription.value,
                prodPurchasePlace.value,
                cat = if(prodCategory.value.isNullOrEmpty()) null else prodCategory.value,
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
        navigateUp(actionDone = true)
    }

    private fun navigateUp(actionDone:Boolean = false){
        findNavController().navigate(EditFragmentDirections.actionEditFragmentToHomeFragment().setActionDone(actionDone))
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun EditProductScreen(onNavigateBack:() -> Unit){
        val fabEnabled = viewModel.fabEnabled
        val modalBottomSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            confirmStateChange = { it == ModalBottomSheetValue.Expanded || it == ModalBottomSheetValue.Hidden })
        val coroutineScope = rememberCoroutineScope()
        var multiFabState by remember {
            mutableStateOf(MultiFloatingButtonState.Collapsed)
        }
        /*Creates a dialog that provides the option to delete the current selected product*/
        var showDeleteProductDialog by remember {
            mutableStateOf(false)
        }

        PriceRecorderTheme(
            context = requireContext()
        ) {
            DeleteProductDialog(
                show = showDeleteProductDialog,
                onConfirm = {
                    showDeleteProductDialog = false
                    viewModel.deleteProduct()
                    Toast.makeText(requireContext(),getString(R.string.delete_success_msg,viewModel.product.getDescription()),
                        Toast.LENGTH_SHORT).show()
                    navigateUp(actionDone = true)
                }, onDismiss = {
                    showDeleteProductDialog = false
                })

            ModalBottomSheetLayout(
                sheetState = modalBottomSheetState,
                sheetContent = {
                    ImagePickerBottomSheetContent(onDismiss = {
                        coroutineScope.launch {
                            modalBottomSheetState.hide()
                        }
                    },
                        title = stringResource(id = R.string.add_image_dialog_title),
                        galleryPicker = {
                            permissionChecker.checkForPermissions(
                                PermissionChecker.READ_EXTERNAL_FILES_PERMISSION,
                                PermissionChecker.FILE_REQUEST_CODE
                            ) { imageHandler.pickImageFromGallery() } },
                        pictureTaker = {
                            permissionChecker.checkForPermissions(
                                PermissionChecker.CAMERA_ACCESS_PERMISSION,
                                PermissionChecker.CAMERA_REQUEST_CODE
                            ) { imageHandler.takePictureFromCamera() }
                        },
                        isDarkThemeEnabled = ThemeUtils.systemInDarkTheme(requireContext()))
                },
                sheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)){
                val scaffoldState = rememberScaffoldState()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { ShowTopAppBar(appBarTitle = stringResource(id = R.string.edit_fragment_title), actionItems = listOf(),
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "")
                            }
                        }) },
                    floatingActionButton = {
                        val items = listOf(
                            FabItem(
                                icon = Icons.Filled.Save,
                                label = getString(R.string.save_changes_label),
                                enabled = fabEnabled.value,
                                onClick = {
                                    updateProduct(
                                        showSnackBar = { msg ->
                                            coroutineScope.launch {
                                                scaffoldState.snackbarHostState.showSnackbar(message = msg)
                                            }
                                        }
                                    )
                                }
                            ),
                            FabItem(
                                icon = Icons.Filled.Delete,
                                label = getString(R.string.delete_product_label),
                                onClick = {
                                    showDeleteProductDialog = true
                                }
                            )
                        )

                        MultiFloatingButton(items = items,
                            multiFabState = multiFabState,
                            onMultiFabStateChange = {
                                multiFabState = it
                            })},
                    floatingActionButtonPosition = FabPosition.End,
                ) {
                    BackHandler(enabled = modalBottomSheetState.isVisible) {
                        coroutineScope.launch {
                            modalBottomSheetState.hide()
                        }
                    }

                    EditProductScreenContent(
                        onExpandBottomSheet = {
                            coroutineScope.launch { modalBottomSheetState.show() }
                        },
                        Modifier
                            .padding(it))
                }
            }
        }
    }

    @Composable
    private fun DeleteProductDialog(
        show:Boolean,
        onConfirm:() -> Unit,
        onDismiss:() -> Unit){
        CustomAlertDialog(
            show = show,
            title = stringResource(id = R.string.delete_product_title),
            msg = {
                Text(text = stringResource(id = R.string.delete_product_dialog_msg),
                    color = MaterialTheme.colors.onSurface)
            },
            confirmButtonText = stringResource(id = R.string.accept_button_string),
            dismissButtonText = stringResource(id = R.string.cancel_button_string),
            onConfirm = onConfirm,
            onDismiss = onDismiss)
    }

    @Composable
    private fun EditProductScreenContent(
        onExpandBottomSheet:() -> Unit,
        modifier: Modifier = Modifier){
        val image = viewModel.prodImage
        val description = viewModel.prodDescription
        val purchasePlace = viewModel.prodPurchasePlace
        val priceState = viewModel.prodPrice
        val categoryState = viewModel.prodCategory
        val sizeState = viewModel.prodSize
        val quantityState = viewModel.prodQuantity
        var showImageDialog by remember {
            mutableStateOf(false)
        }
        val priceErrorState by viewModel.priceEditError
        val placePredictions by viewModel.placesFiltered
        val barcodeState by viewModel.barCode

        SelectedImageCustomDialog(
            show = showImageDialog,
            image = image.value,
            onDismiss = { showImageDialog = false },
            onDelete = {
                viewModel.updateProdImage(null)
                showImageDialog = false
            },
            modifier = Modifier.padding(32.dp),
            orientation = resources.configuration.orientation)

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
                        if(image.value == null)
                            onExpandBottomSheet()
                        else
                            showImageDialog = true
                    })

                /*Description text field*/
                CustomTextField(value = description.value,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.description_string),
                            style = MaterialTheme.typography.subtitle1)
                    },
                    maxLines = 2,
                    maxAllowedChars = AddFragment.DESCRIPTION_MAX_LENGTH,
                    onValueChange = {
                        viewModel.updateProdDescription(it)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = "")
                    },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.updateProdDescription("") }) {
                            Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    darkThemeEnabled = ThemeUtils.systemInDarkTheme(requireContext()))

                /*place of purchase text field*/
                AutoCompleteTextField(
                    value = purchasePlace.value,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.place_hint),
                            style = MaterialTheme.typography.subtitle1)
                    },
                    maxAllowedChars = AddFragment.PLACE_MAX_LENGTH,
                    onValueChange = {
                        viewModel.updateProdPurchasePlace(it)
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Place, contentDescription = "")
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
                    },
                    darkThemeEnabled = ThemeUtils.systemInDarkTheme(requireContext())
                )

                /*Category dropdown menu*/
                ExposedDropdownMenu(
                    value = categoryState.value ?: "",
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.category_label),
                            style = MaterialTheme.typography.subtitle1) },
                    onValueChange = {
                        viewModel.updateProductCategoryState(it)
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Category, contentDescription = "")
                    },
                    helperText = stringResource(id = R.string.helper_text_optional),
                    options = stringArrayResource(id = R.array.categories_array)
                        .toList().sorted(),
                    darkThemeEnabled = ThemeUtils.systemInDarkTheme(requireContext()))

                /*Quantity text field*/
                CustomTextField(value = quantityState.value,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.product_quantity_label),
                            style = MaterialTheme.typography.subtitle1)
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
                    showCount = false,
                    darkThemeEnabled = ThemeUtils.systemInDarkTheme(requireContext()))

                /*Size/Weight text field*/
                CustomTextField(value = sizeState.value,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.product_size_label),
                            style = MaterialTheme.typography.subtitle1)
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
                    showCount = false,
                    darkThemeEnabled = ThemeUtils.systemInDarkTheme(requireContext()))

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
                    helperText = stringResource(id = R.string.optional_helper_text),
                    darkThemeEnabled = ThemeUtils.systemInDarkTheme(requireContext()))

                /*price text field*/
                CustomTextField(
                    value = priceState.value,
                    modifier = Modifier
                        .padding(bottom = 8.dp, top = 4.dp)
                        .width(200.dp),
                    label = {
                        Text(text = stringResource(id = R.string.price_label),
                            style = MaterialTheme.typography.subtitle1)
                    },
                    maxLines = 1,
                    onValueChange = {
                        viewModel.updateProductPriceState(it)
                    },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.updateProductPriceState("") }) {
                            Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "",
                                tint = if(!priceErrorState) LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                                else MaterialTheme.colors.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    isError = priceErrorState,
                    visualTransformation = PrefixVisualTransformation("$ ",
                        MaterialTheme.colors.onSurface.copy(0.7f)),
                    darkThemeEnabled = ThemeUtils.systemInDarkTheme(requireContext()))

                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.Transparent))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("file_uri",imageHandler.getTempUri())
    }
}