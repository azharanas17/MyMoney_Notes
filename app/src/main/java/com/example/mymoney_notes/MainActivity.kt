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
import com.example.mymoney_notes.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private lateinit var expenseAdapter: ExpenseAdapter
    private var isDatabaseInitialized: Boolean = false
    private var currentFilter: String = "all"
    private var startDate: Long? = null
    private var endDate: Long? = null
    private var lastNavClickTime: Long = 0
    private val navDebounceDelay: Long = 500 // 500ms debounce
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("MainActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Main page", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("MainActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        // Set username
        val username = auth.currentUser?.email?.substringBefore("@") ?: "User"
        binding.tvWelcome.text = "Welcome @$username"
        Log.d("MainActivity", "Username set: $username")

        // Initialize RecyclerView
        expenseAdapter = ExpenseAdapter(mutableListOf()) { expense ->
            if (expense.photoPath.isNotEmpty()) {
                val intent = Intent(this, ViewPhotoActivity::class.java).apply {
                    putExtra("photoPath", expense.photoPath)
                }
                startActivity(intent)
                Log.d("MainActivity", "Launching ViewPhotoActivity for expense ${expense.id}")
            }
        }
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = expenseAdapter
        }

        // Initialize database and migrate timestamps
        lifecycleScope.launch {
            try {
                database = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@MainActivity)
                }
                isDatabaseInitialized = true
                Log.d("MainActivity", "Database initialized")
                // Fix existing timestamps
                val userId = auth.currentUser?.uid ?: return@launch
                val expenses = withContext(Dispatchers.IO) {
                    database.expenseDao().getExpenses(userId)
                }
                expenses.forEach { expense ->
                    try {
                        val calendar = Calendar.getInstance()
                        calendar.time = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(expense.date)
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        val newTimestamp = calendar.timeInMillis
                        if (expense.timestamp != newTimestamp) {
                            val updatedExpense = expense.copy(timestamp = newTimestamp)
                            withContext(Dispatchers.IO) {
                                database.expenseDao().insert(updatedExpense)
                            }
                            Log.d("MainActivity", "Updated timestamp for expense ${expense.id}: ${expense.timestamp} to $newTimestamp")
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error updating timestamp for expense ${expense.id}: $e")
                    }
                }
                loadExpenses()
                Log.d("MainActivity", "Expenses loaded after database initialization")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing database: $e")
                Toast.makeText(this@MainActivity, "Error accessing database", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup chip filters
        binding.chipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (group.findViewById<Chip>(checkedId)?.id) {
                R.id.chipAll -> {
                    currentFilter = "all"
                    startDate = null
                    endDate = null
                    binding.chipDay.text = "Pick Date"
                    Log.d("MainActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
                R.id.chipExpenses -> {
                    currentFilter = "expense"
                    startDate = null
                    endDate = null
                    binding.chipDay.text = "Pick Date"
                    Log.d("MainActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
                R.id.chipIncome -> {
                    currentFilter = "income"
                    startDate = null
                    endDate = null
                    binding.chipDay.text = "Pick Date"
                    Log.d("MainActivity", "Filter changed to: $currentFilter")
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
                    Log.d("MainActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
            }
        }

        // Setup logout button
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            Log.d("MainActivity", "Logout clicked")
        }

        // Setup add expense button
        binding.btnAddExpense.setOnClickListener {
            Log.d("MainActivity", "Navigating to AddExpenseActivity via btnAddExpense")
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        // Setup bottom navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNavClickTime < navDebounceDelay) {
                Log.d("MainActivity", "Navigation click debounced: ${item.itemId}")
                return@setOnItemSelectedListener false
            }
            lastNavClickTime = currentTime
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("MainActivity", "Home tab selected")
                    true
                }
                R.id.nav_categories -> {
                    Log.d("MainActivity", "Navigating to CategoriesActivity")
                    startActivity(Intent(this, CategoriesActivity::class.java))
                    true
                }
                R.id.nav_goals -> {
                    Log.d("MainActivity", "Navigating to GoalsActivity")
                    startActivity(Intent(this, GoalsActivity::class.java))
                    true
                }
                else -> {
                    Log.d("MainActivity", "Unknown navigation item: ${item.itemId}")
                    false
                }
            }
        }
        binding.bottomNav.menu.findItem(R.id.nav_home)?.isChecked = true
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
            Log.d("MainActivity", "Date range selected: $startDateStr to $endDateStr")
            loadExpenses()
        }
        datePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun loadExpenses() {
        if (!isDatabaseInitialized) {
            Log.w("MainActivity", "Database not initialized, skipping expense load")
            Toast.makeText(this, "Loading, please wait", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val expenses = withContext(Dispatchers.IO) {
                    when (currentFilter) {
                        "expense" -> database.expenseDao().getExpensesByType(userId, "expense")
                        "income" -> database.expenseDao().getExpensesByType(userId, "income")
                        "day" -> {
                            if (startDate != null && endDate != null) {
                                database.expenseDao().getExpensesByDateRange(userId, startDate!!, endDate!!)
                            } else {
                                emptyList()
                            }
                        }
                        else -> database.expenseDao().getExpenses(userId)
                    }
                }
                expenseAdapter.updateExpenses(expenses)
                Log.d("MainActivity", "Fetched expenses: ${expenses.map { it.id }}")

                // Calculate balance
                val balance = expenses.sumOf { expense ->
                    if (expense.type == "income") expense.amount else -expense.amount
                }
                binding.tvBalance.text = "Balance: Rp ${DecimalFormat("0.00").format(balance)}"
                Log.d("MainActivity", "Balance updated: $balance")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading expenses: $e")
                Toast.makeText(this@MainActivity, "Error loading expenses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")
        loadExpenses()
    }
}