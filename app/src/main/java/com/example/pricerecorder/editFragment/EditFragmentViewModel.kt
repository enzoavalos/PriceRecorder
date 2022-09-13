package com.example.pricerecorder.editFragment

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricerecorder.CurrencyFormatter
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class EditFragmentViewModel(application: Application,productId: Long) : ViewModel() {
    private val repository = ProductsRepository.getInstance(application)
    lateinit var product : Product

    init {
        getProductById(productId)
    }

    private var _fabEnabled : MutableState<Boolean> = mutableStateOf(false)
    var fabEnabled : State<Boolean> = _fabEnabled
    private var _showImageDialog = mutableStateOf(false)
    var showImageDialog : State<Boolean> = _showImageDialog
    private var _productModified : MutableState<Boolean> = mutableStateOf(false)

    private var _prodImage : MutableState<Bitmap?> = mutableStateOf(product.getImage())
    var prodImage : State<Bitmap?> = _prodImage
    private var _prodDescription : MutableState<String> = mutableStateOf(product.getDescription())
    var prodDescription : State<String> = _prodDescription
    private var _prodPrice : MutableState<String> = mutableStateOf(product.getPrice().toString())
    var prodPrice : State<String> = _prodPrice
    private val _priceEditError = mutableStateOf(false)
    val priceEditError : State<Boolean> = _priceEditError
    private var _prodCategory : MutableState<String> = mutableStateOf(product.getCategory())
    var prodCategory : State<String> = _prodCategory
    private var _prodSize : MutableState<String> = mutableStateOf(product.getSize())
    var prodSize : State<String> = _prodSize
    private var _prodQuantity : MutableState<String> = mutableStateOf(product.getQuantity())
    var prodQuantity : State<String> = _prodQuantity
    private var _prodPurchasePlace : MutableState<String> = mutableStateOf(product.getPlaceOfPurchase())
    var prodPurchasePlace : State<String> = _prodPurchasePlace
    private val _placesFiltered : MutableState<List<String>> = mutableStateOf(listOf())
    val placesFiltered : State<List<String>> = _placesFiltered
    private val _barCode : MutableState<String> = mutableStateOf(product.getBarcode())
    val barCode : State<String> = _barCode

    fun updateBarCodeState(newValue: String){
        _barCode.value = newValue
        _productModified.value = true
        updateFabEnabledState()
    }

    fun updateProdPurchasePlace(newValue:String){
        _prodPurchasePlace.value = newValue
        _productModified.value = true
        updateFabEnabledState()

        viewModelScope.launch {
            _placesFiltered.value = repository.filterPlacesRegistered(newValue)
        }
    }

    fun updateProductSizeState(newValue: String){
        _prodSize.value = newValue
        _productModified.value = true
        updateFabEnabledState()
    }

    fun updateProductQuantityState(newValue: String){
        _prodQuantity.value = newValue
        _productModified.value = true
        updateFabEnabledState()
    }

    fun updateProductCategoryState(newValue: String){
        _prodCategory.value = newValue
        _productModified.value = true
        updateFabEnabledState()
    }

    fun updateProductPriceState(newValue: String){
        _priceEditError.value = !CurrencyFormatter.isInputNumericValid(newValue)
        if(_priceEditError.value){
            _prodPrice.value = CurrencyFormatter.formatInput(newValue,_prodPrice.value)
            _productModified.value = true
        }else
            _prodPrice.value = newValue
        updateFabEnabledState()
    }

    fun updateShowImageDialogState(newValue: Boolean){
        _showImageDialog.value = newValue
    }

    /*Checks the state of every text field and if the products has been modified*/
    private fun updateFabEnabledState(){
        _fabEnabled.value = (
                prodDescription.value.isNotEmpty() and
                        prodPurchasePlace.value.isNotEmpty() and
                        prodPrice.value.isNotEmpty() and
                        _productModified.value and
                        !_priceEditError.value
                )
    }

    fun updateProdImage(newValue:Bitmap?){
        _prodImage.value = newValue
        _productModified.value = true
        updateFabEnabledState()
    }

    fun updateProdDescription(newValue:String){
        _prodDescription.value = newValue
        _productModified.value = true
        updateFabEnabledState()
    }

    private fun getProductById(productId:Long){
        /*Launches a coroutine and blocks the current thread until it is completed. It is designed to bridge regular
        * blocking code to libraries written in suspending style*/
        runBlocking {
            product = repository.getProductById(productId)
        }
    }

    fun updateProduct(){
        viewModelScope.launch {
            repository.update(product)
        }
    }

    fun productAlreadyRegistered(description:String,place:String,productId: Long):Boolean{
        return runBlocking {
            return@runBlocking repository.productAlreadyRegistered(description,place,productId)
        }
    }
}