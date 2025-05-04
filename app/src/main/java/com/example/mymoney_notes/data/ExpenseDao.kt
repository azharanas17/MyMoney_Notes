package com.example.mymoney_notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses WHERE userId = :userId")
    suspend fun getExpenses(userId: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND type = :type")
    suspend fun getExpensesByType(userId: String, type: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getExpensesByDateRange(userId: String, startDate: Long, endDate: Long): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND category = :category")
    suspend fun getExpensesByCategory(userId: String, category: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND category = :category AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getExpensesByCategoryAndDateRange(userId: String, category: String, startDate: Long, endDate: Long): List<Expense>

    @Query("SELECT category, SUM(CASE WHEN type = 'expense' THEN amount ELSE -amount END) as total FROM expenses WHERE userId = :userId GROUP BY category")
    suspend fun getCategoryTotals(userId: String): List<CategoryTotal>

    @Query("SELECT DISTINCT category FROM expenses WHERE userId = :userId")
    suspend fun getDistinctCategories(userId: String): List<String>
}
