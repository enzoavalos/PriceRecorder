package com.example.pricerecorder.filters

import com.example.pricerecorder.database.Product

class AndFilter(
    private var filter1: Filter,
    private var filter2: Filter) : Filter {

    fun setFilter1(newFilter: Filter){
        filter1 = newFilter
    }

    fun setFilter2(newFilter: Filter){
        filter2 = newFilter
    }

    override fun meetsCriteria(product: Product): Boolean {
        return filter1.meetsCriteria(product) and filter2.meetsCriteria(product)
    }
}