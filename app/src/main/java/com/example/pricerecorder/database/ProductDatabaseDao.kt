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

    /*Returns all products that contain the query as a substring in any part of their description*/
    @Query("SELECT * FROM products_table WHERE description LIKE '%'||:query||'%' ORDER BY update_date desc,description asc")
    fun searchProductList(query: String) : List<Product>

    /*This function is used to search in the list when there has been a previous filter|*/
    @Query("select * from products_table where description like '%'||:query||'%' and" +
            "(IFNULL(:catFilter,'uncategorized') = IFNULL(category,'uncategorized') or :catFilter = '') and " +
            "(:placeFilter = '' or :placeFilter = place_of_purchase)" +
            "order by description asc")
    fun searchProductList(query: String,catFilter:String?,placeFilter:String) : List<Product>

    /*Selects all different categories associated to at least one product, in case there is one which its category is
    * null then its replaced with an arbitrary string*/
    @Query("SELECT DISTINCT coalesce(category,:noCategoryString) from products_table order by category asc")
    fun getListOfAllCategoriesRegistered(noCategoryString:String) : List<String>

    /*Selects all products that meet the given conditions. If any of the parameters is empty, then it should
    * not be considered when filtering the list*/
    @Query("select * from products_table where" +
            "(IFNULL(:catFilter,'uncategorized') = IFNULL(category,'uncategorized') or :catFilter = '') and " +
            "(:placeFilter = '' or :placeFilter = place_of_purchase)" +
            "order by update_date desc,description asc")
    fun filterProductList(catFilter:String?,placeFilter:String) : List<Product>

    /*Query executed when the user chooses to backup the db. This checkpoint query ensures that all pending transactions are applied.
    A Single is similar to an Observable, but it always emits one value or an error notification*/
    @RawQuery
    fun checkPoint(supportSQLiteQuery : SupportSQLiteQuery) : Single<Int>
}