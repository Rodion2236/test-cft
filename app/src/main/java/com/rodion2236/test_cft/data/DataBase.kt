package com.rodion2236.test_cft.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DataBaseItem::class],
    version = 2
)
abstract class DataBase : RoomDatabase() {

    abstract fun getDao(): Dao

    companion object {
        fun getDb(context: Context): DataBase {
            return Room.databaseBuilder(
                context.applicationContext,
                DataBase::class.java,
                "database.bin"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}