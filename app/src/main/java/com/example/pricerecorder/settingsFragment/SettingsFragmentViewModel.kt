package com.example.pricerecorder.settingsFragment

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.pricerecorder.ConnectivityChecker
import com.example.pricerecorder.DateUtils
import com.example.pricerecorder.R
import com.example.pricerecorder.database.ProductsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.*

/*@get:JvmName used to generate a custom named getter for val application, since if not given then the default
* getter accidentally overrides a defined getter*/
class SettingsFragmentViewModel(
    @get:JvmName("getViewModelApplication") val application: Application) : AndroidViewModel(application) {
    companion object{
        val factory = object : ViewModelProvider.Factory{
            @Suppress("unchecked_cast")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return SettingsFragmentViewModel(application) as T
            }
        }
    }

    private val repository = ProductsRepository.getInstance(application)
    /*Variables used for accessing firebase functionality as authentication and cloud storage*/
    /*Entry point of the firebase authentication sdk*/
    private var mAuth : FirebaseAuth = FirebaseAuth.getInstance()
    /*Creates a reference to upload,download or delete files. It can be thought as a pointer to a file in the cloud*/
    var storageRef : StorageReference = FirebaseStorage.getInstance().reference
    private var _user : MutableState<FirebaseUser?> = mutableStateOf(mAuth.currentUser)
    val user : State<FirebaseUser?> = _user
    private var _lastBackupDate : MutableState<String?> = mutableStateOf(null)
    val lastBackupDate : State<String?> = _lastBackupDate

    /*Gets the date of the last backup performed by the user only in the case one has been made*/
    fun getLastBackupDate(){
        when{
            (user.value == null) or
                    !isUserSignedIn(navigate = null) or
                    !checkInternetConnection(onFailure = null) -> updateLastBackupDateState(null)
            else -> {
                val fileRef = storageRef.child("room_backups/" + user.value!!.uid)
                fileRef.metadata.addOnSuccessListener {
                    val modifiedDate = DateUtils.formatDate(it.creationTimeMillis)
                    val backupDate = application.resources.getString(R.string.last_export_date,modifiedDate)
                    updateLastBackupDateState(backupDate)
                }
            }
        }
    }

    private fun updateLastBackupDateState(newValue:String?){
        _lastBackupDate.value = newValue
    }

    /*Check if the user is currently signed in with its google account*/
    fun isUserSignedIn(navigate:(() -> Unit)?) : Boolean{
        return if(user.value == null){
            if(navigate != null)
                navigate()
            false
        }else
            true
    }

    /*Checks if the device currently has internet connection*/
    fun checkInternetConnection(onFailure:(() -> Unit)?) : Boolean{
        if(!ConnectivityChecker.isOnline(application)){
            if(onFailure != null)
                onFailure()
            return false
        }
        return true
    }

    /*Creates a checkpoint in the Room db and return true if successful
    * Async block is returned from method in the form of a Deferred<Boolean> suspendable object*/
    fun backupDatabaseAsync() =
        viewModelScope.async {
            repository.backupDB()
        }

    fun notifyDatabaseRestored(){
        repository.databaseRestored()
    }
}