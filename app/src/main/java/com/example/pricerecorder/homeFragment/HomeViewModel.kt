package com.example.pricerecorder.homeFragment

import android.app.Application
import android.content.res.Resources
import android.view.View
import androidx.lifecycle.*
import com.example.pricerecorder.R
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabaseDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.ceil

class HomeViewModel(private val database: ProductDatabaseDao,application: Application): AndroidViewModel(application){
    private var viewModelJob = Job()
    val products : LiveData<List<Product>> = database.getAllProducts()
    var filteredList : MutableList<Product>? = null

    private val _fabClicked = MutableLiveData<Int?>()
    val fabClicked : LiveData<Int?>
        get() = _fabClicked

    init {
        _fabClicked.value = null
    }

    fun onFabClicked(view: View){
        _fabClicked.value = view.id
    }

    fun onNavigated(){
        _fabClicked.value = null
    }

    fun deleteProduct(product: Product){
        viewModelScope.launch {
            database.delete(product)
        }
    }

    fun clear(){
        viewModelScope.launch {
            database.clearDb()
        }
    }

    fun addProduct(product: Product){
        viewModelScope.launch {
            database.insert(product)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun updateProduct(p:Product){
        viewModelScope.launch {
            database.update(p.getProductId(),p.getPrice(),p.getUpdateDate())
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

    fun filterByUserSearch(query:String) : MutableList<Product>{
        val tempList : MutableList<Product> = mutableListOf()
        val list = filteredList ?: products.value!!
        list.forEach {
            if(it.getDescription().lowercase(Locale.getDefault()).contains(query)){
                tempList.add(it)
            }
        }
        return tempList
    }

    fun filterByDate(date:String){
        val list = products.value?.filter { it.getUpdateDate() == date }
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