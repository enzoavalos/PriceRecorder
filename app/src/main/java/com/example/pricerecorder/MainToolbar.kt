package com.example.pricerecorder

import androidx.appcompat.app.AppCompatActivity

class MainToolbar {
    fun show(activity : AppCompatActivity, title:String, upButton:Boolean){
        activity.setSupportActionBar(activity.findViewById(R.id.action_bar_toolbar))
        activity.supportActionBar?.title = title
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(upButton)
    }
}