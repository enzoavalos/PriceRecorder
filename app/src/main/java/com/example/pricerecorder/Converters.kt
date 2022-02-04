package com.example.pricerecorder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream
import java.lang.StringBuilder

/*Class used to convert from a list of pairs that represent a price and a date, to a string and vice versa*/
class Converters {
    @TypeConverter
    fun fromString(a : String) : MutableList<Pair<Double,String>>{
        var start = 0  //inclusive
        var end : Int  //exclusive
        val list : MutableList<Pair<Double,String>> = mutableListOf()

        if(a.contains(";")){
            var counter = 0
            var subString : String
            a.forEach{
                if(it == ';'){
                    end = counter
                    subString = a.subSequence(start,end) as String
                    val p = stringToPair(subString)
                    list.add(p)
                    start = end+1
                }
                counter +=1
            }
        }
        return list
    }

    @TypeConverter
    fun fromPairList(list : MutableList<Pair<Double,String>>) : String{
        val builder = StringBuilder()
        list.forEach{
            builder.append("${it.first}:${it.second};")
        }
        return builder.toString()
    }

    private fun stringToPair(a:String) : Pair<Double,String>{
        var start = 0
        var end : Int
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

    /*Function to convert from a bitmap to a byte array*/
    @TypeConverter
    fun fromBitmap(bitmap: Bitmap?):ByteArray{
        if(bitmap == null)
            return ByteArray(0)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,85,outputStream)
        return outputStream.toByteArray()
    }

    @TypeConverter
    fun fromByteArray(array: ByteArray) : Bitmap?{
        return if(array.isNotEmpty()) BitmapFactory.decodeByteArray(array,0,array.size) else null
    }
}