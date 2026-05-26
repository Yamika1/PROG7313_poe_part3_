package com.example.prog7313_poe_part2

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CategoryAmounts : AppCompatActivity() {

    private lateinit var textViewTotalSpent: TextView
    private lateinit var textViewCurrentMonth: TextView
    private lateinit var textViewTopCategory: TextView

    private lateinit var textViewGroceriesAmount: TextView
    private lateinit var textViewGroceriesCount: TextView
    private lateinit var textViewTransportAmount: TextView
    private lateinit var textViewTransportCount: TextView
    private lateinit var textViewEntertainmentAmount: TextView
    private lateinit var textViewEntertainmentCount: TextView
    private lateinit var textViewUtilitiesAmount: TextView
    private lateinit var textViewUtilitiesCount: TextView
    private lateinit var textViewOtherAmount: TextView
    private lateinit var textViewOtherCount: TextView

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
        setContentView(R.layout.activity_category_amounts)

        textViewTotalSpent = findViewById(R.id.textViewTotalSpent)
        textViewCurrentMonth = findViewById(R.id.textViewCurrentMonth)
        textViewTopCategory = findViewById(R.id.textViewTopCategory)

        textViewGroceriesAmount = findViewById(R.id.textViewGroceriesAmount)
        textViewGroceriesCount = findViewById(R.id.textViewGroceriesCount)
        textViewTransportAmount = findViewById(R.id.textViewTransportAmount)
        textViewTransportCount = findViewById(R.id.textViewTransportCount)
        textViewEntertainmentAmount = findViewById(R.id.textViewEntertainmentAmount)
        textViewEntertainmentCount = findViewById(R.id.textViewEntertainmentCount)
        textViewUtilitiesAmount = findViewById(R.id.textViewUtilitiesAmount)
        textViewUtilitiesCount = findViewById(R.id.textViewUtilitiesCount)
        textViewOtherAmount = findViewById(R.id.textViewOtherAmount)
        textViewOtherCount = findViewById(R.id.textViewOtherCount)

        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date()).uppercase()
        textViewCurrentMonth.text = monthName

        loadCategoryData()
    }

    private fun loadCategoryData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .child("expenses")
            .get()
            .addOnSuccessListener { snapshot ->

                val expenses = mutableListOf<Expense>()

                for (expenseSnapshot in snapshot.children) {
                    val expense = expenseSnapshot.getValue(Expense::class.java)

                    if (expense != null) {
                        expenses.add(expense)
                    }
                }

                val groceriesList = expenses.filter { it.category == "Groceries" }
                val transportList = expenses.filter { it.category == "Transport" }
                val entertainmentList = expenses.filter { it.category == "Entertainment" }
                val utilitiesList = expenses.filter { it.category == "Utilities" }
                val otherList = expenses.filter { it.category == "Other" }

                val groceries = groceriesList.sumOf { it.amount }
                val transport = transportList.sumOf { it.amount }
                val entertainment = entertainmentList.sumOf { it.amount }
                val utilities = utilitiesList.sumOf { it.amount }
                val other = otherList.sumOf { it.amount }

                val total = groceries + transport + entertainment + utilities + other

                val topCategory = if (total == 0.0) {
                    "-"
                } else {
                    mapOf(
                        "Groceries" to groceries,
                        "Transport" to transport,
                        "Entertainment" to entertainment,
                        "Utilities" to utilities,
                        "Other" to other
                    ).maxByOrNull { it.value }?.key ?: "-"
                }

                textViewTotalSpent.text = "R%,.2f".format(total)
                textViewTopCategory.text = topCategory

                textViewGroceriesAmount.text = "R%.2f".format(groceries)
                textViewGroceriesCount.text = "${groceriesList.size} Transactions"

                textViewTransportAmount.text = "R%.2f".format(transport)
                textViewTransportCount.text = "${transportList.size} Transactions"

                textViewEntertainmentAmount.text = "R%.2f".format(entertainment)
                textViewEntertainmentCount.text = "${entertainmentList.size} Transactions"

                textViewUtilitiesAmount.text = "R%.2f".format(utilities)
                textViewUtilitiesCount.text = "${utilitiesList.size} Transactions"

                textViewOtherAmount.text = "R%.2f".format(other)
                textViewOtherCount.text = "${otherList.size} Transactions"
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Failed to load category data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}