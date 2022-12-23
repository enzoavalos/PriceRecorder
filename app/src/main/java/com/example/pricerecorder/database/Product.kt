package com.example.pricerecorder.database

import android.content.Context
import android.graphics.Bitmap
import androidx.room.*
import com.example.pricerecorder.DateUtils
import com.example.pricerecorder.R
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
    private var category : String? = null,

    @ColumnInfo(name = "update_date")
    private var updateDate : Long = DateUtils.getCurrentDate(),

    @ColumnInfo(name = "product_size")
    private var size : String,

    @ColumnInfo(name = "product_quantity")
    private var quantity : String,

    @ColumnInfo(name = "product_barcode")
    private var barcode: String="",

    @ColumnInfo(name = "product_img")
    private var image : Bitmap? = null,

    @PrimaryKey(autoGenerate = true)
    private var productId : Long =0L
){
    fun updatePrice(newPrice:Double){
        price = newPrice
        updateDate = DateUtils.getCurrentDate()
    }

    fun updateData(
        des: String,
        place: String,
        cat: String?,
        img: Bitmap?,
        newPrice: Double,
        size: String,
        quantity: String,
        barcode: String
    ){
        description = des
        placeOfPurchase = place
        category = cat
        image = img
        price = newPrice
        updateDate = DateUtils.getCurrentDate()
        this.size = size
        this.quantity = quantity
        this.barcode = barcode
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

    fun getCategory(): String? {
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

    fun getBarcode(): String{
        return this.barcode
    }

    override fun equals(other: Any?): Boolean {
        return try {
            val aux = other as Product
            this.hashCode() == aux.hashCode()
        }catch (e : Exception){
            false
        }
    }

    override fun hashCode(): Int {
        var result = description.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + placeOfPurchase.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + quantity.hashCode()
        result = 31 * result + barcode.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + productId.hashCode()
        return result
    }

    fun formatToPlainText(context: Context):String{
        var output = "$description\n"
        output += "${context.getString(R.string.purchase_place_label)}: $placeOfPurchase\n"
        this.category?.let { output += "${context.getString(R.string.category_label)}: $it\n" }
        this.quantity.also {
            if(it.isNotEmpty()) output += "${context.getString(R.string.product_quantity_label)}: $it\n" }
        this.size.also {
            if(it.isNotEmpty()) output += "${context.getString(R.string.product_size_label)}: $it\n"
        }
        this.barcode.also {
            if(it.isNotEmpty()) output += "${context.getString(R.string.product_barcode_label)}: $it\n"
        }
        output += "${context.getString(R.string.price_label)}: $$price"

        return output
    }
}