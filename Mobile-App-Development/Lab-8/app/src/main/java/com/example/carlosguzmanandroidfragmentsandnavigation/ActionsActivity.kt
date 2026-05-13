package com.example.carlosguzmanandroidfragmentsandnavigation

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ActionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actions)

        val editText = findViewById<EditText>(R.id.actions_edit_text)

        findViewById<Button>(R.id.write_to_logcat_btn).setOnClickListener {
            Log.d("ActionsActivity", "Button clicked: Write to LogCat")
        }

        findViewById<Button>(R.id.show_toast_btn).setOnClickListener {
            Toast.makeText(this, "Hello from Actions Activity", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.write_to_edit_text_btn).setOnClickListener {
            editText.setText("Text written from button")
        }

        findViewById<Button>(R.id.back_to_first_fragment_btn).setOnClickListener {
            finish()
        }
    }
}