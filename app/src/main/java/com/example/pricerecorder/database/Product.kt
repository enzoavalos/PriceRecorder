package com.example.pricerecorder.database

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
    var category : String?,

    @ColumnInfo(name = "update_date")
    var updateDate : String,

    @ColumnInfo(name = "price_history")
    var priceHistory : MutableList<Pair<Double,String>> = mutableListOf(),

    @PrimaryKey(autoGenerate = true)
    var productId : Long =0L
){
    init {
        priceHistory.add(Pair(price,updateDate))
    }

    fun updatePrice(newPrice:Double){
        price = newPrice
        updateDate = setUpdateDate()
        priceHistory.add(Pair(newPrice,updateDate))
    }

    fun updateData(des:String,place:String,cat:String){
        description = des
        placeOfPurchase = place
        category = cat
    }

    companion object{
        fun setUpdateDate(): String {
            return DateFormat.getDateInstance().format(Calendar.getInstance().time)
        }

        val categories = listOf("Comestibles","Limpieza","Hogar","Bebidas","Mascotas","Jardineria","Cuidado personal",
            "Verduleria","Lacteos","Panaderia")
    }
}