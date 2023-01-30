package com.example.pricerecorder.database

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.pricerecorder.SearchState
import com.example.pricerecorder.SearchWidgetState
import com.example.pricerecorder.FilterState
import com.example.pricerecorder.R
import java.lang.Exception
import kotlinx.coroutines.*

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
    private val noCategoryString = application.resources.getString(R.string.option_uncategorized)
    private val databaseDao = ProductDatabase.getInstance(application).productDatabaseDao

    /*Both public val's will expose their values to composable functions, and are used to track the state
    * of the search bar and its text input*/
    val searchWidgetState: MutableState<SearchWidgetState> =
        mutableStateOf(SearchWidgetState.CLOSED)
    val searchTextState: MutableState<String> = mutableStateOf("")

    /*Used to track whether the swipe tutorial is shown to the user, able to survive home fragment's
    * destruction*/
    private val _showSwipeTutorial = mutableStateOf(true)
    val showSwipeTutorial : State<Boolean> = _showSwipeTutorial

    private val _isSearching : MutableState<SearchState> = mutableStateOf(SearchState.STARTING)
    val searching: State<Boolean> = derivedStateOf { _isSearching.value == SearchState.SEARCHING }
    /*var that keeps track of the DB data*/
    private var _products : LiveData<List<Product>> = databaseDao.getAllProducts()
    /*val to store the current dataset to expose to the view model*/
    private val _searchResults = MutableLiveData<List<Product>>(mutableListOf())
    /*val to store the list associated to the last filter made by the user*/
    private val _filterResults = MutableLiveData<List<Product>>(mutableListOf())
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

    fun updateShowSwipeTutorialState(show:Boolean){
        _showSwipeTutorial.value = show
    }

    fun updatePlaceFilter(newValue: String){
        _placeFilter.value = newValue
    }

    fun updateCategoryFilter(newValue: String){
        _categoryFilter.value = newValue
    }

    fun updateFilterState(newValue: FilterState){
        _isFiltering.value = newValue
        if(_isFiltering.value != FilterState.FILTERING)
            resetFilters()
    }

    fun resetFilters(){
        _categoryFilter.value = ""
        _placeFilter.value = ""
        _filterResults.value = listOf()
    }

    /*Filters the product list based on user queries, if either the searchbar is newly opened or its content
    * is empty, all products registered or those previously filtered are shown*/
    fun updateSearchTextState(newValue: String,
        viewModelScope:CoroutineScope?=null){
        searchTextState.value = newValue
        _isSearching.value = if(newValue.isEmpty()) SearchState.STARTING else SearchState.SEARCHING

        if(!searching.value) {
            if(!isFiltering.value)
                _searchResults.value = _products.value
            else
                _searchResults.value = _filterResults.value
            return
        }

        viewModelScope?.launch {
            searchProductList(query = newValue)
        }
    }

    /*Filters the current list based on the user input parameters*/
    private suspend fun searchProductList(query:String){
        val results = withContext(ioDispatcher){
            return@withContext if(isFiltering.value)
                databaseDao.searchProductList(
                    query,
                    catFilter = if(_categoryFilter.value == noCategoryString) null else _categoryFilter.value,
                    _placeFilter.value
                    )
            else
                databaseDao.searchProductList(query)
        }
        _searchResults.value = results
    }

    suspend fun filterProducts(){
        _isFiltering.value = FilterState.FILTERING
        val result = withContext(ioDispatcher){
            return@withContext databaseDao.filterProductList(
                catFilter = if(_categoryFilter.value == noCategoryString) null else _categoryFilter.value,
                _placeFilter.value)
        }
        _filterResults.value = result
        _searchResults.value = result
    }

    /*Filters the whole products list based on a scanned barcode*/
    suspend fun filterByBarcode(barcode:String){
        val result = withContext(ioDispatcher){
            return@withContext databaseDao.filterByBarcode(barcode)
        }
        _isFiltering.value = FilterState.FILTERING
        _filterResults.value = result
        _searchResults.value = _filterResults.value
    }

    fun updateSearchWidgetState(newValue: SearchWidgetState){
        searchWidgetState.value = newValue
    }

    /*Determines if there already is a product registered with the same data*/
    suspend fun productAlreadyRegistered(description:String,place:String,productId: Long? = null):Boolean{
        val result = withContext(ioDispatcher){
            return@withContext databaseDao.checkExistence(description,place,productId)
        }
        return (result == 1)
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

    suspend fun deleteProduct(productId:Long){
        withContext(ioDispatcher){
            databaseDao.delete(productId)
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

    suspend fun getCategoriesRegistered():List<String>{
        return withContext(ioDispatcher){
            return@withContext databaseDao.getListOfAllCategoriesRegistered(noCategoryString)
        }
    }
}