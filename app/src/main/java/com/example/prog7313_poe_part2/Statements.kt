package com.example.prog7313_poe_part2

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Statements : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var btnGenerateStatement: Button
    private lateinit var btnDownloadCsv: Button
    private lateinit var tvStatementSummary: TextView
    private lateinit var statementResults: LinearLayout

    private var latestStatementExpenses: List<Expense> = emptyList()
    private var latestStartDate: String = ""
    private var latestEndDate: String = ""
    private var latestCsvContent: String = ""

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

    private val createCsvFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(latestCsvContent.toByteArray())
                    }

                    Toast.makeText(this, "Statement downloaded successfully", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Download cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.statements)

        btnBack = findViewById(R.id.btnBack)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)
        btnGenerateStatement = findViewById(R.id.btnGenerateStatement)
        btnDownloadCsv = findViewById(R.id.btnDownloadCsv)
        tvStatementSummary = findViewById(R.id.tvStatementSummary)
        statementResults = findViewById(R.id.statementResults)

        btnBack.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
            finish()
        }

        tvStartDate.setOnClickListener {
            showDatePicker(tvStartDate)
        }

        tvEndDate.setOnClickListener {
            showDatePicker(tvEndDate)
        }

        btnGenerateStatement.setOnClickListener {
            generateStatement()
        }

        btnDownloadCsv.setOnClickListener {
            downloadCsvStatement()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollView2)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showDatePicker(target: TextView) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedMonth = String.format("%02d", selectedMonth + 1)
                val formattedDay = String.format("%02d", selectedDay)
                val selectedDate = "$selectedYear-$formattedMonth-$formattedDay"
                target.text = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun generateStatement() {
        val startDate = tvStartDate.text.toString().trim()
        val endDate = tvEndDate.text.toString().trim()

        if (startDate == "Select start date" || endDate == "Select end date") {
            Toast.makeText(this, "Please select a start and end date", Toast.LENGTH_SHORT).show()
            return
        }

        if (startDate > endDate) {
            Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
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
                val expenses = mutableListOf<Expense>()

                for (item in snapshot.children) {
                    val expense = item.getValue(Expense::class.java)

                    if (expense != null && expense.date >= startDate && expense.date <= endDate) {
                        expenses.add(expense)
                    }
                }

                val sortedExpenses = expenses.sortedBy { it.date }
                displayStatement(startDate, endDate, sortedExpenses)
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Failed to load statement: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayStatement(
        startDate: String,
        endDate: String,
        expenses: List<Expense>
    ) {
        latestStatementExpenses = expenses
        latestStartDate = startDate
        latestEndDate = endDate

        statementResults.removeAllViews()

        if (expenses.isEmpty()) {
            tvStatementSummary.text =
                "Period: $startDate to $endDate\n" +
                        "Total Spent: R0.00\n" +
                        "Entries: 0"

            val emptyText = TextView(this).apply {
                text = "No expenses found for the selected period."
                textSize = 15f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER
                setPadding(0, 20, 0, 20)
            }

            statementResults.addView(emptyText)
            return
        }

        val totalSpent = expenses.sumOf { it.amount }
        val receiptCount = expenses.count {
            it.receiptUri.isNotEmpty() || it.photoUri.isNotEmpty()
        }

        val categoryTotals = expenses
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val topCategory = categoryTotals.maxByOrNull { it.value }?.key ?: "-"

        tvStatementSummary.text =
            "Period: $startDate to $endDate\n" +
                    "Total Spent: R%.2f\n".format(totalSpent) +
                    "Entries: ${expenses.size}\n" +
                    "Receipts Attached: $receiptCount\n" +
                    "Top Category: $topCategory"

        for (expense in expenses) {
            addExpenseRow(expense)
        }
    }

    private fun addExpenseRow(expense: Expense) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "${expense.date}  •  ${expense.category}"
            textSize = 15f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }

        val amount = TextView(this).apply {
            text = "Amount: R%.2f".format(expense.amount)
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(0, 6, 0, 0)
        }

        val description = TextView(this).apply {
            text = "Description: ${expense.description}"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 4, 0, 0)
        }

        val receiptUri = when {
            expense.receiptUri.isNotEmpty() -> expense.receiptUri
            expense.photoUri.isNotEmpty() -> expense.photoUri
            else -> ""
        }

        val receiptText = TextView(this).apply {
            text = if (receiptUri.isNotEmpty()) {
                if (expense.receiptType == "pdf") {
                    "Receipt: PDF attached - tap to open"
                } else {
                    "Receipt: Image attached - tap to open"
                }
            } else {
                "Receipt: No receipt attached"
            }

            textSize = 13f
            setTextColor(if (receiptUri.isNotEmpty()) Color.parseColor("#6D5A1E") else Color.GRAY)
            setPadding(0, 6, 0, 0)

            if (receiptUri.isNotEmpty()) {
                setOnClickListener {
                    openReceipt(receiptUri)
                }
            }
        }

        card.addView(title)
        card.addView(amount)
        card.addView(description)
        card.addView(receiptText)

        statementResults.addView(card)

        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            )
            setBackgroundColor(Color.parseColor("#EEEEEE"))
        }

        statementResults.addView(divider)
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

    private fun downloadCsvStatement() {
        if (latestStartDate.isEmpty() || latestEndDate.isEmpty()) {
            Toast.makeText(this, "Generate a statement first", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "statement_${latestStartDate}_to_${latestEndDate}.csv"

        val csvBuilder = StringBuilder()
        csvBuilder.append("Date,Category,Amount,Description,Receipt Type,Receipt Attached\n")

        if (latestStatementExpenses.isEmpty()) {
            csvBuilder.append("No expenses found for selected period,,,,,\n")
        } else {
            for (expense in latestStatementExpenses) {
                val receiptAttached =
                    if (expense.receiptUri.isNotEmpty() || expense.photoUri.isNotEmpty()) {
                        "Yes"
                    } else {
                        "No"
                    }

                val safeDescription = expense.description.replace("\"", "\"\"")

                csvBuilder.append(
                    "${expense.date}," +
                            "${expense.category}," +
                            "${expense.amount}," +
                            "\"$safeDescription\"," +
                            "${expense.receiptType}," +
                            "$receiptAttached\n"
                )
            }
        }

        latestCsvContent = csvBuilder.toString()
        createCsvFileLauncher.launch(fileName)
    }
}