package com.example.pricerecorder.homeFragment

import android.app.Application
import android.content.res.Resources
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*
import com.example.pricerecorder.CurrencyFormatter
import com.example.pricerecorder.DateUtils
import com.example.pricerecorder.R
import com.example.pricerecorder.SearchWidgetState
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductsRepository
import kotlinx.coroutines.launch
import kotlin.math.ceil

class HomeViewModel(application: Application): AndroidViewModel(application){
    private val repository = ProductsRepository.getInstance(application)
    val products : LiveData<List<Product>>
        get() = repository.products
    var searchWidgetState:State<SearchWidgetState> = repository.searchWidgetState
    var searchTextState: State<String> = repository.searchTextState
    var searching: State<Boolean> = repository.searching

    var filteredList : MutableList<Product>? = null

    private val _priceEditTextState = mutableStateOf("")
    val priceEditTextState : State<String> = _priceEditTextState
    private val _priceEditError = mutableStateOf(false)
    val priceEditError : State<Boolean> = _priceEditError

    fun updatePriceEditTextState(newValue: String){
        /*TODO("arreglar bugs al manejar numeros y caracteres")*/
        _priceEditError.value = !CurrencyFormatter.isInputNumericValid(newValue)
        if(!priceEditError.value)
            _priceEditTextState.value = CurrencyFormatter.formatInput(newValue)
        else
            _priceEditTextState.value = newValue
    }

    fun updateSearchWidgetState(newValue: SearchWidgetState){
        repository.updateSearchWidgetState(newValue)
    }

    fun updateSearchTextState(newValue: String){
        repository.updateSearchTextState(newValue)
    }

    fun deleteProduct(product: Product){
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun clear(){
        viewModelScope.launch {
            repository.clear()
        }
    }

    fun addProduct(product: Product){
        viewModelScope.launch {
            repository.insertProduct(product)
        }
    }

    fun updateProduct(p:Product){
        viewModelScope.launch {
            repository.update(p)
        }
    }

    /*Returns a list with all the categories associated to the products the user has registered*/
    fun getListOfCategories(resources:Resources) : MutableList<String>{
        val list = mutableListOf<String>()
        var noCategory = false
        products.value?.let {
            it.forEach { p ->
                if(p.getCategory().isNotEmpty()){
                    if(!list.contains(p.getCategory()))
                        list.add(p.getCategory())
                }else
                    noCategory = true
            }
        }
        if(list.isNotEmpty())
            list.sortBy { it }
        if(noCategory)
            list.add(0,resources.getString(R.string.option_uncategorized))
        return list
    }

    /*Returns a list with all the purchase places associated to the products the user has registered*/
    fun getListOfPlaces() : MutableList<String>{
        val list = mutableListOf<String>()
        products.value?.let {
            it.forEach { p ->
                if(!list.contains(p.getPlaceOfPurchase()))
                    list.add(p.getPlaceOfPurchase())
            }
        }
        if(list.isNotEmpty())
            list.sortBy { it }
        return list
    }

    fun filterByPlace(place:String){
        val list = products.value?.filter { it.getPlaceOfPurchase() == place }
        filteredList = list as MutableList<Product>
    }

    fun filterByCategory(cat:String?){
        val list = products.value?.filter { it.getCategory() == cat }
        filteredList = list as MutableList<Product>
    }

    fun filterByDate(date:String){
        val list = products.value?.filter { DateUtils.formatDate(it.getUpdateDate()) == date }
        filteredList = list as MutableList<Product>
    }

    /*Returns the maximum price the user has registered rounded up*/
    fun getMaxPrice() : Float{
        products.value!!.let{
            val list = it.sortedByDescending { p -> p.getPrice() }
            return if(list.isNotEmpty()) ceil(list[0].getPrice()).toFloat() else 100f
        }
    }

    fun filterByPriceRange(min:Float,max:Float){
        val list = products.value?.filter { (it.getPrice() >= min.toDouble()) and (it.getPrice() <= max.toDouble()) }
        filteredList = list as MutableList<Product>
    }
}