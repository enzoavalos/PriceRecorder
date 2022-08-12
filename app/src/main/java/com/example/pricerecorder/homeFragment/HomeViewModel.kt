package com.example.pricerecorder.homeFragment

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*
import com.example.pricerecorder.CurrencyFormatter
import com.example.pricerecorder.R
import com.example.pricerecorder.SearchWidgetState
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductsRepository
import com.example.pricerecorder.filters.FilterState
import kotlinx.coroutines.launch
import kotlin.math.ceil

class HomeViewModel(
    @get:JvmName("getViewModelApplication") val application: Application
): AndroidViewModel(application){
    private val repository = ProductsRepository.getInstance(application)
    val products : LiveData<List<Product>>
        get() = repository.products
    var searchWidgetState:State<SearchWidgetState> = repository.searchWidgetState
    var searchTextState: State<String> = repository.searchTextState
    var searching: State<Boolean> = repository.searching

    private val _priceEditTextState = mutableStateOf("")
    val priceEditTextState : State<String> = _priceEditTextState
    private val _priceEditError = mutableStateOf(false)
    val priceEditError : State<Boolean> = _priceEditError

    private val _placesFiltered : MutableState<List<String>> = mutableStateOf(listOf())
    val placesFiltered : State<List<String>> = _placesFiltered
    var categoryFilter : State<String> = repository.categoryFilter
    var placeFilter : State<String> = repository.placeFilter
    var isFiltering : State<Boolean> = repository.isFiltering
    private var _filterEnabled : MutableState<Boolean> = mutableStateOf(false)
    var filterEnabled : State<Boolean> = _filterEnabled

    private fun updateFilterEnabledState(){
        _filterEnabled.value = (
                categoryFilter.value.isNotEmpty() or
                placeFilter.value.isNotEmpty()
                )
    }

    fun updatePlaceFilter(newValue: String){
        repository.updatePlaceFilter(newValue)
        updateFilterEnabledState()

        viewModelScope.launch {
            _placesFiltered.value = repository.filterPlacesRegistered(newValue)
        }
    }

    fun updateCategoryFilter(newValue: String){
        repository.updateCategoryFilter(newValue)
        updateFilterEnabledState()
    }

    fun updateFilterState(newValue: FilterState){
        repository.updateFilterState(newValue)
    }

    fun resetFilters(){
        repository.resetFilters()
        _filterEnabled.value = false
    }

    fun updatePriceEditTextState(newValue: String){
        _priceEditError.value = !CurrencyFormatter.isInputNumericValid(newValue)
        if(!priceEditError.value)
            _priceEditTextState.value = CurrencyFormatter.formatInput(newValue,_priceEditTextState.value)
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
            repository.updateCategoryFilter("")
            repository.updateSearchTextState("")
            repository.updateFilterState(FilterState.IDLE)
            repository.updateSearchWidgetState(SearchWidgetState.CLOSED)
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
    fun getListOfCategories() : List<String>{
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
            list.add(0,application.resources.getString(R.string.option_uncategorized))
        return list.toList()
    }

    /*Returns the maximum price the user has registered rounded up*/
    fun getMaxPrice() : Float{
        products.value!!.let{
            val list = it.sortedByDescending { p -> p.getPrice() }
            return if(list.isNotEmpty()) ceil(list[0].getPrice()).toFloat() else 100f
        }
    }
}