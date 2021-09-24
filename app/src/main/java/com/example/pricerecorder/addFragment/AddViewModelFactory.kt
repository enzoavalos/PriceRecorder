package com.example.pricerecorder.addFragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pricerecorder.database.ProductDatabaseDao

class AddViewModelFactory(private val dataSource: ProductDatabaseDao):ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddFragmentViewModel::class.java)) {
            return AddFragmentViewModel(dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}