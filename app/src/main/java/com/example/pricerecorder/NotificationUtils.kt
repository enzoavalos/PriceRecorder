package com.example.pricerecorder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

const val CHANNEL_ID = "CHANNEL_ID"
const val UPLOAD_NOTIFICATION_ID = 1
const val DOWNLOAD_NOTIFICATION_ID = 2

/*Creates a notification channel in APIs 26 and higher*/
fun createNotificationChannel(context: Context){
    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O){
        val name = "NotificationChannelName"
        val descriptionText = "NotificationChannelDescriptionText"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

/*Creates a notification with a progress bar.
* pending work attribute is the action to be performed while showing the notification, set progress is used to
* periodically update the current progress of the task being performed, and onComplete is used to set the notification
* to be shown when the task is completed, either successfully or not*/
fun createProgressNotification(
    context: Context,
    notificationId : Int,
    smallIcon:Int,
    title:String,
    text:String?,
    indefinite:Boolean,
    pendingWork:(
        setProgress:(Int)->Unit,
        onComplete:(String,Int?)->Unit) -> Unit
    ){
    val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
        setSmallIcon(smallIcon)
        setContentTitle(title)
        setContentText(text)
        priority = NotificationCompat.PRIORITY_DEFAULT
        setAutoCancel(true)
    }
    val progressMax = 100
    NotificationManagerCompat.from(context).apply {
        builder.setProgress(progressMax,0,indefinite)
        notify(notificationId,builder.build())

        Thread {
            pendingWork({ current_progress ->
                builder.setProgress(progressMax, current_progress, false)
            }){onCompleteText,onCompleteIcon ->
                /*When complete, max value must equal current to hide the progress bar*/
                val icon = onCompleteIcon ?: smallIcon
                builder
                    .setSmallIcon(icon)
                    .setContentText(onCompleteText)
                    .setProgress(0, 0, false)
                notify(notificationId, builder.build())
            }
        }.start()
    }
}