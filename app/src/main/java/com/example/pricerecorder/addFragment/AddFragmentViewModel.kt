package com.example.pricerecorder.addFragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricerecorder.database.Product
import com.example.pricerecorder.database.ProductDatabaseDao
import kotlinx.coroutines.launch

class AddFragmentViewModel(private val databaseDao: ProductDatabaseDao):ViewModel() {
    private val _addButtonClicked = MutableLiveData<Boolean>()
    val addButtonClicked: LiveData<Boolean>
        get() = _addButtonClicked

    init {
        _addButtonClicked.value = false
    }

    fun onAddButtonCLicked(){
        _addButtonClicked.value = true
    }

    fun onNavigated() {
        _addButtonClicked.value = false
    }

    fun addProduct(product: Product){
        viewModelScope.launch {
            databaseDao.insert(product)
        }
    }
}