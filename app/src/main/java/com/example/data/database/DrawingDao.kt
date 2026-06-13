package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawingDao {
    @Query("SELECT * FROM drawings ORDER BY updatedAt DESC")
    fun getAllDrawings(): Flow<List<DrawingEntity>>

    @Query("SELECT * FROM drawings WHERE id = :id LIMIT 1")
    suspend fun getDrawingById(id: Long): DrawingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawing(drawing: DrawingEntity): Long

    @Update
    suspend fun updateDrawing(drawing: DrawingEntity)

    @Delete
    suspend fun deleteDrawing(drawing: DrawingEntity)

    @Query("DELETE FROM drawings WHERE id = :id")
    suspend fun deleteDrawingById(id: Long)
}
