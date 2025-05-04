package com.example.mymoney_notes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.example.mymoney_notes.data.AppDatabase
import com.example.mymoney_notes.data.Expense
import com.example.mymoney_notes.databinding.ActivityCategoriesExpensesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CategoryExpensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesExpensesBinding
    private lateinit var auth: FirebaseAuth
    private var database: AppDatabase? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private lateinit var category: String
    private var startDate: Long? = null
    private var endDate: Long? = null
    private lateinit var expenseAdapter: ExpenseAdapter
    private var currentFilter: String = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityCategoriesExpensesBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("CategoryExpensesActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            Log.e("CategoryExpensesActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Category Expenses", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("CategoryExpensesActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        category = intent.getStringExtra("category")?.trim() ?: ""
        if (category.isEmpty()) {
            Log.e("CategoryExpensesActivity", "No category provided in intent")
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.tvTitle.text = "$category Expenses"
        Log.d("CategoryExpensesActivity", "Category set: $category")

        setupRecyclerView()
        setupBottomNavigation()
        setupChipFilters()

        // Initialize database, log categories, and load expenses
        lifecycleScope.launch {
            try {
                database = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@CategoryExpensesActivity)
                }
                Log.d("CategoryExpensesActivity", "Database initialized")
                // Log all categories for debugging
                val userId = auth.currentUser?.uid ?: return@launch
                val categories = withContext(Dispatchers.IO) {
                    database?.expenseDao()?.getDistinctCategories(userId) ?: emptyList()
                }
                Log.d("CategoryExpensesActivity", "Available categories: $categories")
                loadExpenses()
            } catch (e: Exception) {
                Log.e("CategoryExpensesActivity", "Error initializing database: $e")
                Toast.makeText(this@CategoryExpensesActivity, "Error accessing database", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(mutableListOf()) { expense ->
            try {
                if (expense.photoPath.isNotEmpty()) {
                    val intent = Intent(this@CategoryExpensesActivity, ViewPhotoActivity::class.java)
                    intent.putExtra("photoPath", expense.photoPath)
                    startActivity(intent)
                    Log.d("CategoryExpensesActivity", "Launching ViewPhotoActivity for expense ${expense.id}")
                }
            } catch (e: Exception) {
                Log.e("CategoryExpensesActivity", "Error opening photo for expense ${expense.id}: $e")
                Toast.makeText(this@CategoryExpensesActivity, "Error viewing photo", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@CategoryExpensesActivity)
            adapter = expenseAdapter
            setHasFixedSize(true)
        }
        Log.d("CategoryExpensesActivity", "RecyclerView set up")
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_categories
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    try {
                        startActivity(Intent(this, MainActivity::class.java))
                        Log.d("CategoryExpensesActivity", "Navigating to MainActivity")
                        true
                    } catch (e: Exception) {
                        Log.e("CategoryExpensesActivity", "Error navigating to MainActivity: $e")
                        Toast.makeText(this, "Error opening Home", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.nav_categories -> {
                    try {
                        startActivity(Intent(this, CategoriesActivity::class.java))
                        Log.d("CategoryExpensesActivity", "Navigating to CategoriesActivity")
                        true
                    } catch (e: Exception) {
                        Log.e("CategoryExpensesActivity", "Error navigating to CategoriesActivity: $e")
                        Toast.makeText(this, "Error opening Categories", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.nav_goals -> {
                    try {
                        startActivity(Intent(this, GoalsActivity::class.java))
                        Log.d("CategoryExpensesActivity", "Navigating to GoalsActivity")
                        true
                    } catch (e: Exception) {
                        Log.e("CategoryExpensesActivity", "Error navigating to GoalsActivity: $e")
                        Toast.makeText(this, "Error opening Goals", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun setupChipFilters() {
        binding.chipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (group.findViewById<Chip>(checkedId)?.id) {
                R.id.chipAll -> {
                    currentFilter = "all"
                    startDate = null
                    endDate = null
                    binding.chipDay.text = "Pick Date"
                    Log.d("CategoryExpensesActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
                R.id.chipDay -> {
                    currentFilter = "day"
                    showDateRangePicker()
                }
                else -> {
                    currentFilter = "all"
                    startDate = null
                    endDate = null
                    binding.chipDay.text = "Pick Date"
                    Log.d("CategoryExpensesActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
            }
        }
    }

    private fun showDateRangePicker() {
        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            startDate = selection.first
            endDate = selection.second
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = endDate!!
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            endDate = calendar.timeInMillis
            val startDateStr = dateFormat.format(Date(startDate!!))
            val endDateStr = dateFormat.format(Date(endDate!!))
            binding.chipDay.text = "$startDateStr - $endDateStr"
            Log.d("CategoryExpensesActivity", "Date range selected: $startDateStr to $endDateStr (timestamps: $startDate to $endDate)")
            loadExpenses()
        }
        datePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val db = database
            if (db == null) {
                Log.e("CategoryExpensesActivity", "Database not initialized in loadExpenses")
                Toast.makeText(this@CategoryExpensesActivity, "Database error", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                val expenses = withContext(Dispatchers.IO) {
                    when (currentFilter) {
                        "day" -> {
                            if (startDate != null && endDate != null) {
                                Log.d("CategoryExpensesActivity", "Querying expenses for userId=$userId, category=$category, timestamp=$startDate to $endDate")
                                db.expenseDao().getExpensesByCategoryAndDateRange(
                                    userId,
                                    category,
                                    startDate!!,
                                    endDate!!
                                )
                            } else {
                                emptyList()
                            }
                        }
                        else -> {
                            Log.d("CategoryExpensesActivity", "Querying all expenses for userId=$userId, category=$category")
                            db.expenseDao().getExpensesByCategory(userId, category)
                        }
                    }
                }.toMutableList()
                expenseAdapter.updateExpenses(expenses)
                Log.d("CategoryExpensesActivity", "Fetched expenses: ${expenses.map { it.id to it.date }}")
                if (expenses.isEmpty()) {
                    Toast.makeText(this@CategoryExpensesActivity, "No expenses found for $category in selected range", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CategoryExpensesActivity", "Error loading expenses: $e")
                Toast.makeText(this@CategoryExpensesActivity, "Error loading expenses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("CategoryExpensesActivity", "onResume called")
        database?.let {
            loadExpenses()
        } ?: Log.w("CategoryExpensesActivity", "Database not initialized in onResume")
    }
}
