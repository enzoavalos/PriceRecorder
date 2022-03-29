package com.example.pricerecorder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream
import java.lang.StringBuilder

class Converters {
    /*Function to convert from a bitmap to a byte array, used to store an image in the Room DB*/
    @TypeConverter
    fun fromBitmap(bitmap: Bitmap?):ByteArray{
        if(bitmap == null)
            return ByteArray(0)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        return outputStream.toByteArray()
    }

    @TypeConverter
    fun fromByteArray(array: ByteArray) : Bitmap?{
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return if(array.isNotEmpty()) BitmapFactory.decodeByteArray(array,0,array.size,options) else null
    }

    @TypeConverter
    fun fromPair(p : Pair<Double,String>) : String{
        return StringBuilder().append("${p.first}:${p.second}").toString()
    }

    @TypeConverter
    fun fromString(a:String) : Pair<Double,String>{
        var start = 0  //inclusive
        var end : Int  //exclusive
        var counter = 0
        var subString = ""
        var b = ""
        a.forEach{
            if(it == ':'){
                end = counter
                subString = a.subSequence(start,end) as String
                start = end+1
                b = a.subSequence(start,a.length) as String
                return@forEach
            }
            counter +=1
        }
        return Pair(subString.toDouble(),b)
    }
}