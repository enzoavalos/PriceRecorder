package com.example.pricerecorder

import android.app.Application
import android.view.View
import androidx.lifecycle.*
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabaseDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeViewModel(val database: ProductDatabaseDao, application: Application): AndroidViewModel(application){
    private var viewModelJob = Job()
    val products = database.getAllProducts()

    private val _fabClicked = MutableLiveData<Int?>()
    val fabClicked : LiveData<Int?>
        get() = _fabClicked

    init {
        _fabClicked.value = null
    }

    fun getProductsList() = products

    fun onFabClicked(view: View){
        _fabClicked.value = view.id
    }

    fun onNavigated(){
        _fabClicked.value = null
    }

    fun addProduct(product: Product){
        viewModelScope.launch {
            database.insert(product)
        }
    }

    fun clear(){
        viewModelScope.launch {
            database.clearDb()
        }
    }

    override fun onCleared() {
        super.onCleared()
        clear()
        viewModelJob.cancel()
    }
}