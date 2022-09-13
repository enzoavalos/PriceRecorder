package com.example.pricerecorder

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionChecker(
    private val context: Context,
    registry: ActivityResultRegistry
) {
    private var onPermissionGranted : () -> Unit = {}

    /*Handles the return value of their specific request made to the user*/
    private val readExternalFilesPermission = registry.register("readExternalFilesPermission",
        ActivityResultContracts.RequestPermission()){
        if(it)
            onPermissionGranted()
    }

    private val accessCameraPermission = registry.register("accessCameraPermission",
        ActivityResultContracts.RequestPermission()){
        if(it)
            onPermissionGranted()
    }

    /*Check if permission has been granted to either access external files or the camera, and if its not
    * then it requests for it*/
    fun checkForPermissions(permission:String, requestCode:Int, activityWithPermission: () -> Unit){
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
                    onPermissionGranted = activityWithPermission
                    /*Request for the user permission to access certain documents and features of the device*/
                    when(requestCode){
                        FILE_REQUEST_CODE -> readExternalFilesPermission.launch(permission)
                        CAMERA_REQUEST_CODE -> accessCameraPermission.launch(permission)
                    }
                }
            }
        }else{ launchActivityWithPermission(requestCode) }
    }

    companion object{
        const val FILE_REQUEST_CODE = 101
        const val CAMERA_REQUEST_CODE = 102
        const val READ_EXTERNAL_FILES_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        const val CAMERA_ACCESS_PERMISSION = android.Manifest.permission.CAMERA
    }
}