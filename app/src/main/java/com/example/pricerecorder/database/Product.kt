package com.example.pricerecorder.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products_table")
data class Product(
    @ColumnInfo(name = "description")
    var description : String,

    @ColumnInfo(name = "price")
    var price : Double,

    @ColumnInfo(name = "place_of_purchase")
    var placeOfPurchase : String,

    @ColumnInfo(name = "image_url")
    var image : String,

    @ColumnInfo(name = "update_date")
    var updateDate : String,

    @PrimaryKey(autoGenerate = true)
    var productId : Long =0L,
    /*@ColumnInfo(name = "upload_date")
    var date : String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())*/
)