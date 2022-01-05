package com.example.pricerecorder.homeFragment

import android.annotation.SuppressLint
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.example.pricerecorder.database.Product

/*Method responsible for making the right framework calls to establish certain values*/
@SuppressLint("SetTextI18n")
@BindingAdapter("productPriceString")
fun TextView.setProductPriceString(item : Product?){
    item?.let {
        text = "$${item.price}"
    }
}