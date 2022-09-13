package com.example.pricerecorder.database

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import io.reactivex.Single

@Dao
interface ProductDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(product: Product)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(product: Product)

    @Query("DELETE FROM products_table WHERE productId = :productId")
    fun delete(productId:Long)

    @Query("DELETE FROM products_table")
    fun clearDb()

    /* Determines if a product is already registered in the db, comparing via its description and place of purchase
    * and optional product id, for updating scenarios */
    @Query("SELECT EXISTS (SELECT productId FROM products_table WHERE " +
            "description COLLATE NOCASE = :desc AND place_of_purchase COLLATE NOCASE = :place" +
            " AND productId != IFNULL(:productId,0) LIMIT 1)")
    fun checkExistence(desc:String,place:String,productId: Long? = null) : Int

    @Query("SELECT * FROM products_table WHERE productId = :key")
    fun get(key: Long) : Product?

    @Query("SELECT * FROM products_table ORDER BY update_date desc,description asc")
    fun getAllProducts() : LiveData<List<Product>>

    /*Selects all different values that contain the given query arg as a substring in any part of the text*/
    @Query("SELECT DISTINCT place_of_purchase FROM products_table WHERE place_of_purchase LIKE '%'||:query||'%' " +
            " ORDER BY place_of_purchase ASC")
    fun filterPlacesRegistered(query: String) : List<String>

    /*Performs a search and returns a list with the products matching a scanned barcode*/
    @Query("SELECT * FROM products_table WHERE product_barcode = :barcode")
    fun filterByBarcode(barcode:String): List<Product>

    /*Query executed when the user chooses to backup the db. This checkpoint query ensures that all pending transactions are applied.
    A Single is similar to an Observable, but it always emits one value or an error notification*/
    @RawQuery
    fun checkPoint(supportSQLiteQuery : SupportSQLiteQuery) : Single<Int>
}