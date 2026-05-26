package com.example.prog7313_poe_part2

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
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
    private lateinit var progressGoal: ProgressBar
    private lateinit var textViewGoalStatus: TextView

    data class Goal(
        val month: String = "",
        val minGoal: Double = 0.0,
        val maxGoal: Double = 0.0
    )

    data class Expense(
        val id: String = "",
        val category: String = "",
        val amount: Double = 0.0,
        val date: String = "",
        val description: String = "",
        val receiptUri: String = "",
        val receiptType: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_goals)

        minimum = findViewById(R.id.minimum)
        maximum = findViewById(R.id.maximum)
        save = findViewById(R.id.save)
        progressGoal = findViewById(R.id.progressGoal)
        textViewGoalStatus = findViewById(R.id.textViewGoalStatus)

        loadCurrentGoal()
        loadGoalProgress()

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
                loadCurrentGoal()
                loadGoalProgress()
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

    private fun loadGoalProgress() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val userRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)

        userRef.child("goals")
            .child(currentMonth)
            .get()
            .addOnSuccessListener { goalSnapshot ->
                val goal = goalSnapshot.getValue(Goal::class.java)

                if (goal == null) {
                    progressGoal.progress = 0
                    textViewGoalStatus.text = "No goal set for this month"
                    return@addOnSuccessListener
                }

                userRef.child("expenses")
                    .get()
                    .addOnSuccessListener { expenseSnapshot ->
                        var totalSpent = 0.0

                        for (item in expenseSnapshot.children) {
                            val expense = item.getValue(Expense::class.java)

                            if (expense != null && expense.date.startsWith(currentMonth)) {
                                totalSpent += expense.amount
                            }
                        }

                        val progress = if (goal.maxGoal > 0) {
                            ((totalSpent / goal.maxGoal) * 100).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }

                        progressGoal.progress = progress

                        val status = when {
                            totalSpent < goal.minGoal -> "Below budget range"
                            totalSpent <= goal.maxGoal -> "In budget"
                            else -> "Over budget"
                        }

                        textViewGoalStatus.text =
                                    "Minimum: R%.2f\n".format(goal.minGoal) +
                                    "Maximum: R%.2f\n".format(goal.maxGoal) +
                                    "Spent this month: R%.2f\n\n".format(totalSpent) +
                                    "Status: $status"
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(this, "Failed to load expenses: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Failed to load goal progress: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }
}