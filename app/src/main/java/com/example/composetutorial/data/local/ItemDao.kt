package com.example.composetutorial.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.composetutorial.data.model.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE isDeleted = 0")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE isDirty = 1 OR isDeleted = 1")
    suspend fun getDirtyItems(): List<Item>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Item>)

    @Update
    suspend fun updateItem(item: Item)

    @Query("DELETE FROM items")
    suspend fun clearAll()

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Int)
}
