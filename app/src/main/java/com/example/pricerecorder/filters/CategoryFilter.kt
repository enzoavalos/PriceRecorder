package com.example.pricerecorder.filters

import com.example.pricerecorder.database.Product

/*Class that implements Filter interface used to filter products by category*/
class CategoryFilter(
    var category: String = "",
    private val noCategoryString:String) : Filter {

    override fun meetsCriteria(product: Product): Boolean {
        if(category.isEmpty())
            return true
        return product.getCategory().let {
            if(category == noCategoryString)
                it.isEmpty()
            else
                (it == category)
        }
    }
}