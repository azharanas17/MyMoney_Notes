package com.example.mymoney_notes.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Category::class, Expense::class, Goal::class, User::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            Log.d("AppDatabase", "Initializing database")
            val startTime = System.currentTimeMillis()
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "cashify_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    Log.d("AppDatabase", "Database initialized in ${System.currentTimeMillis() - startTime}ms")
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error initializing database: $e")
                    throw RuntimeException("Failed to initialize database", e)
                }
            }
        }
    }
}