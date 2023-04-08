package com.rodion2236.test_cft.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "request",
    indices = [
        Index(value = ["name"],
            unique = true)
    ]
)
data class DataBaseItem(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,

    @ColumnInfo(name = "name")
    var name: String,
)