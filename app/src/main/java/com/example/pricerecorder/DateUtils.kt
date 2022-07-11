package com.example.pricerecorder

import android.content.res.Resources
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

interface DateUtils {
    companion object{
        private val deviceLocale = Resources.getSystem().configuration.locales[0]
        private val timezone = TimeZone.getTimeZone("UTC")

        /*Receives a date represented as a Long value and returns the date in string format*/
        fun formatDate(dateInMillis:Long) : String{
            val utcTime = Date(dateInMillis)
            val format = "yyy/MM/dd HH:mm:ss"
            val sdf = SimpleDateFormat(format, deviceLocale)
            sdf.timeZone = timezone
            val gmtTime = SimpleDateFormat(format, deviceLocale).parse(sdf.format(utcTime))
            return if(gmtTime != null) DateFormat.getDateInstance().format(gmtTime) else ""
        }

        fun getCurrentDate(): Long {
            return Calendar.getInstance(timezone, deviceLocale).timeInMillis
        }
    }
}