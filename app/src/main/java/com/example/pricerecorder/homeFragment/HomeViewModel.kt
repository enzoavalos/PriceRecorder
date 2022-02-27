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
            database.update(p.productId,p.price,p.updateDate,p.priceHistory)
        }
    }

    /*Calculates the increase in the price of the product since the last time it was updated*/
    fun getPriceIncrease(p:Product) : Pair<String,String>{
        val oldPrice = p.priceHistory.first
        val diff = p.price - oldPrice
        var percentage : Double = (diff * 100) / oldPrice
        percentage = "%.${1}f".format(Locale.ENGLISH,percentage).toDouble()
        val increase = if(percentage >= 0.0) "+${percentage}" else percentage.toString()
        return Pair(increase,p.priceHistory.second)
    }

    /*Returns a list with all the categories associated to the products the user has registered*/
    fun getListOfCategories(resources:Resources) : MutableList<String>{
        val list = mutableListOf<String>()
        var noCategory = false
        products.value?.let {
            it.forEach { p ->
                if(p.category.isNotEmpty()){
                    if(!list.contains(p.category))
                        list.add(p.category)
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
                if(!list.contains(p.placeOfPurchase))
                    list.add(p.placeOfPurchase)
            }
        }
        if(list.isNotEmpty())
            list.sortBy { it }
        return list
    }

    fun filterByPlace(place:String){
        val list = products.value?.filter { it.placeOfPurchase == place }
        filteredList = list as MutableList<Product>
    }

    fun filterByCategory(cat:String?){
        val list = products.value?.filter { it.category == cat }
        filteredList = list as MutableList<Product>
    }

    fun filterByUserSearch(query:String) : MutableList<Product>{
        val tempList : MutableList<Product> = mutableListOf()
        val list = filteredList ?: products.value!!
        list.forEach {
            if(it.description.lowercase(Locale.getDefault()).contains(query)){
                tempList.add(it)
            }
        }
        return tempList
    }

    fun filterByDate(date:String){
        val list = products.value?.filter { it.updateDate == date }
        filteredList = list as MutableList<Product>
    }

    /*Returns the maximum price the user has registered rounded up*/
    fun getMaxPrice() : Float{
        products.value!!.let{
            val list = it.sortedByDescending { p -> p.price }
            return ceil(list[0].price).toFloat()
        }
    }

    fun filterByPriceRange(min:Float,max:Float){
        val list = products.value?.filter { (it.price >= min.toDouble()) and (it.price <= max.toDouble()) }
        filteredList = list as MutableList<Product>
    }
}