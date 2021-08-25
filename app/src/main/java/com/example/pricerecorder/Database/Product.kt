package com.example.pricerecorder.Database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "products_table")
data class Product(
    @PrimaryKey(autoGenerate = true)
    var productId : Long =0L,

    @ColumnInfo(name = "description")
    var description : String,

    @ColumnInfo(name = "price")
    var price : Double,

    @ColumnInfo(name = "place_of_purchase")
    var placeOfPurchase : String,

    @ColumnInfo(name = "upload_date")
    var date : String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
)