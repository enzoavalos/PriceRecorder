package com.example.pricerecorder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File

interface ImageUtils {
    companion object{
        private const val MAX_BITMAP_SIZE = 500000

        /*Receives an uri and returns the image corresponding to it in bitmap format*/
        fun getBitmapFromUri(context: Context, uri: Uri) : Bitmap?{
            var bitmap : Bitmap? = null
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.RGB_565
                /*Returns a bitmap that is 1/4 the width/height of the original, and 1/16 the number of pixels*/
                options.inSampleSize = 4
                bitmap = BitmapFactory.decodeStream(inputStream,null,options)
                inputStream?.close()
            }catch (e:Exception){
                Log.v("AddFragment",e.toString())
            }
            return bitmap
        }

        /*Creates a temporary file where to store the picture taken by the camera*/
        fun createTemporaryImageFile(context: Context) : File {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            return File.createTempFile("temp_image",".jpg",storageDir)
        }

        fun getModifiedBitmap(context: Context,bitmap: Bitmap,uri: Uri) : Bitmap{
            val modified = rotateImageIfNecessary(uri,bitmap,context) ?: bitmap
            return scaleDownBitmapIfNecessary(modified)
        }

        /*Receives an image in bitmap format and its corresponding uri and gets its orientation via the image exif info.
        * If the orientation is known and different from normal, a new bitmap with the right orientation is created*/
        private fun rotateImageIfNecessary(uri: Uri, bitmap: Bitmap, context: Context) : Bitmap?{
            var rotated: Bitmap? = null
            val inputStream = context.contentResolver.openInputStream(uri)
            if(inputStream != null){
                try {
                    val orientation = ExifInterface(inputStream).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL)
                    var rotate = 0f
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270f
                    }
                    if(rotate != 0f){
                        val matrix = Matrix()
                        matrix.postRotate(rotate)
                        rotated = Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
                    }
                }catch (e:Exception){
                    Log.v("AddFragment",e.toString())
                }finally {
                    inputStream.close()
                }
            }
            return rotated
        }

        /*Resizes a given bitmap to 70% of its original size if the latter surpasses 0.5MB*/
        private fun scaleDownBitmapIfNecessary(bitmap: Bitmap) : Bitmap {
            return if(bitmap.byteCount >= MAX_BITMAP_SIZE)
                Bitmap.createScaledBitmap(bitmap,(bitmap.width * 0.7).toInt(),(bitmap.height * 0.7).toInt(),true)
            else
                bitmap
        }
    }
}