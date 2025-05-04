package com.example.mymoney_notes

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mymoney_notes.databinding.ActivityViewPhotoBinding

class ViewPhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewPhotoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityViewPhotoBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("ViewPhotoActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            Log.e("ViewPhotoActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading photo view", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val photoPath = intent.getStringExtra("photoPath")
        Log.d("ViewPhotoActivity", "Received photoPath: $photoPath")

        if (photoPath.isNullOrEmpty()) {
            Log.w("ViewPhotoActivity", "No photo path provided")
            Toast.makeText(this, "No photo available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            contentResolver.openInputStream(android.net.Uri.parse(photoPath))?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                binding.ivFullPhoto.setImageBitmap(bitmap)
                Log.d("ViewPhotoActivity", "Displayed photo: $photoPath")
            } ?: throw Exception("Failed to open input stream for URI")
        } catch (e: Exception) {
            Log.e("ViewPhotoActivity", "Error loading photo: $e")
            Toast.makeText(this, "Error loading photo", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnBack.setOnClickListener {
            Log.d("ViewPhotoActivity", "Back button clicked")
            finish()
        }
    }
}