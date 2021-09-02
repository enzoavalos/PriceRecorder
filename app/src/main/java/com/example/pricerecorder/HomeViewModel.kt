package com.example.pricerecorder

import android.app.Application
import android.view.View
import androidx.lifecycle.*
import com.example.pricerecorder.database.ProductDatabaseDao
import kotlinx.coroutines.Job

class HomeViewModel(val database: ProductDatabaseDao, application: Application): AndroidViewModel(application){
    private var viewModelJob = Job()
    private val products = database.getAllProducts()

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
}