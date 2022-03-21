package com.example.pricerecorder.settingsFragment

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsFragmentViewModel : ViewModel() {
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
}