package com.example.pricerecorder.homeFragment

import android.app.Application
import android.view.View
import androidx.lifecycle.*
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabaseDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class HomeViewModel(private val database: ProductDatabaseDao,application: Application): AndroidViewModel(application){
    private var viewModelJob = Job()
    val products : LiveData<List<Product>> = database.getAllProducts()

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
            database.update(p)
        }
    }

    /*Calculates the increase in the price of the product since the last time it was updated*/
    fun getPriceIncrease(p:Product) : Pair<String,String>{
        var i = p.priceHistory.lastIndex
        i = if(i > 1) i-2 else 0
        var pair = Pair(p.updateDate,"+0.0")
        if(i != 0){
            val oldPrice = p.priceHistory[i].first
            val diff = p.price - oldPrice
            var percentage : Double = (diff * 100) / oldPrice
            percentage = "%.${1}f".format(Locale.ENGLISH,percentage).toDouble()
            val increase = if(percentage >= 0.0) "+${percentage}" else percentage.toString()
            pair = Pair(p.priceHistory[i].second,increase)
        }
        return pair
    }
}