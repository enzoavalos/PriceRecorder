package com.example.pricerecorder.settingsFragment

import android.app.Application
import android.util.Log
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.pricerecorder.database.ProductDatabaseDao
import java.lang.Exception

class SettingsFragmentViewModel(private val database: ProductDatabaseDao, application: Application) : AndroidViewModel(application) {
    private val _viewClicked = MutableLiveData<Int?>()
    val viewClicked : LiveData<Int?>
        get() = _viewClicked

    init {
        _viewClicked.value = null
    }

    fun onViewClicked(view: View){
        _viewClicked.value = view.id
    }

    fun onClickEventHandled(){
        _viewClicked.value = null
    }

    /*Creates a checkpoint in the Room db and return true if successful*/
    fun backupDatabase() : Boolean{
        return try {
            database.checkPoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
            true
        }catch (e:Exception){
            Log.w("SettingsViewModel",e.toString())
            false
        }
    }
}