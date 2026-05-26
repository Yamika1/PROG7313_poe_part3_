package com.example.prog7313_poe_part2

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
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
    private lateinit var achievementsContainer: LinearLayout

    private val xpPerLevel = 500

    data class Expense(
        val id: String = "",
        val category: String = "",
        val amount: Double = 0.0,
        val date: String = "",
        val description: String = "",
        val receiptUri: String = "",
        val receiptType: String = "",
        val photoUri: String = ""
    )

    data class Goal(
        val month: String = "",
        val minGoal: Double = 0.0,
        val maxGoal: Double = 0.0
    )

    data class Achievement(
        val id: String,
        val title: String,
        val description: String,
        val xp: Int,
        val unlocked: Boolean
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
        achievementsContainer = findViewById(R.id.achievementsContainer)

        loadRewards()

        buttonClaimReward.setOnClickListener {
            Toast.makeText(this, "Achievements refreshed", Toast.LENGTH_SHORT).show()
            loadRewards()
        }
    }

    private fun loadRewards() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val userRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)

        userRef.child("expenses")
            .get()
            .addOnSuccessListener { expenseSnapshot ->

                val allExpenses = mutableListOf<Expense>()

                for (item in expenseSnapshot.children) {
                    val expense = item.getValue(Expense::class.java)
                    if (expense != null) {
                        allExpenses.add(expense)
                    }
                }

                val monthExpenses = allExpenses.filter { it.date.startsWith(currentMonth) }
                val totalSpentThisMonth = monthExpenses.sumOf { it.amount }
                val totalExpenseCount = allExpenses.size
                val monthExpenseCount = monthExpenses.size

                val receiptCount = allExpenses.count {
                    it.receiptUri.isNotEmpty() || it.photoUri.isNotEmpty()
                }

                userRef.child("goals")
                    .child(currentMonth)
                    .get()
                    .addOnSuccessListener { goalSnapshot ->

                        val goal = goalSnapshot.getValue(Goal::class.java)

                        val hasGoal = goal != null

                        val isInBudget = goal != null &&
                                totalSpentThisMonth >= goal.minGoal &&
                                totalSpentThisMonth <= goal.maxGoal

                        val isBelowMaxGoal = goal != null &&
                                totalSpentThisMonth <= goal.maxGoal

                        val achievements = buildAchievements(
                            totalExpenseCount = totalExpenseCount,
                            monthExpenseCount = monthExpenseCount,
                            receiptCount = receiptCount,
                            isInBudget = isInBudget,
                            hasGoal = hasGoal,
                            isBelowMaxGoal = isBelowMaxGoal
                        )

                        val unlockedAchievements = achievements.filter { it.unlocked }
                        val xp = unlockedAchievements.sumOf { it.xp }

                        val level = (xp / xpPerLevel) + 1
                        val progress = ((xp % xpPerLevel) * 100) / xpPerLevel
                        val creditScore = xp + monthExpenseCount + receiptCount

                        progressXp.progress = progress
                        textViewLevel.text = "Level $level"
                        textViewXpPercent.text = "$progress% TOWARDS LEVEL ${level + 1}"
                        textViewXpCount.text = "$xp / $xpPerLevel XP"
                        textViewCreditScore.text = "%,d".format(creditScore)

                        displayAchievements(achievements)
                        saveReward(uid, xp, level, progress, creditScore, achievements)
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(this, "Failed to load goals: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Failed to load rewards: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buildAchievements(
        totalExpenseCount: Int,
        monthExpenseCount: Int,
        receiptCount: Int,
        isInBudget: Boolean,
        hasGoal: Boolean,
        isBelowMaxGoal: Boolean
    ): List<Achievement> {
        return listOf(
            Achievement(
                id = "first_step",
                title = "First Step",
                description = "Add your first expense entry.",
                xp = 50,
                unlocked = totalExpenseCount >= 1
            ),
            Achievement(
                id = "tracker",
                title = "Tracker",
                description = "Add at least 5 expenses this month.",
                xp = 100,
                unlocked = monthExpenseCount >= 5
            ),
            Achievement(
                id = "consistent_bee",
                title = "Consistent Bee",
                description = "Add at least 10 expenses this month.",
                xp = 150,
                unlocked = monthExpenseCount >= 10
            ),
            Achievement(
                id = "budget_keeper",
                title = "Budget Keeper",
                description = "Stay within your minimum and maximum goal range.",
                xp = 200,
                unlocked = isInBudget
            ),
            Achievement(
                id = "receipt_saver",
                title = "Receipt Saver",
                description = "Upload at least one receipt, screenshot, or PDF.",
                xp = 75,
                unlocked = receiptCount >= 1
            ),
            Achievement(
                id = "goal_setter",
                title = "Goal Setter",
                description = "Set a minimum and maximum goal for this month.",
                xp = 50,
                unlocked = hasGoal
            ),
            Achievement(
                id = "big_saver",
                title = "Big Saver",
                description = "Stay below your maximum spending goal.",
                xp = 100,
                unlocked = isBelowMaxGoal
            )
        )
    }

    private fun displayAchievements(achievements: List<Achievement>) {
        achievementsContainer.removeAllViews()

        for (achievement in achievements) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 18, 0, 18)
            }

            val icon = TextView(this).apply {
                text = if (achievement.unlocked) "✓" else "○"
                textSize = 24f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(42, 42)
            }

            val textBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setPadding(14, 0, 14, 0)
            }

            val title = TextView(this).apply {
                text = achievement.title
                textSize = 15f
                setTextColor(Color.BLACK)
                setTypeface(null, Typeface.BOLD)
            }

            val description = TextView(this).apply {
                text = achievement.description
                textSize = 13f
                setTextColor(Color.GRAY)
            }

            val status = TextView(this).apply {
                text = if (achievement.unlocked) "Unlocked" else "Locked"
                textSize = 11f
                setTextColor(Color.DKGRAY)
            }

            textBox.addView(title)
            textBox.addView(description)
            textBox.addView(status)

            val xpText = TextView(this).apply {
                text = "+${achievement.xp} XP"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setPadding(10, 8, 10, 8)
                setTextColor(Color.BLACK)
            }

            row.addView(icon)
            row.addView(textBox)
            row.addView(xpText)

            achievementsContainer.addView(row)

            val divider = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                setBackgroundColor(Color.parseColor("#EEEEEE"))
            }

            achievementsContainer.addView(divider)
        }
    }

    private fun saveReward(
        uid: String,
        xp: Int,
        level: Int,
        progress: Int,
        creditScore: Int,
        achievements: List<Achievement>
    ) {
        val achievementMap = achievements.associate { achievement ->
            achievement.id to mapOf(
                "title" to achievement.title,
                "description" to achievement.description,
                "xp" to achievement.xp,
                "unlocked" to achievement.unlocked
            )
        }

        val reward = mapOf(
            "xp" to xp,
            "level" to level,
            "progress" to progress,
            "creditScore" to creditScore,
            "achievements" to achievementMap
        )

        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("rewards")
            .child("current")
            .setValue(reward)
    }
}