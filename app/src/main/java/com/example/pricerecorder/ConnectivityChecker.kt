package com.example.pricerecorder

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

@Suppress("Deprecation")
class ConnectivityChecker() {
    /*Checks if the device currently has an internet connection available*/
    companion object{
        fun isOnline(application: Application) : Boolean{
            val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if(capabilities != null){
                    when{
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return true
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return true
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return true
                    }
                }
            } else {
                val networkInfo = connectivityManager.activeNetworkInfo
                if((networkInfo != null) and (networkInfo!!.isConnected))
                    return true
            }
            return false
        }
    }
}