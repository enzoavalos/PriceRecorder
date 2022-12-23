package com.example.pricerecorder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.setUserPreferredThemeMode(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}