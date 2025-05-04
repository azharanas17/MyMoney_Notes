package com.example.mymoney_notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GoalDao {
    @Insert
    suspend fun insert(goal: Goal)

    @Query("SELECT * FROM goals WHERE userId = :userId")
    suspend fun getGoals(userId: String): List<Goal>
}