package com.example.pricerecorder.database

import android.graphics.Bitmap
import androidx.room.*
import com.example.pricerecorder.Converters
import java.text.DateFormat
import java.util.*

@Entity(tableName = "products_table")
@TypeConverters(Converters::class)
data class Product(
    @ColumnInfo(name = "description")
    var description : String,

    @ColumnInfo(name = "price")
    var price : Double,

    @ColumnInfo(name = "place_of_purchase")
    var placeOfPurchase : String,

    @ColumnInfo(name = "category")
    var category : String,

    @ColumnInfo(name = "update_date")
    var updateDate : String,

    @ColumnInfo(name = "product_img")
    var image : Bitmap? = null,

    @ColumnInfo(name = "price_history")
    var priceHistory : Pair<Double,String> = Pair(price,updateDate),

    @PrimaryKey(autoGenerate = true)
    var productId : Long =0L
){
    fun updatePrice(newPrice:Double){
        priceHistory = Pair(price,updateDate)
        price = newPrice
        updateDate = getCurrentDate()
    }

    fun updateData(des:String,place:String,cat:String,img:Bitmap?){
        description = des
        placeOfPurchase = place
        category = cat
        image = img
    }

    companion object{
        fun getCurrentDate(): String {
            return DateFormat.getDateInstance().format(Calendar.getInstance().time)
        }
    }
}