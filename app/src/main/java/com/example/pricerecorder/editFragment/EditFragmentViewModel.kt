package com.example.pricerecorder.editFragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabaseDao
import kotlinx.coroutines.launch

class EditFragmentViewModel(private val databaseDao: ProductDatabaseDao) : ViewModel() {
    suspend fun getProductById(productId:Long): Product{
        return databaseDao.get(productId)!!
    }

    fun updateProduct(product: Product){
        viewModelScope.launch {
            databaseDao.update(product)
        }
    }
}