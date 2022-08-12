package com.example.pricerecorder.filters

import com.example.pricerecorder.database.Product

class PlaceFilter(var place:String = "") : Filter {

    override fun meetsCriteria(product: Product): Boolean {
        if(place.isEmpty())
            return true

        return product.getPlaceOfPurchase() == place
    }
}