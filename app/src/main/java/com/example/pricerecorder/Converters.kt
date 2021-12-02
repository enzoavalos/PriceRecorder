package com.example.pricerecorder

import androidx.room.TypeConverter

/*Class used to convert from a list of pairs that represent a price and a date, to a string and vice versa*/
class Converters {
    @TypeConverter
    fun fromString(a : String) : MutableList<Pair<Double,String>>{
        var start = 0  //inclusive
        var end : Int  //exclusive
        val list : MutableList<Pair<Double,String>> = ArrayList()

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
        var sequence = ""
        list.forEach{
            sequence += "${it.first}:${it.second};"
        }
        return sequence
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
}