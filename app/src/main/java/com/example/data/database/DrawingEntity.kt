package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drawings")
data class DrawingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val strokesJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
