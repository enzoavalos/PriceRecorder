package com.example.pricerecorder.addFragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabaseDao
import kotlinx.coroutines.launch

class AddFragmentViewModel(private val databaseDao: ProductDatabaseDao):ViewModel() {
    fun addProduct(product: Product){
        viewModelScope.launch {
            databaseDao.insert(product)
        }
    }
}