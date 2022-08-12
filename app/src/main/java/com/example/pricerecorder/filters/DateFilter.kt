package com.example.pricerecorder.filters

import com.example.pricerecorder.DateUtils
import com.example.pricerecorder.database.Product

class DateFilter(var date:Long? = null) : Filter{

    override fun meetsCriteria(product: Product): Boolean {
        if(date == null)
            return true

        return product.getUpdateDate().let {
            DateUtils.formatDate(it) == DateUtils.formatDate(date!!)
        }
    }
}