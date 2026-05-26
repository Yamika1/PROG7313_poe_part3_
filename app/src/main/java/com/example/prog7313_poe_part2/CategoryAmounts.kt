package com.example.prog7313_poe_part2

import android.app.DatePickerDialog
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CategoryAmounts : AppCompatActivity() {

    private lateinit var textViewTotalSpent: TextView
    private lateinit var textViewCurrentMonth: TextView
    private lateinit var textViewTopCategory: TextView
    private lateinit var textViewGoalComparison: TextView

    private lateinit var editTextStartDate: EditText
    private lateinit var editTextEndDate: EditText
    private lateinit var buttonLoadChart: Button
    private lateinit var pieChartCategories: PieChart

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
        val receiptUri: String = "",
        val receiptType: String = ""
    )

    data class Goal(
        val month: String = "",
        val minGoal: Double = 0.0,
        val maxGoal: Double = 0.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category_amounts)

        textViewTotalSpent = findViewById(R.id.textViewTotalSpent)
        textViewCurrentMonth = findViewById(R.id.textViewCurrentMonth)
        textViewTopCategory = findViewById(R.id.textViewTopCategory)
        textViewGoalComparison = findViewById(R.id.textViewGoalComparison)

        editTextStartDate = findViewById(R.id.editTextStartDate)
        editTextEndDate = findViewById(R.id.editTextEndDate)
        buttonLoadChart = findViewById(R.id.buttonLoadChart)
        pieChartCategories = findViewById(R.id.pieChartCategories)

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

        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        editTextStartDate.setText("$currentMonth-01")
        editTextEndDate.setText(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))

        editTextStartDate.setOnClickListener {
            showDatePicker(editTextStartDate)
        }

        editTextEndDate.setOnClickListener {
            showDatePicker(editTextEndDate)
        }

        buttonLoadChart.setOnClickListener {
            loadCategoryData()
        }

        loadCategoryData()
    }

    private fun showDatePicker(target: EditText) {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedMonth = String.format("%02d", selectedMonth + 1)
                val formattedDay = String.format("%02d", selectedDay)
                val selectedDate = "$selectedYear-$formattedMonth-$formattedDay"
                target.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun loadCategoryData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val startDate = editTextStartDate.text.toString().trim()
        val endDate = editTextEndDate.text.toString().trim()

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please select start and end dates", Toast.LENGTH_SHORT).show()
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

                    if (expense != null && expense.date >= startDate && expense.date <= endDate) {
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

                updatePieChart(groceries, transport, entertainment, utilities, other)
                loadGoalComparison(uid, total)
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Failed to load category data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updatePieChart(
        groceries: Double,
        transport: Double,
        entertainment: Double,
        utilities: Double,
        other: Double
    ) {
        val entries = mutableListOf<PieEntry>()

        if (groceries > 0) entries.add(PieEntry(groceries.toFloat(), "Groceries"))
        if (transport > 0) entries.add(PieEntry(transport.toFloat(), "Transport"))
        if (entertainment > 0) entries.add(PieEntry(entertainment.toFloat(), "Entertainment"))
        if (utilities > 0) entries.add(PieEntry(utilities.toFloat(), "Utilities"))
        if (other > 0) entries.add(PieEntry(other.toFloat(), "Other"))

        if (entries.isEmpty()) {
            pieChartCategories.clear()
            pieChartCategories.centerText = "No expenses found"
            pieChartCategories.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "Spending by Category")
        dataSet.colors = listOf(
            Color.rgb(255, 193, 7),
            Color.rgb(33, 150, 243),
            Color.rgb(156, 39, 176),
            Color.rgb(76, 175, 80),
            Color.rgb(255, 87, 34)
        )
        dataSet.valueTextSize = 13f
        dataSet.valueTextColor = Color.BLACK
        dataSet.sliceSpace = 3f

        val pieData = PieData(dataSet)

        pieChartCategories.data = pieData
        pieChartCategories.description.isEnabled = false
        pieChartCategories.isDrawHoleEnabled = true
        pieChartCategories.holeRadius = 45f
        pieChartCategories.setTransparentCircleAlpha(0)
        pieChartCategories.centerText = "Spending"
        pieChartCategories.setCenterTextSize(15f)
        pieChartCategories.legend.isEnabled = true
        pieChartCategories.animateY(900)
        pieChartCategories.invalidate()
    }

    private fun loadGoalComparison(uid: String, totalSpent: Double) {
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .child("goals")
            .child(currentMonth)
            .get()
            .addOnSuccessListener { snapshot ->
                val goal = snapshot.getValue(Goal::class.java)

                if (goal == null) {
                    textViewGoalComparison.text = "No goals set for this month"
                    return@addOnSuccessListener
                }

                textViewGoalComparison.text = when {
                    totalSpent < goal.minGoal ->
                        "Min Goal: R%.2f | Max Goal: R%.2f\nSpent: R%.2f\nStatus: Below minimum goal"
                            .format(goal.minGoal, goal.maxGoal, totalSpent)

                    totalSpent <= goal.maxGoal ->
                        "Min Goal: R%.2f | Max Goal: R%.2f\nSpent: R%.2f\nStatus: Within goal range"
                            .format(goal.minGoal, goal.maxGoal, totalSpent)

                    else ->
                        "Min Goal: R%.2f | Max Goal: R%.2f\nSpent: R%.2f\nStatus: Over maximum goal"
                            .format(goal.minGoal, goal.maxGoal, totalSpent)
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Failed to load goals: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }
}