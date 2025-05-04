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
import com.example.mymoney_notes.data.CategoryTotal
import com.example.mymoney_notes.databinding.ActivityCategoriesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var auth: FirebaseAuth
    private var database: AppDatabase? = null
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityCategoriesBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("CategoriesActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            Log.e("CategoriesActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Categories", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("CategoriesActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setupRecyclerView()
        setupBottomNavigation()

        lifecycleScope.launch {
            try {
                database = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@CategoriesActivity)
                }
                Log.d("CategoriesActivity", "Database initialized")
                loadCategories()
            } catch (e: Exception) {
                Log.e("CategoriesActivity", "Error initializing database: $e")
                Toast.makeText(this@CategoriesActivity, "Error accessing database", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { categoryTotal ->
            try {
                val intent = Intent(this@CategoriesActivity, CategoryExpensesActivity::class.java)
                intent.putExtra("category", categoryTotal.category)
                startActivity(intent)
                Log.d("CategoriesActivity", "Navigating to CategoryExpensesActivity for ${categoryTotal.category}")
            } catch (e: Exception) {
                Log.e("CategoriesActivity", "Error navigating to CategoryExpensesActivity: $e")
                Toast.makeText(this@CategoriesActivity, "Error opening category", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@CategoriesActivity)
            adapter = categoryAdapter
            setHasFixedSize(true)
        }
        Log.d("CategoriesActivity", "RecyclerView set up")
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_categories
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    try {
                        startActivity(Intent(this, MainActivity::class.java))
                        Log.d("CategoriesActivity", "Navigating to MainActivity")
                        true
                    } catch (e: Exception) {
                        Log.e("CategoriesActivity", "Error navigating to MainActivity: $e")
                        Toast.makeText(this, "Error opening Home", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.nav_categories -> true
                R.id.nav_goals -> {
                    try {
                        startActivity(Intent(this, GoalsActivity::class.java))
                        Log.d("CategoriesActivity", "Navigating to GoalsActivity")
                        true
                    } catch (e: Exception) {
                        Log.e("CategoriesActivity", "Error navigating to GoalsActivity: $e")
                        Toast.makeText(this, "Error opening Goals", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val db = database
            if (db == null) {
                Log.e("CategoriesActivity", "Database not initialized in loadCategories")
                Toast.makeText(this@CategoriesActivity, "Database error", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                val categoryTotals = withContext(Dispatchers.IO) {
                    db.expenseDao().getCategoryTotals(userId)
                }
                categoryAdapter.submitList(categoryTotals)
                binding.tvTitle.text = "Categories"
                Log.d("CategoriesActivity", "Categories loaded: ${categoryTotals.map { it.category to it.total }}")
                if (categoryTotals.isEmpty()) {
                    Toast.makeText(this@CategoriesActivity, "No categories found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CategoriesActivity", "Error loading categories: $e")
                Toast.makeText(this@CategoriesActivity, "Error loading categories", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("CategoriesActivity", "onResume called")
        database?.let {
            loadCategories()
        } ?: Log.w("CategoriesActivity", "Database not initialized in onResume")
    }
}