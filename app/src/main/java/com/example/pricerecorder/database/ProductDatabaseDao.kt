package com.example.pricerecorder.database

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import io.reactivex.Single

@Dao
interface ProductDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(product: Product)

    @Update
    fun update(product: Product)

    @Query("UPDATE products_table SET update_date = :date, price = :price WHERE productId = :id")
    fun update(id:Long,price:Double,date:Long) : Int

    @Query("UPDATE products_table SET description=:des,place_of_purchase=:place,category=:cat,product_img=:img WHERE productId = :id")
    fun update(id: Long,des:String,place:String,cat:String?,img:Bitmap?)

    @Delete
    fun delete(product: Product)

    @Query("DELETE FROM products_table")
    fun clearDb()

    @Query("SELECT * FROM products_table WHERE productId = :key")
    fun get(key: Long) : Product?

    @Query("SELECT * FROM products_table ORDER BY update_date desc,description asc")
    fun getAllProducts() : LiveData<List<Product>>

    /*Query executed when the user chooses to backup the db. This checkpoint query ensures that all pending transactions are applied.
    A Single is similar to an Observable, but it always emits one value or an error notification*/
    @RawQuery
    fun checkPoint(supportSQLiteQuery : SupportSQLiteQuery) : Single<Int>
}