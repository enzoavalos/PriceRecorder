package com.example.pricerecorder.settingsFragment

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pricerecorder.database.ProductDatabaseDao

class SettingFragmentViewModelFactory(private val dataSource: ProductDatabaseDao,
                                      private val application: Application) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsFragmentViewModel::class.java)) {
            return SettingsFragmentViewModel(dataSource,application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}