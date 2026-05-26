package com.example.prog7313_poe_part2

import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddExpense : AppCompatActivity() {

    private lateinit var editTextAmount: EditText
    private lateinit var editTextDate: EditText
    private lateinit var editTextDescription: EditText

    private lateinit var rowGroceries: LinearLayout
    private lateinit var rowTransport: LinearLayout
    private lateinit var rowEntertainment: LinearLayout
    private lateinit var rowUtilities: LinearLayout
    private lateinit var rowOther: LinearLayout

    private lateinit var buttonSaveExpense: Button
    private lateinit var buttonCancel: TextView

    private var selectedCategory = "Groceries"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        editTextAmount = findViewById(R.id.editTextAmount)
        editTextDate = findViewById(R.id.editTextDate)
        editTextDescription = findViewById(R.id.editTextDescription)

        rowGroceries = findViewById(R.id.rowGroceries)
        rowTransport = findViewById(R.id.rowTransport)
        rowEntertainment = findViewById(R.id.rowEntertainment)
        rowUtilities = findViewById(R.id.rowUtilities)
        rowOther = findViewById(R.id.rowOther)

        buttonSaveExpense = findViewById(R.id.buttonSaveExpense)
        buttonCancel = findViewById(R.id.buttonCancel)

        editTextDate.setOnClickListener {
            showDatePicker()
        }

        rowGroceries.setOnClickListener {
            selectCategory(rowGroceries, "Groceries")
        }

        rowTransport.setOnClickListener {
            selectCategory(rowTransport, "Transport")
        }

        rowEntertainment.setOnClickListener {
            selectCategory(rowEntertainment, "Entertainment")
        }

        rowUtilities.setOnClickListener {
            selectCategory(rowUtilities, "Utilities")
        }

        rowOther.setOnClickListener {
            selectCategory(rowOther, "Other")
        }

        buttonSaveExpense.setOnClickListener {
            saveExpense()
        }

        buttonCancel.setOnClickListener {
            finish()
        }

        selectCategory(rowGroceries, "Groceries")
    }

    private fun selectCategory(selectedRow: LinearLayout, category: String) {
        rowGroceries.setBackgroundResource(R.drawable.bg_white_card)
        rowTransport.setBackgroundResource(R.drawable.bg_white_card)
        rowEntertainment.setBackgroundResource(R.drawable.bg_white_card)
        rowUtilities.setBackgroundResource(R.drawable.bg_white_card)
        rowOther.setBackgroundResource(R.drawable.bg_white_card)

        selectedRow.setBackgroundResource(R.drawable.bg_soft_yellow_card)
        selectedCategory = category
    }

    private fun saveExpense() {
        val amountText = editTextAmount.text.toString().trim()
        val date = editTextDate.text.toString().trim()
        val description = editTextDescription.text.toString().trim()

        if (amountText.isEmpty() || date.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill in all the required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val databaseRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .child("expenses")

        val expenseId = databaseRef.push().key

        if (expenseId == null) {
            Toast.makeText(this, "Could not create expense ID", Toast.LENGTH_SHORT).show()
            return
        }

        val expense = mapOf(
            "id" to expenseId,
            "category" to selectedCategory,
            "amount" to amount,
            "date" to date,
            "description" to description,
            "photoUri" to ""
        )

        databaseRef.child(expenseId)
            .setValue(expense)
            .addOnSuccessListener {
                Toast.makeText(this, "Expense saved successfully", Toast.LENGTH_SHORT).show()
                clearFields()
                finish()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Failed to save expense: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearFields() {
        editTextAmount.text.clear()
        editTextDate.text.clear()
        editTextDescription.text.clear()
        selectCategory(rowGroceries, "Groceries")
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedMonth = String.format("%02d", selectedMonth + 1)
                val formattedDay = String.format("%02d", selectedDay)
                val selectedDate = "$selectedYear-$formattedMonth-$formattedDay"
                editTextDate.setText(selectedDate)
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }
}