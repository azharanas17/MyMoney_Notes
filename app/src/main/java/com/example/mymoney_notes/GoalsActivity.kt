package com.example.mymoney_notes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.example.mymoney_notes.data.AppDatabase
import com.example.mymoney_notes.data.Expense
import com.example.mymoney_notes.data.Goal
import com.example.mymoney_notes.databinding.ActivityGoalsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class GoalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoalsBinding
    private lateinit var auth: FirebaseAuth
    private var database: AppDatabase? = null
    private lateinit var goalAdapter: GoalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityGoalsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("GoalsActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            Log.e("GoalsActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Goals page", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("GoalsActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        // Setup RecyclerView before coroutine to avoid layout warning
        setupRecyclerView()

        lifecycleScope.launch {
            try {
                database = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@GoalsActivity)
                }
                Log.d("GoalsActivity", "Database initialized")
                loadGoals()
            } catch (e: Exception) {
                Log.e("GoalsActivity", "Error initializing database: $e")
                Toast.makeText(this@GoalsActivity, "Error accessing database", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        binding.btnAddGoal.setOnClickListener {
            Log.d("GoalsActivity", "Add New Goal clicked")
            startActivity(Intent(this, AddGoalActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            Log.d("GoalsActivity", "Logout clicked")
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("GoalsActivity", "Navigating to MainActivity")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    Log.d("GoalsActivity", "Navigating to AddExpenseActivity")
                    startActivity(Intent(this, CategoriesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_goals -> {
                    Log.d("GoalsActivity", "Goals tab selected")
                    true
                }
                else -> false
            }
        }
        binding.bottomNav.menu.findItem(R.id.nav_goals)?.isChecked = true
    }

    private fun setupRecyclerView() {
        goalAdapter = GoalAdapter()
        binding.rvGoals.apply {
            layoutManager = LinearLayoutManager(this@GoalsActivity)
            adapter = goalAdapter
        }
        Log.d("GoalsActivity", "RecyclerView set up")
    }

    private fun loadGoals() {
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val db = database
            if (db == null) {
                Log.e("GoalsActivity", "Database not initialized in loadGoals")
                Toast.makeText(this@GoalsActivity, "Database error", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                val goals = withContext(Dispatchers.IO) {
                    db.goalDao().getGoals(userId)
                }
                val expenses = withContext(Dispatchers.IO) {
                    db.expenseDao().getExpenses(userId)
                }
                val goalItems = goals.map { goal ->
                    val totalSpent = expenses
                        .filter { expense ->
                            expense.category == goal.category &&
                                    expense.type == goal.type &&
                                    extractMonthYear(expense.date) == goal.month
                        }
                        .sumOf { it.amount }
                    GoalItem(goal, totalSpent)
                }
                goalAdapter.submitList(goalItems)
                Log.d("GoalsActivity", "Goals loaded: $goalItems")
            } catch (e: Exception) {
                Log.e("GoalsActivity", "Error loading goals: $e")
                Toast.makeText(this@GoalsActivity, "Error loading goals", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun extractMonthYear(date: String): String {
        return try {
            val sdfInput = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sdfOutput = SimpleDateFormat("MM/yyyy", Locale.getDefault())
            val parsedDate = sdfInput.parse(date)
            sdfOutput.format(parsedDate)
        } catch (e: Exception) {
            Log.e("GoalsActivity", "Error parsing date: $date", e)
            ""
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("GoalsActivity", "onResume called")
        database?.let {
            loadGoals()
        } ?: Log.w("GoalsActivity", "Database not initialized in onResume")
    }
}

data class GoalItem(val goal: Goal, val totalSpent: Double)