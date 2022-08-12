package com.example.pricerecorder.database

import android.graphics.Bitmap
import androidx.room.*
import com.example.pricerecorder.Converters
import com.example.pricerecorder.DateUtils
import java.lang.Exception

@Entity(tableName = "products_table")
data class Product(
    @ColumnInfo(name = "description")
    private var description : String,

    @ColumnInfo(name = "price")
    private var price : Double,

    @ColumnInfo(name = "place_of_purchase")
    private var placeOfPurchase : String,

    @ColumnInfo(name = "category")
    private var category : String,

    @ColumnInfo(name = "update_date")
    private var updateDate : Long = DateUtils.getCurrentDate(),

    @ColumnInfo(name = "product_size")
    private var size : String,

    @ColumnInfo(name = "product_quantity")
    private var quantity : String,

    @ColumnInfo(name = "product_img")
    private var image : Bitmap? = null,

    @PrimaryKey(autoGenerate = true)
    private var productId : Long =0L
){
    fun updatePrice(newPrice:Double){
        price = newPrice
        updateDate = DateUtils.getCurrentDate()
    }

    fun updateData(des:String,place:String,cat:String,img:Bitmap?,newPrice:Double){
        description = des
        placeOfPurchase = place
        category = cat
        image = img
        price = newPrice
    }

    fun getProductId() : Long{
        return  this.productId
    }

    fun getQuantity() : String{
        return  this.quantity
    }

    fun getSize() : String{
        return  this.size
    }

    fun getPlaceOfPurchase(): String {
        return this.placeOfPurchase
    }

    fun getPrice(): Double {
        return this.price
    }

    fun getDescription(): String {
        return this.description
    }

    fun getCategory(): String {
        return this.category
    }

    fun getUpdateDate(): Long {
        return this.updateDate
    }

    fun getImage() : Bitmap?{
        return this.image
    }

    fun getId(): Long{
        return this.productId
    }

    override fun equals(other: Any?): Boolean {
        return try {
            val aux = other as Product
            ((this.description == aux.getDescription()) and (this.placeOfPurchase == aux.getPlaceOfPurchase()))
        }catch (e : Exception){
            false
        }
    }

    override fun hashCode(): Int {
        var result = description.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + placeOfPurchase.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + updateDate.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + productId.hashCode()
        return result
    }
}