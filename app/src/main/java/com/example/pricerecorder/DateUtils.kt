package com.example.pricerecorder

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

interface DateUtils {
    companion object{
        private val timezone = TimeZone.getDefault()

        /*Receives a date represented as a Long value and returns the date in string format*/
        fun formatDate(dateInMillis:Long) : String{
            val utcTime = Date(dateInMillis)
            val format = "yyy/MM/dd HH:mm:ss"
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            sdf.timeZone = timezone
            val gmtTime = SimpleDateFormat(format, Locale.getDefault()).parse(sdf.format(utcTime))
            return if(gmtTime != null) DateFormat.getDateInstance().format(gmtTime) else ""
        }

        fun getCurrentDate(): Long {
            return Calendar.getInstance(timezone, Locale.getDefault()).timeInMillis
        }
    }
}