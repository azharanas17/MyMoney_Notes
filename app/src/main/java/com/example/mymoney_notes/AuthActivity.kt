package com.example.mymoney_notes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.example.mymoney_notes.data.AppDatabase
import com.example.mymoney_notes.data.User
import com.example.mymoney_notes.databinding.ActivityAuthBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        lifecycleScope.launch {
            database = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@AuthActivity)
            }
        }

        // Toggle between login and signup
        binding.toggleButton.setOnClickListener {
            if (binding.loginLayout.visibility == android.view.View.VISIBLE) {
                binding.loginLayout.visibility = android.view.View.GONE
                binding.signupLayout.visibility = android.view.View.VISIBLE
                binding.toggleButton.text = "Switch to Login"
            } else {
                binding.loginLayout.visibility = android.view.View.VISIBLE
                binding.signupLayout.visibility = android.view.View.GONE
                binding.toggleButton.text = "Switch to Signup"
            }
        }

        // Login button
        binding.btnLogin.setOnClickListener {
            val username = binding.loginUsername.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loginUser(username, password)
        }

        // Signup button
        binding.btnSignup.setOnClickListener {
            val username = binding.signupUsername.text.toString().trim()
            val email = binding.signupEmail.text.toString().trim()
            val password = binding.signupPassword.text.toString().trim()
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            signupUser(email, password, username)
        }
    }

    private fun loginUser(username: String, password: String) {
        Log.d("AuthActivity", "Attempting login with username: $username")
        lifecycleScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    database.userDao().getUserByUsername(username)
                }
                if (user == null) {
                    Toast.makeText(this@AuthActivity, "Username not found", Toast.LENGTH_SHORT).show()
                    Log.e("AuthActivity", "No user found for username: $username")
                    return@launch
                }
                auth.signInWithEmailAndPassword(user.email, password)
                    .addOnCompleteListener(this@AuthActivity) { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: run {
                                Toast.makeText(this@AuthActivity, "Failed to get user ID", Toast.LENGTH_SHORT).show()
                                Log.e("AuthActivity", "No user ID after login")
                                return@addOnCompleteListener
                            }
                            Log.d("AuthActivity", "Login successful for user: $username, id: $userId")
                            Toast.makeText(this@AuthActivity, "Login successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@AuthActivity, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            Log.e("AuthActivity", "Login failed", task.exception)
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(this@AuthActivity, "Error accessing user data", Toast.LENGTH_SHORT).show()
                Log.e("AuthActivity", "Error querying user by username: $username", e)
            }
        }
    }

    private fun signupUser(email: String, password: String, username: String) {
        Log.d("AuthActivity", "Attempting signup with email: $email, username: $username")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: run {
                        Toast.makeText(this@AuthActivity, "Failed to get user ID", Toast.LENGTH_SHORT).show()
                        Log.e("AuthActivity", "No user ID after signup")
                        return@addOnCompleteListener
                    }
                    lifecycleScope.launch {
                        try {
                            val newUser = User(id = userId, username = username, email = email)
                            withContext(Dispatchers.IO) {
                                database.userDao().insert(newUser)
                            }
                            Log.d("AuthActivity", "User signed up and saved to Room: $username, id: $userId")
                            Toast.makeText(this@AuthActivity, "Signup successful, please login", Toast.LENGTH_SHORT).show()
                            // Switch to login layout
                            binding.loginLayout.visibility = android.view.View.VISIBLE
                            binding.signupLayout.visibility = android.view.View.GONE
                            binding.toggleButton.text = "Switch to Signup"
                            binding.loginUsername.setText(username) // Prefill username
                            binding.loginPassword.text?.clear() // Clear password
                            auth.signOut() // Ensure user is signed out
                        } catch (e: Exception) {
                            Toast.makeText(this@AuthActivity, "Error saving user data", Toast.LENGTH_SHORT).show()
                            Log.e("AuthActivity", "Error saving user to Room: $username", e)
                            auth.signOut()
                        }
                    }
                } else {
                    Toast.makeText(this@AuthActivity, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AuthActivity", "Signup failed", task.exception)
                }
            }
    }
}