package com.example.prog7313_poe_part2

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.util.UUID

class add_profile : AppCompatActivity() {

    private lateinit var choose_image: Button
    private lateinit var upload_image: Button
    private lateinit var image_view: ImageView
    private var fileUri: Uri? = null
    private var encodedImage: String? = null

    private lateinit var realtime: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_profile)

        choose_image = findViewById(R.id.choose_image)
        upload_image = findViewById(R.id.upload_image)
        image_view = findViewById(R.id.image_view)
        realtime = FirebaseDatabase.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        choose_image.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Choose an image to upload"), 0
            )
        }

        upload_image.setOnClickListener {
            if (encodedImage != null) {
                uploadImage()
            } else {
                Toast.makeText(applicationContext, "Please select an image to upload", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK && data != null && data.data != null) {
            fileUri = data.data
            try {
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, fileUri)
                image_view.setImageBitmap(bitmap)

                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                encodedImage = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            } catch (e: Exception) {
                Log.e("exception", "error: $e")
            }
        }
    }

    private fun uploadImage() {
        if (encodedImage != null) {
            val imageId = UUID.randomUUID().toString()

            val imageData = mapOf(
                "imageId"    to imageId,
                "imageBase64" to encodedImage,
                "uploadedAt" to System.currentTimeMillis()
            )

            realtime.getReference("profile_images")
                .child(imageId)
                .setValue(imageData)
                .addOnSuccessListener {
                    Toast.makeText(applicationContext, "Image saved successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("RealtimeDB", "Failed to save to database: $e")
                    Toast.makeText(applicationContext, "Failed to save image.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}