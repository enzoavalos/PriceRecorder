package com.example.pricerecorder.addFragment

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.pricerecorder.CurrencyFormatter
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductsRepository
import kotlinx.coroutines.*

class AddFragmentViewModel(application: Application):ViewModel() {
    companion object{
        val factory = object : ViewModelProvider.Factory{
            @Suppress("unchecked_cast")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                return AddFragmentViewModel(application) as T
            }
        }
    }

    private val repository = ProductsRepository.getInstance(application)

    private var _fabEnabled : MutableState<Boolean> = mutableStateOf(false)
    var fabEnabled : State<Boolean> = _fabEnabled
    private var _showImageDialog = mutableStateOf(false)
    var showImageDialog : State<Boolean> = _showImageDialog

    private var _prodImage : MutableState<Bitmap?> = mutableStateOf(null)
    var prodImage : State<Bitmap?> = _prodImage
    private var _prodDescription : MutableState<String> = mutableStateOf("")
    var prodDescription : State<String> = _prodDescription
    private var _prodPrice : MutableState<String> = mutableStateOf("")
    var prodPrice : State<String> = _prodPrice
    private val _priceEditError = mutableStateOf(false)
    val priceEditError : State<Boolean> = _priceEditError
    private var _prodCategory : MutableState<String> = mutableStateOf("")
    var prodCategory : State<String> = _prodCategory
    private var _prodSize : MutableState<String> = mutableStateOf("")
    var prodSize : State<String> = _prodSize
    private var _prodQuantity : MutableState<String> = mutableStateOf("")
    var prodQuantity : State<String> = _prodQuantity
    private var _prodPurchasePlace : MutableState<String> = mutableStateOf("")
    var prodPurchasePlace : State<String> = _prodPurchasePlace
    private val _placesFiltered : MutableState<List<String>> = mutableStateOf(listOf())
    val placesFiltered : State<List<String>> = _placesFiltered

    private val _barCode : MutableState<String> = mutableStateOf("")
    val barCode : State<String> = _barCode

    fun updateBarCodeState(newValue: String){
        _barCode.value = newValue
    }

    fun updateProdPurchasePlace(newValue:String){
        _prodPurchasePlace.value = newValue
        updateFabEnabledState()

        viewModelScope.launch {
            _placesFiltered.value = repository.filterPlacesRegistered(newValue)
        }
    }

    fun updateProductSizeState(newValue: String){
        _prodSize.value = newValue
    }

    fun updateProductQuantityState(newValue: String){
        _prodQuantity.value = newValue
    }

    fun updateProductCategoryState(newValue: String){
        _prodCategory.value = newValue
    }

    fun updateProductPriceState(newValue: String){
        _priceEditError.value = !CurrencyFormatter.isInputNumericValid(newValue)
        if(_priceEditError.value)
            _prodPrice.value = CurrencyFormatter.formatInput(newValue,_prodPrice.value)
        else
            _prodPrice.value = newValue
        updateFabEnabledState()
    }

    fun updateShowImageDialogState(newValue: Boolean){
        _showImageDialog.value = newValue
    }

    /*Checks the state of every text field to determine if they all have valid inputs*/
    private fun updateFabEnabledState(){
        _fabEnabled.value = (
            _prodDescription.value.isNotEmpty() and
            _prodPurchasePlace.value.isNotEmpty() and
            _prodPrice.value.isNotEmpty() and
            !_priceEditError.value
        )
    }

    fun updateProdImage(newValue:Bitmap?){
        _prodImage.value = newValue
    }

    fun updateProdDescription(newValue:String){
        _prodDescription.value = newValue
        updateFabEnabledState()
    }

    fun addProduct(product: Product){
        viewModelScope.launch {
            repository.insertProduct(product)
        }
    }

    fun productAlreadyRegistered(description:String,place:String):Boolean{
        return runBlocking {
            return@runBlocking repository.productAlreadyRegistered(description,place)
        }
    }
}