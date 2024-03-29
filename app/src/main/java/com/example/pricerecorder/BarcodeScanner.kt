package com.example.pricerecorder

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class BarcodeScanner(
    private val context: Context,
    registry: ActivityResultRegistry) {
    private var onScanSuccess : (String) -> Unit = {}

    /*Registers a scan contract, launches the Capture activity and receives a string as a return value*/
    private val barcodeReaderLauncher = registry.register("key",
        ScanContract()){
        if(it.contents != null){
            onScanSuccess(it.contents)
        }
    }

    /*Firstly requests camera access if not already given, and after that it launches the registered activity above*/
    fun scanCode(
        permissionChecker: PermissionChecker,
        onSuccessCallback:(String) -> Unit){
        permissionChecker.checkForPermissions(
            PermissionChecker.CAMERA_ACCESS_PERMISSION,
            PermissionChecker.CAMERA_REQUEST_CODE
        ) {
            onScanSuccess = onSuccessCallback
            val options = ScanOptions()
            options.apply {
                setPrompt(context.getString(R.string.scan_barcode_prompt))
                setBeepEnabled(true)
                setOrientationLocked(true)
                captureActivity = CaptureAct::class.java
            }
            barcodeReaderLauncher.launch(options)
        }
    }
}