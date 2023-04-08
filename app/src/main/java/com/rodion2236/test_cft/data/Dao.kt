package com.rodion2236.test_cft.data

import androidx.lifecycle.LiveData
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@androidx.room.Dao
interface Dao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItem(item: DataBaseItem)

    @Query("SELECT name FROM request")
    suspend fun requestList() : Array<String>

    @Query("SELECT * FROM request WHERE name LIKE :name")
    fun getItem(name: String) : LiveData<DataBaseItem>
}