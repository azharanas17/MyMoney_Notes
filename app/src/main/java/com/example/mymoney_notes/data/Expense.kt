package com.example.mymoney_notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double,
    val category: String,
    val categoryId: String,
    val type: String,
    val date: String,
    val timestamp: Long,
    val startTime: String,
    val endTime: String,
    val description: String,
    val photoPath: String // Local file path
)