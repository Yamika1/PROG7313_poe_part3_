package com.example.prog7313_poe_part2

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var editTextText: EditText
    private lateinit var editTextText2: EditText
    private lateinit var button: Button
    private lateinit var button2: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        textView = findViewById(R.id.textView)
        editTextText = findViewById(R.id.editTextText)
        editTextText2 = findViewById(R.id.editTextText2)
        button = findViewById(R.id.button)
        button2 = findViewById(R.id.button2)

        textView.paintFlags = Paint.UNDERLINE_TEXT_FLAG

        button.setOnClickListener {
            val username = editTextText.text.toString().trim()
            val password = editTextText2.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all the fields", Toast.LENGTH_SHORT).show()
            } else {
                login(username, password)
            }
        }

        button2.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Welcome Back!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Home::class.java))
                    finish()
                } else {
             Toast.makeText(this, "Login Failed: ${task.exception?.message}",
                 Toast.LENGTH_SHORT).show()
                }
            }
    }
}