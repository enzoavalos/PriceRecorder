package com.example.pricerecorder

import com.journeyapps.barcodescanner.CaptureActivity

/* This activity opens the camera and does the code scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.*/
class CaptureAct : CaptureActivity()