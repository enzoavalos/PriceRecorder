package com.example.pricerecorder

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

interface PermissionChecker {
    companion object{
        /*Check if permission has been granted to either access external files or the camera, and if its not
        * then it requests for it*/
        fun checkForPermissions(context: Context, permission:String, requestCode:Int, galleryPicker: () -> Unit,
                                photoTaker: () -> Unit, launchers:List<ActivityResultLauncher<String>>){
            fun launchActivityWithPermission(requestCode:Int){
                when(requestCode){
                    ImageUtils.FILE_REQUEST_CODE -> galleryPicker()
                    ImageUtils.CAMERA_REQUEST_CODE -> photoTaker()
                }
            }

            /*Checks if the sdk version is 23 or above*/
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                when(ContextCompat.checkSelfPermission(context,permission)){
                    PackageManager.PERMISSION_GRANTED -> { launchActivityWithPermission(requestCode) }
                    else -> {
                        /*Request for the user permission to access certain documents and features of the device*/
                        when(requestCode){
                            ImageUtils.FILE_REQUEST_CODE -> launchers[0].launch(permission)
                            ImageUtils.CAMERA_REQUEST_CODE -> launchers[1].launch(permission)
                        }
                    }
                }
            }else{ launchActivityWithPermission(requestCode) }
        }
    }
}