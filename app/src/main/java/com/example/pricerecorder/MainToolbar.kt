package com.example.pricerecorder

import androidx.appcompat.app.AppCompatActivity

class MainToolbar {
    companion object{
        fun show(activity : AppCompatActivity, title:String, upButton:Boolean){
            activity.setSupportActionBar(activity.findViewById(R.id.action_bar_toolbar))
            activity.supportActionBar?.title = title
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(upButton)
        }

        fun setUpButton(activity: AppCompatActivity,upButton: Boolean){
            activity.supportActionBar?.setDisplayShowHomeEnabled(upButton)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(upButton)
        }

        fun setTitle(activity: AppCompatActivity,title: String){
            activity.supportActionBar?.title = title
        }
    }
}