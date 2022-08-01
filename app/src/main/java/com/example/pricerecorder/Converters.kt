package com.example.pricerecorder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream

class Converters {
    /*Function to convert from a bitmap to a byte array, used to store an image in the Room DB*/
    @Suppress("DEPRECATION")
    @TypeConverter
    fun fromBitmap(bitmap: Bitmap?):ByteArray{
        if(bitmap == null)
            return ByteArray(0)
        val outputStream = ByteArrayOutputStream()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 25, outputStream)
        else
            bitmap.compress(Bitmap.CompressFormat.WEBP, 25, outputStream)
        return outputStream.toByteArray()
    }

    @TypeConverter
    fun fromByteArray(array: ByteArray) : Bitmap?{
        return if(array.isNotEmpty()) BitmapFactory.decodeByteArray(array,0,array.size) else null
    }

    @TypeConverter
    fun fromLong(date:Long) : String{
        return DateUtils.formatDate(date)
    }

    @TypeConverter
    fun fromString(date:String) : Long{
        return date.toLong()
    }
}