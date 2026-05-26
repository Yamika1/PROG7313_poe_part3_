package com.example.prog7313_poe_part2

import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ExpenseSearch : AppCompatActivity() {

    private lateinit var editTextStartDate: EditText
    private lateinit var editTextEndDate: EditText
    private lateinit var buttonSearch: Button
    private lateinit var results: LinearLayout

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
        setContentView(R.layout.activity_expense_search)

        editTextStartDate = findViewById(R.id.editTextStartDate)
        editTextEndDate = findViewById(R.id.editTextEndDate)
        buttonSearch = findViewById(R.id.buttonSearch)
        results = findViewById(R.id.results)

        editTextStartDate.setOnClickListener {
            showDatePicker(isStartDate = true)
        }

        editTextEndDate.setOnClickListener {
            showDatePicker(isStartDate = false)
        }

        buttonSearch.setOnClickListener {
            searchExpenses()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedMonth = String.format("%02d", selectedMonth + 1)
                val formattedDay = String.format("%02d", selectedDay)
                val selectedDate = "$selectedYear-$formattedMonth-$formattedDay"

                if (isStartDate) {
                    editTextStartDate.setText(selectedDate)
                } else {
                    editTextEndDate.setText(selectedDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun searchExpenses() {
        val startDate = editTextStartDate.text.toString().trim()
        val endDate = editTextEndDate.text.toString().trim()

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please enter both start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

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
                results.removeAllViews()

                val expenseResults = mutableListOf<Expense>()

                for (expenseSnapshot in snapshot.children) {
                    val expense = expenseSnapshot.getValue(Expense::class.java)

                    if (expense != null && expense.date >= startDate && expense.date <= endDate) {
                        expenseResults.add(expense)
                    }
                }

                if (expenseResults.isEmpty()) {
                    val noResultsText = TextView(this).apply {
                        text = "No expenses found for the selected period"
                        textSize = 16f
                    }
                    results.addView(noResultsText)
                    return@addOnSuccessListener
                }

                for (expense in expenseResults) {
                    val expenseText = TextView(this).apply {
                        text = "Category: ${expense.category}\n" +
                                "Amount: R${expense.amount}\n" +
                                "Date: ${expense.date}\n" +
                                "Description: ${expense.description}"
                        textSize = 16f
                        setPadding(0, 16, 0, 8)
                    }

                    results.addView(expenseText)

                    if (expense.photoUri.isNotEmpty()) {
                        val imageView = ImageView(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            adjustViewBounds = true
                            scaleType = ImageView.ScaleType.FIT_CENTER
                        }

                        try {
                            imageView.setImageURI(Uri.parse(expense.photoUri))
                            results.addView(imageView)
                        } catch (e: Exception) {
                            val imageErrorText = TextView(this).apply {
                                text = "Image could not be loaded"
                                textSize = 14f
                            }
                            results.addView(imageErrorText)
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Failed to search expenses: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}