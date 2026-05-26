package com.example.prog7313_poe_part2

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Rewards : AppCompatActivity() {

    private lateinit var progressXp: ProgressBar
    private lateinit var textViewLevel: TextView
    private lateinit var textViewXpPercent: TextView
    private lateinit var textViewXpCount: TextView
    private lateinit var textViewCreditScore: TextView
    private lateinit var buttonClaimReward: Button

    private val maxXp = 2000

    data class Expense(
        val id: String = "",
        val category: String = "",
        val amount: Double = 0.0,
        val date: String = "",
        val description: String = "",
        val photoUri: String = ""
    )

    data class Goal(
        val month: String = "",
        val minGoal: Double = 0.0,
        val maxGoal: Double = 0.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rewards)

        progressXp = findViewById(R.id.progressXp)
        textViewLevel = findViewById(R.id.textViewLevel)
        textViewXpPercent = findViewById(R.id.textViewXpPercent)
        textViewXpCount = findViewById(R.id.textViewXpCount)
        textViewCreditScore = findViewById(R.id.textViewCreditScore)
        buttonClaimReward = findViewById(R.id.buttonClaimReward)

        loadRewards()

        buttonClaimReward.setOnClickListener {
            claimReward()
        }
    }

    private fun loadRewards() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val database = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)

        database.child("expenses")
            .get()
            .addOnSuccessListener { expenseSnapshot ->

                val expenses = mutableListOf<Expense>()

                for (item in expenseSnapshot.children) {
                    val expense = item.getValue(Expense::class.java)

                    if (expense != null) {
                        expenses.add(expense)
                    }
                }

                val monthExpenses = expenses.filter { it.date.startsWith(currentMonth) }
                val totalSpent = monthExpenses.sumOf { it.amount }
                val expenseCount = monthExpenses.size

                database.child("goals")
                    .child(currentMonth)
                    .get()
                    .addOnSuccessListener { goalSnapshot ->

                        val goal = goalSnapshot.getValue(Goal::class.java)

                        var xp = expenseCount * 10

                        if (goal != null) {
                            if (totalSpent >= goal.minGoal && totalSpent <= goal.maxGoal) {
                                xp += 50
                            }
                        }

                        val level = (xp / maxXp) + 1
                        val percent = ((xp % maxXp) * 100) / maxXp
                        val creditScore = xp + totalSpent.toInt()

                        progressXp.progress = percent
                        textViewLevel.text = "Level $level"
                        textViewXpPercent.text = "$percent% TOWARDS LEVEL ${level + 1}"
                        textViewXpCount.text = "$xp / $maxXp XP"
                        textViewCreditScore.text = "%,d".format(creditScore)

                        saveReward(uid, xp, level, percent, creditScore)
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(
                            this,
                            "Failed to load goals: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Failed to load rewards: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveReward(
        uid: String,
        xp: Int,
        level: Int,
        percent: Int,
        creditScore: Int
    ) {
        val reward = mapOf(
            "xp" to xp,
            "level" to level,
            "progress" to percent,
            "creditScore" to creditScore
        )

        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("rewards")
            .child("current")
            .setValue(reward)
    }

    private fun claimReward() {
        Toast.makeText(this, "Reward checked successfully", Toast.LENGTH_SHORT).show()
        loadRewards()
    }
}