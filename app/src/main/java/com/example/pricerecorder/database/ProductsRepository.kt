package com.example.pricerecorder.database

import android.app.Application
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.pricerecorder.SearchState
import com.example.pricerecorder.SearchWidgetState
import com.example.pricerecorder.filters.CategoryFilter
import com.example.pricerecorder.filters.FilterState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception
import com.example.pricerecorder.R
import com.example.pricerecorder.filters.AndFilter
import com.example.pricerecorder.filters.PlaceFilter
import java.net.Inet4Address

/*Repository isolates the data layer from the rest of the app, meaning, where all data and business logic
* is handled, exposing consistent APIs for the rest of the app to access the data*/
class ProductsRepository private constructor(
    @get:JvmName("getProductsRepositoryApplication") val application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
){
    companion object{
        const val MIN_INPUT_PREDICTION_LENGTH = 2

        /*Instance stores a reference to the first ever Repo returned by get instance, therefore being
        * instantiated only once.*/
        @Volatile private var INSTANCE : ProductsRepository? = null

        fun getInstance(application: Application) : ProductsRepository{
            synchronized(this){
                if(INSTANCE == null)
                    INSTANCE = ProductsRepository(application)
                return INSTANCE!!
            }
        }
    }

    private val databaseDao = ProductDatabase.getInstance(application).productDatabaseDao

    /*Both public val's will expose their values to composable functions, and are used to track the state
    * of the search bar and its text input*/
    val searchWidgetState: MutableState<SearchWidgetState> =
        mutableStateOf(SearchWidgetState.CLOSED)
    val searchTextState: MutableState<String> = mutableStateOf("")

    private val _isSearching : MutableState<SearchState> = mutableStateOf(SearchState.STARTING)
    val searching: State<Boolean> = derivedStateOf { _isSearching.value == SearchState.SEARCHING }
    /*var that keeps track of the DB data*/
    private var _products : LiveData<List<Product>> = databaseDao.getAllProducts()
    /*val to store the current dataset to expose to the view model*/
    private val _searchResults = MutableLiveData<List<Product>>(mutableListOf())
    val products : LiveData<List<Product>>
        get() {
            return if((_isSearching.value == SearchState.STARTING) and (_isFiltering.value == FilterState.IDLE)) _products
            else _searchResults}

    private var _isFiltering : MutableState<FilterState> = mutableStateOf(FilterState.IDLE)
    val isFiltering : State<Boolean> = derivedStateOf { _isFiltering.value == FilterState.FILTERING }
    private var _categoryFilter = mutableStateOf("")
    var categoryFilter : State<String> = _categoryFilter
    private var _placeFilter = mutableStateOf("")
    var placeFilter : State<String> = _placeFilter

    fun updatePlaceFilter(newValue: String){
        _placeFilter.value = newValue
    }

    fun updateCategoryFilter(newValue: String){
        _categoryFilter.value = newValue
    }

    fun updateFilterState(newValue: FilterState){
        _isFiltering.value = newValue
        if(_isFiltering.value == FilterState.FILTERING)
            filterProducts()
        else
            resetFilters()
    }

    private fun filterProducts(){
        val compoundFilter = AndFilter(
            CategoryFilter(
            category = _categoryFilter.value,
            application.resources.getString(R.string.option_uncategorized)),
            PlaceFilter( _placeFilter.value )
        )
        val list = _products.value?.filter{ compoundFilter.meetsCriteria(it) }
        _searchResults.value = list ?: listOf()
    }

    fun resetFilters(){
        _categoryFilter.value = ""
        _placeFilter.value = ""
    }

    /*Filters the current list based on the user input parameters*/
    private fun searchProductList(query:String,listToSearch:List<Product>?){
        if(_isSearching.value == SearchState.STARTING) {
            _searchResults.value = _products.value
            return
        }

        val results = listToSearch?.filter {
            it.getDescription().contains(query, ignoreCase = true)
        }
        _searchResults.value = results ?: listOf()
    }

    /*When user enters a new character the search is performed on the previous filtered list, otherwise
    * performed on the whole products list
    * Also, when search bar is emptied or newly opened, all products are shown*/
    fun updateSearchTextState(newValue: String){
        val userDeletedChars = userDeletedCharsFromQuery(newValue,searchTextState.value)
        _isSearching.value = if(newValue.isEmpty()) SearchState.STARTING else SearchState.SEARCHING
        searchTextState.value = newValue

        searchProductList(newValue, listToSearch =
        if(userDeletedChars or (_isSearching.value == SearchState.SEARCHING)) _products.value
        else _searchResults.value)
    }

    fun updateSearchWidgetState(newValue: SearchWidgetState){
        searchWidgetState.value = newValue
    }

    /*Determines if the user has deleter chars from the query*/
    private fun userDeletedCharsFromQuery(newValue: String,oldValue:String):Boolean{
        return (newValue.length < oldValue.length)
    }

    suspend fun insertProduct(product: Product){
        withContext(ioDispatcher){
            databaseDao.insert(product)
        }
    }

    suspend fun getProductById(productId:Long): Product{
        val result = withContext(ioDispatcher){
            return@withContext databaseDao.get(productId)!!
        }
        return result
    }

    suspend fun deleteProduct(product: Product){
        withContext(ioDispatcher){
            databaseDao.delete(product)
        }
    }

    suspend fun clear(){
        withContext(ioDispatcher){
            databaseDao.clearDb()
        }
    }

    suspend fun update(p: Product){
        withContext(ioDispatcher){
            databaseDao.update(p)
        }
    }

    /*withContext method when called immediately suspends the block inside it and awaits for the value of a
    * coroutine powered lambda call*/
    suspend fun backupDB() =
        withContext(ioDispatcher){
            return@withContext try {
                databaseDao.checkPoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
                true
            }catch (e: Exception){
                Log.w("SettingsViewModel",e.toString())
                false
            }
        }

    /*Called when database has been restored from previous backup*/
    fun databaseRestored(){
        _products = databaseDao.getAllProducts()
    }

    suspend fun filterPlacesRegistered(query: String):List<String>{
        if(query.length < MIN_INPUT_PREDICTION_LENGTH)
            return listOf()

        return withContext(ioDispatcher){
            return@withContext databaseDao.filterPlacesRegistered(query)
        }
    }
}