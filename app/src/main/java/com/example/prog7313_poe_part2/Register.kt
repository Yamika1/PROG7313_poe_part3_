package com.example.prog7313_poe_part2

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


class Register : AppCompatActivity(){

    private lateinit var heading: TextView
    private lateinit var editTextFName: EditText
    private lateinit var editTextLName: EditText
    private lateinit var editTextUsername : EditText
    private lateinit var editTextEmail : EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText

    private lateinit var buttonLogin : Button

    private lateinit var buttonRegister : Button

    private lateinit var imageButtonProfilePic : ImageButton
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        imageButtonProfilePic = findViewById(R.id.imageButtonProfilePic)
        heading = findViewById(R.id.heading)
        editTextFName = findViewById(R.id.editTextFName)
        editTextLName = findViewById(R.id.editTextLName)
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegister = findViewById(R.id.buttonRegister)
        auth = FirebaseAuth.getInstance()

        heading.paintFlags = Paint.UNDERLINE_TEXT_FLAG


        buttonRegister.setOnClickListener {
            registerUser()
        }

        buttonLogin.setOnClickListener {
            openLoginScreen()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun registerUser() {

        val firstName = editTextFName.text.toString().trim()
        val lastName = editTextLName.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val userName = editTextUsername.text.toString().trim()
        val password = editTextPassword.text.toString().trim()
        val confirmPassword = editTextConfirmPassword.text.toString().trim()




        if (firstName.isEmpty() || lastName.isEmpty()|| email.isEmpty() || userName.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return

        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
            return

                    clearFields()
                    openLoginScreen()
                }
            }




    private fun openLoginScreen() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()

    }

    private fun clearFields(){
        editTextFName.text.clear()
        editTextLName.text.clear()
        editTextUsername.text.clear()
        editTextEmail.text.clear()
        editTextPassword.text.clear()
        editTextConfirmPassword.text.clear()
    }
}
