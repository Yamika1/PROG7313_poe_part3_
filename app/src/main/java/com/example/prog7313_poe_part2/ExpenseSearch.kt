package com.example.prog7313_poe_part2

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
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
        val receiptUri: String = "",
        val receiptType: String = "",
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
                        gravity = Gravity.CENTER
                        setPadding(0, 20, 0, 20)
                    }
                    results.addView(noResultsText)
                    return@addOnSuccessListener
                }

                for (expense in expenseResults) {
                    addExpenseResultView(expense)
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

    private fun addExpenseResultView(expense: Expense) {
        val expenseText = TextView(this).apply {
            text =
                "Category: ${expense.category}\n" +
                        "Amount: R%.2f\n".format(expense.amount) +
                        "Date: ${expense.date}\n" +
                        "Description: ${expense.description}"
            textSize = 15f
            setTextColor(android.graphics.Color.BLACK)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 16, 0, 8)
        }

        results.addView(expenseText)

        val receiptUri = when {
            expense.receiptUri.isNotEmpty() -> expense.receiptUri
            expense.photoUri.isNotEmpty() -> expense.photoUri
            else -> ""
        }

        if (receiptUri.isNotEmpty()) {
            if (expense.receiptType == "pdf") {
                val pdfText = TextView(this).apply {
                    text = "PDF receipt attached - tap to open"
                    textSize = 14f
                    setTextColor(android.graphics.Color.DKGRAY)
                    setPadding(0, 8, 0, 8)
                    setOnClickListener {
                        openReceipt(receiptUri)
                    }
                }
                results.addView(pdfText)
            } else {
                val imageView = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        350
                    )
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(0, 8, 0, 8)
                    setImageURI(Uri.parse(receiptUri))
                    setOnClickListener {
                        openReceipt(receiptUri)
                    }
                }

                results.addView(imageView)
            }
        } else {
            val noReceiptText = TextView(this).apply {
                text = "No receipt attached"
                textSize = 13f
                setTextColor(android.graphics.Color.GRAY)
                setPadding(0, 4, 0, 8)
            }
            results.addView(noReceiptText)
        }

        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
            setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
        }

        results.addView(divider)
    }

    private fun openReceipt(receiptUri: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(receiptUri), "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open receipt", Toast.LENGTH_SHORT).show()
        }
    }
}