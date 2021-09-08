package com.example.pricerecorder.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.DateFormat
import java.util.*

@Entity(tableName = "products_table")
data class Product(
    @ColumnInfo(name = "description")
    var description : String,

    @ColumnInfo(name = "price")
    var price : Double,

    @ColumnInfo(name = "place_of_purchase")
    var placeOfPurchase : String,

    @ColumnInfo(name = "image_url")
    var image : String? = null,

    @ColumnInfo(name = "update_date")
    var updateDate : String = "",

    @PrimaryKey(autoGenerate = true)
    var productId : Long =0L
){
    init {
        updateDate = setUpdateDate()
    }

    private fun setUpdateDate(): String {
        return DateFormat.getDateInstance().format(Calendar.getInstance().time)
    }
}