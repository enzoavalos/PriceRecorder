package com.example.pricerecorder.homeFragment

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.pricerecorder.CurrencyFormatter
import com.example.pricerecorder.SearchWidgetState
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductsRepository
import com.example.pricerecorder.FilterState
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class HomeViewModel(
    @get:JvmName("getViewModelApplication") val application: Application
): AndroidViewModel(application){
    companion object{
        val factory = object : ViewModelProvider.Factory{
            @Suppress("unchecked_cast")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return HomeViewModel(application) as T
            }
        }
    }

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
    var showSwipeTutorial : State<Boolean> = repository.showSwipeTutorial

    private var _barcodeFilter = mutableStateOf("")
    var barcodeFilter : State<String> = _barcodeFilter

    fun updateShowSwipeTutorialState(show:Boolean){
        repository.updateShowSwipeTutorialState(show)
    }

    fun updateSearchWidgetState(newValue: SearchWidgetState){
        repository.updateShowSwipeTutorialState(show = false)
        repository.updateSearchWidgetState(newValue)
    }

    fun updateSearchTextState(newValue: String){
        repository.updateSearchTextState(newValue,viewModelScope)
    }

    fun updateBarcodeFilter(newValue: String){
        _barcodeFilter.value = newValue
        if(newValue.isNotEmpty())
            viewModelScope.launch {
                repository.filterByBarcode(newValue)
            }
    }

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

    fun filterProducts(){
        viewModelScope.launch{
            repository.filterProducts()
        }
    }

    fun resetFilters(){
        repository.resetFilters()
        _filterEnabled.value = false
    }

    fun resetSearchState(){
        updateSearchTextState("")
        updateSearchWidgetState(SearchWidgetState.CLOSED)
    }

    fun updatePriceEditTextState(newValue: String) {
        _priceEditError.value = !CurrencyFormatter.isInputNumericValid(newValue)
        if (!priceEditError.value)
            _priceEditTextState.value =
                CurrencyFormatter.formatInput(newValue, _priceEditTextState.value)
        else
            _priceEditTextState.value = newValue
    }

    fun deleteProduct(productId:Long){
        viewModelScope.launch {
            repository.deleteProduct(productId)
        }
        repository.updateFilterState(FilterState.IDLE)
        repository.updateSearchWidgetState(SearchWidgetState.CLOSED)
        repository.updateCategoryFilter("")
        repository.updateSearchTextState("")
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
    fun getListOfCategories() : List<String> {
        var result : List<String>
        runBlocking {
            result = repository.getCategoriesRegistered()
        }
        return result
    }
}