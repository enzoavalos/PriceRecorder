package com.example.pricerecorder.Database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ProductDatabaseDao {
    @Insert
    suspend fun insert(product:Product)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("DELETE FROM products_table")
    suspend fun clearDb()

    @Query("SELECT * FROM products_table WHERE productId = :key")
    suspend fun get(key: Long) : Product?

    @Query("SELECT * FROM products_table ORDER BY upload_date desc")
    suspend fun getAllProducts() : LiveData<List<Product>>
}