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

    @Delete
    fun delete(product: Product)

    @Query("DELETE FROM products_table")
    fun clearDb()

    @Query("SELECT * FROM products_table WHERE productId = :key")
    fun get(key: Long) : Product?

    @Query("SELECT * FROM products_table ORDER BY update_date desc,description asc")
    fun getAllProducts() : LiveData<List<Product>>


    /*Selects all different values that contain the given query arg as a substring in any part of the text*/
    @Query("SELECT DISTINCT place_of_purchase FROM products_table WHERE place_of_purchase LIKE '%'||:query||'%' " +
            " ORDER BY place_of_purchase ASC")
    fun filterPlacesRegistered(query: String) : List<String>

    /*Query executed when the user chooses to backup the db. This checkpoint query ensures that all pending transactions are applied.
    A Single is similar to an Observable, but it always emits one value or an error notification*/
    @RawQuery
    fun checkPoint(supportSQLiteQuery : SupportSQLiteQuery) : Single<Int>
}