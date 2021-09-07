package com.example.pricerecorder.homeFragment

import android.app.Application
import android.view.View
import androidx.lifecycle.*
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabaseDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeViewModel(private val database: ProductDatabaseDao, application: Application): AndroidViewModel(application){
    private var viewModelJob = Job()

    val products : LiveData<List<Product>> = database.getAllProducts()

    private val _fabClicked = MutableLiveData<Int?>()
    val fabClicked : LiveData<Int?>
        get() = _fabClicked

    private val _productClicked = MutableLiveData<Long?>()
    val productClicked : LiveData<Long?>
        get() = _productClicked

    init {
        _fabClicked.value = null
        _productClicked.value = null
    }

    fun onProductClicked(productId: Long){
        _productClicked.value = productId
    }

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

    fun deleteProduct(product: Product){
        viewModelScope.launch {
            database.delete(product)
        }
    }

    private fun clear(){
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