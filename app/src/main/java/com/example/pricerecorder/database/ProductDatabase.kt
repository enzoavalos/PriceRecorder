package com.example.pricerecorder.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase


@Database(entities = [Product::class],version = 1,exportSchema = false)
abstract class ProductDatabase:RoomDatabase() {
    abstract val productDatabaseDao: ProductDatabaseDao

    companion object {
        /*Instance keeps a reference to any DB returned by getInstance, that way, unless needed, the DB will be instantiated only once.
        * Being volatile, the value of the variable will never be cached, meaning all writes and reads will be performed from the main
        * memory, so all changes made by one thread, will be visible to other threads*/
        @Volatile
        private var INSTANCE: ProductDatabase? = null

        fun getInstance(context: Context): ProductDatabase {
            /*Multiple threads may ask for th DB simultaneously, with synchronized we ensure it is initialized just once*/
            synchronized(this) {
                // Copy the current value of INSTANCE to a local variable so Kotlin can smart cast.
                var instance = INSTANCE
                // If instance is `null` make a new database instance.
                if (instance == null) {
                    instance = databaseBuilder(
                        context.applicationContext,
                        ProductDatabase::class.java,
                        "products_database"
                    )
                        /*This option makes the db to be stored in a single file*/
                        .setJournalMode(JournalMode.TRUNCATE)
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                // Return instance; smart cast to be non-null.
                return instance
            }
        }
    }
}