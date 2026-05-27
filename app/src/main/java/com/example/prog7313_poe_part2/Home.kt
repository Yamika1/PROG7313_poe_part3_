package com.example.prog7313_poe_part2

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
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
    private lateinit var imageViewProfile: ImageView

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
        imageViewProfile = findViewById(R.id.imageViewProfile)

        buttonDownloadStatement = findViewById(R.id.buttonDownloadStatement)
        buttonCategoryAmount = findViewById(R.id.buttonCategoryAmount)
        buttonRewards = findViewById(R.id.buttonRewards)
        currentBalance = findViewById(R.id.currentBalance)
        buttonExpenseEntry = findViewById(R.id.buttonExpenseEntry)
        expenseSearch = findViewById(R.id.expenseSearch)
        buttonUserGoals = findViewById(R.id.buttonUserGoals)

        getLoggedInUser()
        getCurrentBalance()
        loadProfilePicture()

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

                    val amount = expenseSnapshot
                        .child("amount")
                        .getValue(Double::class.java) ?: 0.0

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

    private fun loadProfilePicture() {

        FirebaseDatabase.getInstance()
            .getReference("profile_images")
            .orderByChild("uploadedAt")
            .limitToLast(1)
            .get()

            .addOnSuccessListener { snapshot ->

                if (snapshot.exists()) {

                    for (child in snapshot.children) {

                        val base64Image =
                            child.child("imageBase64")
                                .getValue(String::class.java)

                        if (base64Image != null) {

                            try {

                                val imageBytes =
                                    Base64.decode(base64Image, Base64.DEFAULT)

                                val bitmap = BitmapFactory.decodeByteArray(
                                    imageBytes,
                                    0,
                                    imageBytes.size
                                )

                                imageViewProfile.setImageBitmap(bitmap)

                            } catch (e: Exception) {

                                Log.e("HomeProfile", "Decode error: $e")
                            }
                        }
                    }
                }
            }

            .addOnFailureListener {

                Log.e("HomeProfile", "Failed to load image")
            }
    }

    override fun onResume() {
        super.onResume()

        getCurrentBalance()
        loadProfilePicture()
    }

    private fun openStatementsPage() {
        startActivity(Intent(this, Statements::class.java))
    }

    private fun openRewardsPage() {
        startActivity(Intent(this, Rewards::class.java))
    }

    private fun openCategoryAmountsPage() {
        startActivity(Intent(this, CategoryAmounts::class.java))
    }

    private fun openExpenseEntryPage() {
        startActivity(Intent(this, AddExpense::class.java))
    }

    private fun openExpenseSearchPage() {
        startActivity(Intent(this, ExpenseSearch::class.java))
    }

    private fun openUserGoalsPage() {
        startActivity(Intent(this, UserGoals::class.java))
    }
}