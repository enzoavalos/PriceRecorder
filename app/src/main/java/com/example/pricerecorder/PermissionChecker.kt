package com.example.pricerecorder

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

interface PermissionChecker {
    companion object{
        const val FILE_REQUEST_CODE = 101
        const val CAMERA_REQUEST_CODE = 102
        /*Check if permission has been granted to either access external files or the camera, and if its not
        * then it requests for it*/
        fun checkForPermissions(context: Context, permission:String, requestCode:Int, activityWithPermission: () -> Unit,
                                launcher:ActivityResultLauncher<String>){
            fun launchActivityWithPermission(requestCode:Int){
                when(requestCode){
                    FILE_REQUEST_CODE, CAMERA_REQUEST_CODE -> activityWithPermission()
                }
            }

            /*Checks if the sdk version is 23 or above*/
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                when(ContextCompat.checkSelfPermission(context,permission)){
                    PackageManager.PERMISSION_GRANTED -> { launchActivityWithPermission(requestCode) }
                    else -> {
                        /*Request for the user permission to access certain documents and features of the device*/
                        when(requestCode){
                            FILE_REQUEST_CODE, CAMERA_REQUEST_CODE -> launcher.launch(permission)
                        }
                    }
                }
            }else{ launchActivityWithPermission(requestCode) }
        }
    }
}