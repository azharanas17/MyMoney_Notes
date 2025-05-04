package com.example.mymoney_notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val month: String,
    val category: String,
    val categoryId: String,
    val type: String,
    val description: String,
    val photoPath: String,
    val minGoal: Double,
    val maxGoal: Double
)