package com.example.pricerecorder.database

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.pricerecorder.Converters
import io.reactivex.Single

@Dao
interface ProductDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(product: Product)

    @TypeConverters(Converters::class)
    @Query("UPDATE products_table SET update_date = :date, price = :price, price_history = :hist WHERE productId = :id")
    suspend fun update(id:Long,price:Double,date:String,hist:Pair<Double,String>) : Int

    @TypeConverters(Converters::class)
    @Query("UPDATE products_table SET description=:des,place_of_purchase=:place,category=:cat,product_img=:img WHERE productId = :id")
    suspend fun update(id: Long,des:String,place:String,cat:String?,img:Bitmap?)

    @Delete
    suspend fun delete(product: Product)

    @Query("DELETE FROM products_table")
    suspend fun clearDb()

    @Query("SELECT * FROM products_table WHERE productId = :key")
    suspend fun get(key: Long) : Product?

    @Query("SELECT * FROM products_table ORDER BY update_date desc,description asc")
    fun getAllProducts() : LiveData<List<Product>>

    /*Query executed when the user chooses to backup the db. This checkpoint query ensures that all pending transactions are applied.
    A Single is similar to an Observable, but it always emits one value or an error notification*/
    @RawQuery
    fun checkPoint(supportSQLiteQuery : SupportSQLiteQuery) : Single<Int>
}