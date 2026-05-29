package com.example.prog7313_poe_part2

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class add_profile : AppCompatActivity() {

    private lateinit var profilePreview: ImageView
    private lateinit var image1: ImageView
    private lateinit var image2: ImageView
    private lateinit var image3: ImageView
    private lateinit var image4: ImageView
    private lateinit var selectButton: Button

    private var selectedImageName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_profile)

        profilePreview = findViewById(R.id.profilePreview)
        image1 = findViewById(R.id.image1)
        image2 = findViewById(R.id.image2)
        image3 = findViewById(R.id.image3)
        image4 = findViewById(R.id.image4)
        selectButton = findViewById(R.id.selectButton)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        image1.setOnClickListener { selectProfileImage(R.drawable.bee_avatar_one, "bee_avatar_one") }
        image2.setOnClickListener { selectProfileImage(R.drawable.bee_avatar_two, "bee_avatar_two") }
        image3.setOnClickListener { selectProfileImage(R.drawable.bee_avatar_three, "bee_avatar_three") }
        image4.setOnClickListener { selectProfileImage(R.drawable.bee_avatar_four, "bee_avatar_four") }

        selectButton.setOnClickListener {
            if (selectedImageName != null) {
                saveProfilePictureToFirebase(selectedImageName!!)
            } else {
                Toast.makeText(this, "Please select a profile picture", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectProfileImage(imageRes: Int, imageName: String) {
        selectedImageName = imageName
        profilePreview.setImageResource(imageRes)
    }

    private fun saveProfilePictureToFirebase(imageName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .child("profile")
            .child("avatarName")
            .setValue(imageName)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile picture saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Failed to save: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }
}