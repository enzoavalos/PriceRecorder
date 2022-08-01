package com.example.pricerecorder.addFragment

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
import kotlinx.coroutines.*

class AddFragmentViewModel(application: Application):ViewModel() {
    private val repository = ProductsRepository.getInstance(application)

    private var _fabEnabled : MutableState<Boolean> = mutableStateOf(false)
    var fabEnabled : State<Boolean> = _fabEnabled

    private var _prodImage : MutableState<Bitmap?> = mutableStateOf(null)
    var prodImage : State<Bitmap?> = _prodImage

    private var _prodDescription : MutableState<String> = mutableStateOf("")
    var prodDescription : State<String> = _prodDescription

    private var _prodPurchasePlace : MutableState<String> = mutableStateOf("")
    var prodPurchasePlace : State<String> = _prodPurchasePlace

    private var _showImageDialog = mutableStateOf(false)
    var showImageDialog : State<Boolean> = _showImageDialog

    private var _prodPrice : MutableState<String> = mutableStateOf("")
    var prodPrice : State<String> = _prodPrice

    fun updateProductPriceState(newValue: String){
        _prodPrice.value = CurrencyFormatter.formatInput(newValue)
        updateFabEnabledState()
    }

    fun updateShowImageDialogState(newValue: Boolean){
        _showImageDialog.value = newValue
    }

    /*Checks the state of every text field to determine if they all have valid inputs*/
    private fun updateFabEnabledState(){
        _fabEnabled.value = (
            prodDescription.value.isNotEmpty() and
            prodPurchasePlace.value.isNotEmpty() and
            prodPrice.value.isNotEmpty()
        )
        /*TODO("agregar chequeo sobre si textfield de precio esta en error si no es posible agregar teclado decimal")*/
    }

    fun updateProdImage(newValue:Bitmap?){
        _prodImage.value = newValue
    }

    fun updateProdDescription(newValue:String){
        _prodDescription.value = newValue
        updateFabEnabledState()
    }

    fun updateProdPurchasePlace(newValue:String){
        _prodPurchasePlace.value = newValue
        updateFabEnabledState()
    }

    fun addProduct(product: Product){
        viewModelScope.launch {
            repository.insertProduct(product)
        }
    }
}