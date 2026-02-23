package com.example.composetutorial.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    val id: Int = 0,
    
    @SerializedName("name")
    val name: String?,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("date")
    val date: String?, // Keep as String for simplicity or convert to Date
    
    @SerializedName("flag")
    val flag: Boolean,
    
    @SerializedName("value")
    val value: Int,
    
    var isDirty: Boolean = false, // For offline sync status
    
    @SerializedName("is_deleted")
    var isDeleted: Boolean = false, // For offline delete syncing
    
    var isCreatedLocally: Boolean = false // To distinguish Create vs Update during sync
)
