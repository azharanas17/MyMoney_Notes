package com.example.mymoney_notes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.example.mymoney_notes.data.AppDatabase
import com.example.mymoney_notes.data.Category
import com.example.mymoney_notes.data.Goal
import com.example.mymoney_notes.databinding.ActivityAddGoalBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddGoalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddGoalBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private var photoUri: Uri? = null
    private val categories = mutableListOf<String>()
    private val defaultCategories = listOf("Food", "Transport", "Entertainment", "Bills", "Other")

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Log.d("AddGoalActivity", "Camera permission granted, launching camera")
            launchCamera()
        } else {
            Log.w("AddGoalActivity", "Camera permission denied")
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("AddGoalActivity", "Camera result: resultCode=${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            binding.ivPhoto.setImageURI(photoUri)
            binding.ivPhoto.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityAddGoalBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("AddGoalActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            Log.e("AddGoalActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Add Goal page", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("AddGoalActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        if (savedInstanceState != null) {
            photoUri = savedInstanceState.getParcelable("photoUri")
            if (photoUri != null) {
                binding.ivPhoto.setImageURI(photoUri)
                binding.ivPhoto.visibility = View.VISIBLE
                Log.d("AddGoalActivity", "Restored photoUri: $photoUri")
            }
        }

        lifecycleScope.launch {
            try {
                database = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@AddGoalActivity)
                }
                loadCategories()
                Log.d("AddGoalActivity", "Database initialized and categories loaded")
            } catch (e: Exception) {
                Log.e("AddGoalActivity", "Error initializing database: $e")
                Toast.makeText(this@AddGoalActivity, "Error accessing database", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Setup category dropdown
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        (binding.categoryInput as MaterialAutoCompleteTextView).setAdapter(categoryAdapter)
        binding.categoryInput.setOnClickListener {
            binding.categoryInput.showDropDown()
        }

        binding.btnCapturePhoto.setOnClickListener {
            Log.d("AddGoalActivity", "Capture Photo clicked")
            checkPermissionsAndCapturePhoto()
        }

        binding.addCategoryText.setOnClickListener {
            Log.d("AddGoalActivity", "Add Category clicked")
            showAddCategoryDialog()
        }

        binding.btnSave.setOnClickListener {
            Log.d("AddGoalActivity", "Save clicked")
            saveGoal()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("AddGoalActivity", "Navigating to MainActivity")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    Log.d("AddGoalActivity", "Navigating to AddExpenseActivity")
                    startActivity(Intent(this, AddExpenseActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_goals -> {
                    Log.d("AddGoalActivity", "Goals tab selected")
                    true
                }
                else -> false
            }
        }
        binding.bottomNav.menu.findItem(R.id.nav_goals)?.isChecked = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("photoUri", photoUri)
        Log.d("AddGoalActivity", "onSaveInstanceState: Saved photoUri")
    }

    private fun checkPermissionsAndCapturePhoto() {
        when {
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("AddGoalActivity", "Camera permission already granted, launching camera")
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Log.d("AddGoalActivity", "Showing permission rationale for camera")
                showPermissionRationale()
            }
            else -> {
                Log.d("AddGoalActivity", "Requesting camera permission")
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("This app needs camera access to take photos for your goals. Please grant the permission.")
            .setPositiveButton("OK") { _, _ ->
                Log.d("AddGoalActivity", "User acknowledged rationale, requesting camera permission")
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Log.w("AddGoalActivity", "User cancelled permission rationale")
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun launchCamera() {
        try {
            val photoFile = createImageFile()
            photoUri = FileProvider.getUriForFile(
                this,
                "com.example.mymoney_notes.fileprovider",
                photoFile
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraLauncher.launch(intent)
            Log.d("AddGoalActivity", "Camera launched with photoUri: $photoUri")
        } catch (e: Exception) {
            Log.e("AddGoalActivity", "Error launching camera: $e")
            Toast.makeText(this, "Error accessing camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val dbCategories = withContext(Dispatchers.IO) {
                    database.categoryDao().getCategories(userId)
                }
                categories.clear()
                categories.addAll(defaultCategories)
                categories.addAll(dbCategories.map { it.name }.filter { !defaultCategories.contains(it) })
                categories.sort()
                val categoryAdapter = ArrayAdapter(this@AddGoalActivity, android.R.layout.simple_dropdown_item_1line, categories)
                (binding.categoryInput as MaterialAutoCompleteTextView).setAdapter(categoryAdapter)
                Log.d("AddGoalActivity", "Categories loaded: $categories")
            } catch (e: Exception) {
                Log.e("AddGoalActivity", "Error loading categories: $e")
                Toast.makeText(this@AddGoalActivity, "Error loading categories", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddCategoryDialog() {
        try {
            val builder = AlertDialog.Builder(this)
            val dialogBinding = layoutInflater.inflate(R.layout.dialog_add_category, null)
            val categoryInput = dialogBinding.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.categoryInput)
            builder.setView(dialogBinding)
                .setTitle("Add Category")
                .setPositiveButton("Add") { _, _ ->
                    val categoryName = categoryInput?.text?.toString()?.trim() ?: ""
                    if (categoryName.isNotEmpty() && !categories.contains(categoryName)) {
                        lifecycleScope.launch {
                            try {
                                val userId = auth.currentUser?.uid ?: return@launch
                                withContext(Dispatchers.IO) {
                                    database.categoryDao().insert(Category(userId = userId, name = categoryName))
                                }
                                loadCategories()
                                Toast.makeText(this@AddGoalActivity, "Category added", Toast.LENGTH_SHORT).show()
                                Log.d("AddGoalActivity", "Category added: $categoryName")
                            } catch (e: Exception) {
                                Log.e("AddGoalActivity", "Error adding category: $e")
                                Toast.makeText(this@AddGoalActivity, "Error adding category", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Invalid or duplicate category", Toast.LENGTH_SHORT).show()
                        Log.w("AddGoalActivity", "Invalid category: $categoryName")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            Log.d("AddGoalActivity", "Add Category dialog shown")
        } catch (e: Exception) {
            Log.e("AddGoalActivity", "Error showing add category dialog: $e")
            Toast.makeText(this, "Error showing category dialog", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveGoal() {
        val category = binding.categoryInput.text.toString().trim()
        val type = if (binding.radioIncome.isChecked) "income" else "expense"
        val description = binding.descriptionInput.text.toString().trim()
        val minGoalStr = binding.minGoalInput.text.toString().trim()
        val maxGoalStr = binding.maxGoalInput.text.toString().trim()
        val month = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())
        val userId = auth.currentUser?.uid ?: return

        if (category.isEmpty() || minGoalStr.isEmpty() || maxGoalStr.isEmpty()) {
            Toast.makeText(this, "Please fill category, min goal, and max goal", Toast.LENGTH_SHORT).show()
            Log.w("AddGoalActivity", "Validation failed: Empty fields")
            return
        }

        val minGoal = minGoalStr.toDoubleOrNull()
        val maxGoal = maxGoalStr.toDoubleOrNull()

        if (minGoal == null || maxGoal == null) {
            Toast.makeText(this, "Invalid min or max goal", Toast.LENGTH_SHORT).show()
            Log.w("AddGoalActivity", "Validation failed: Invalid min/max goal")
            return
        }

        if (minGoal >= maxGoal) {
            Toast.makeText(this, "Minimum goal must be less than maximum goal", Toast.LENGTH_SHORT).show()
            Log.w("AddGoalActivity", "Validation failed: minGoal >= maxGoal")
            return
        }

        lifecycleScope.launch {
            try {
                val categoryId = withContext(Dispatchers.IO) {
                    database.categoryDao().getCategories(userId)
                        .find { it.name == category }?.id?.toString() ?: category
                }

                val goal = Goal(
                    userId = userId,
                    month = month,
                    category = category,
                    categoryId = categoryId,
                    type = type,
                    description = description,
                    photoPath = photoUri?.toString() ?: "",
                    minGoal = minGoal,
                    maxGoal = maxGoal
                )
                withContext(Dispatchers.IO) {
                    database.goalDao().insert(goal)
                }
                Toast.makeText(this@AddGoalActivity, "Goal saved", Toast.LENGTH_SHORT).show()
                Log.d("AddGoalActivity", "Goal saved: $goal")
                startActivity(Intent(this@AddGoalActivity, GoalsActivity::class.java))
                finish()
            } catch (e: Exception) {
                Log.e("AddGoalActivity", "Error saving goal: $e")
                Toast.makeText(this@AddGoalActivity, "Error saving goal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir("photos")
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onResume() {
        super.onResume()
        Log.d("AddGoalActivity", "onResume called")
    }
}