package com.example.pricerecorder.filters

import com.example.pricerecorder.database.Product

interface Filter {
    fun meetsCriteria(product: Product) : Boolean
}