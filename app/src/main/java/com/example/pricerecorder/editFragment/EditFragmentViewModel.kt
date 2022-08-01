package com.example.pricerecorder.editFragment

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabaseDao
import com.example.pricerecorder.database.ProductsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class EditFragmentViewModel(application: Application) : ViewModel() {
    private val repository = ProductsRepository.getInstance(application)

    fun getProductById(productId:Long): Product{
        var product:Product?
        runBlocking {
            product = repository.getProductById(productId)
        }
        return product!!
    }

    fun updateProduct(p: Product){
        viewModelScope.launch {
            repository.update(p)
        }
    }
}