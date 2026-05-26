package com.example.prog7313_poe_part2

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserGoals : AppCompatActivity() {

    private lateinit var minimum: EditText
    private lateinit var maximum: EditText
    private lateinit var save: Button

    data class Goal(
        val month: String = "",
        val minGoal: Double = 0.0,
        val maxGoal: Double = 0.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_goals)

        minimum = findViewById(R.id.minimum)
        maximum = findViewById(R.id.maximum)
        save = findViewById(R.id.save)

        loadCurrentGoal()

        save.setOnClickListener {
            saveGoals()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun saveGoals() {
        val minText = minimum.text.toString().trim()
        val maxText = maximum.text.toString().trim()

        if (minText.isEmpty() || maxText.isEmpty()) {
            Toast.makeText(this, "Please enter both minimum and maximum goals", Toast.LENGTH_SHORT).show()
            return
        }

        val minGoal = minText.toDoubleOrNull()
        val maxGoal = maxText.toDoubleOrNull()

        if (minGoal == null || maxGoal == null) {
            Toast.makeText(this, "Please enter valid goal amounts", Toast.LENGTH_SHORT).show()
            return
        }

        if (minGoal > maxGoal) {
            Toast.makeText(this, "Minimum goal cannot be greater than maximum goal", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val goal = Goal(
            month = currentMonth,
            minGoal = minGoal,
            maxGoal = maxGoal
        )

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .child("goals")
            .child(currentMonth)
            .setValue(goal)
            .addOnSuccessListener {
                Toast.makeText(this, "Goals saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Failed to save goals: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCurrentGoal() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .child("goals")
            .child(currentMonth)
            .get()
            .addOnSuccessListener { snapshot ->
                val goal = snapshot.getValue(Goal::class.java)

                if (goal != null) {
                    minimum.setText(goal.minGoal.toString())
                    maximum.setText(goal.maxGoal.toString())
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Failed to load goals: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }
}