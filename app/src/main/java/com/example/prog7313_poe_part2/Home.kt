package com.example.prog7313_poe_part2

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Home : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var textViewUserName: TextView

    private lateinit var buttonDownloadStatement: Button
    private lateinit var buttonCategoryAmount: Button
    private lateinit var buttonRewards: Button
    private lateinit var buttonExpenseEntry: Button
    private lateinit var expenseSearch: Button
    private lateinit var buttonUserGoals: Button

    private lateinit var currentBalance: TextView

    data class Expense(
        val id: String = "",
        val category: String = "",
        val amount: Double = 0.0,
        val date: String = "",
        val description: String = "",
        val photoUri: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        textViewUserName = findViewById(R.id.textViewUserName)
        buttonDownloadStatement = findViewById(R.id.buttonDownloadStatement)
        buttonCategoryAmount = findViewById(R.id.buttonCategoryAmount)
        buttonRewards = findViewById(R.id.buttonRewards)
        currentBalance = findViewById(R.id.currentBalance)
        buttonExpenseEntry = findViewById(R.id.buttonExpenseEntry)
        expenseSearch = findViewById(R.id.expenseSearch)
        buttonUserGoals = findViewById(R.id.buttonUserGoals)

        getLoggedInUser()
        getCurrentBalance()

        buttonDownloadStatement.setOnClickListener {
            openStatementsPage()
        }

        buttonCategoryAmount.setOnClickListener {
            openCategoryAmountsPage()
        }

        buttonExpenseEntry.setOnClickListener {
            openExpenseEntryPage()
        }

        buttonRewards.setOnClickListener {
            openRewardsPage()
        }

        expenseSearch.setOnClickListener {
            openExpenseSearchPage()
        }

        buttonUserGoals.setOnClickListener {
            openUserGoalsPage()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getCurrentBalance() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            currentBalance.text = "CURRENT BALANCE: R0.00"
            return
        }

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .child("expenses")
            .get()
            .addOnSuccessListener { snapshot ->
                var total = 0.0

                for (expenseSnapshot in snapshot.children) {
                    val amount = expenseSnapshot.child("amount").getValue(Double::class.java) ?: 0.0
                    total += amount
                }

                currentBalance.text = "CURRENT BALANCE: R%.2f".format(total)
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Failed to load balance: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                currentBalance.text = "CURRENT BALANCE: R0.00"
            }
    }

    @SuppressLint("SetTextI18n")
    private fun getLoggedInUser() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            textViewUserName.text = "LOGGED IN AS: Unknown"
            return
        }

        val email = user.email ?: "Unknown user"
        textViewUserName.text = "LOGGED IN AS: $email"
    }

    override fun onResume() {
        super.onResume()
        getCurrentBalance()
    }

    private fun openStatementsPage() {
        val intent = Intent(this, Statements::class.java)
        startActivity(intent)
    }

    private fun openRewardsPage() {
        val intent = Intent(this, Rewards::class.java)
        startActivity(intent)
    }

    private fun openCategoryAmountsPage() {
        val intent = Intent(this, CategoryAmounts::class.java)
        startActivity(intent)
    }

    private fun openExpenseEntryPage() {
        val intent = Intent(this, AddExpense::class.java)
        startActivity(intent)
    }

    private fun openExpenseSearchPage() {
        val intent = Intent(this, ExpenseSearch::class.java)
        startActivity(intent)
    }

    private fun openUserGoalsPage() {
        val intent = Intent(this, UserGoals::class.java)
        startActivity(intent)
    }
}