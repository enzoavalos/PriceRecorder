package com.example.pricerecorder.addFragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabaseDao
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AddFragmentViewModel(private val databaseDao: ProductDatabaseDao):ViewModel() {
    fun addProduct(product: Product){
        runBlocking {
            viewModelScope.launch {
                databaseDao.insert(product)
            }
        }
    }
}