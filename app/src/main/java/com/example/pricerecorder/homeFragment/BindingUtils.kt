package com.example.pricerecorder.homeFragment

import android.annotation.SuppressLint
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.example.pricerecorder.database.Product

/*Metodo responsable de realizar las llamadas de framework necesarias para establecer valores*/
@SuppressLint("SetTextI18n")
@BindingAdapter("productPriceString")
fun TextView.setProductPriceString(item : Product?){
    item?.let {
        text = "$${item.price}"
    }
}