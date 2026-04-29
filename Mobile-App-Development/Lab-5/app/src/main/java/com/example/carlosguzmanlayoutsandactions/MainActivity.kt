package com.example.carlosguzmanlayoutsandactions

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.carlosguzmanlayoutsandactions.databinding.ActivityActionsBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActionsBinding
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityActionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        user = ViewModelProvider(this)[User::class.java]
        user.firstName.value = "John"
        user.lastName.value = "Doe"

        binding.user = user
        binding.lifecycleOwner = this

        setButtonListeners()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }

    fun writeToLogCat(view: View) {
        Log.i("MyApp", "Message from my App")
    }

    private fun setButtonListeners() {
        binding.btnWriteToLogcatListener.setOnClickListener {
            Log.i("MyAppList", "Message from the Listener")
        }

        binding.btnShowToast.setOnClickListener {
            Toast.makeText(
                this,
                "Message From My App",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.btnWriteToTextview.setOnClickListener {
            binding.tvWriteSomething.text = getString(R.string.i_love_this_game)
        }

        binding.btnUpdateUser.setOnClickListener {
            user.firstName.value = "Radek"
            user.lastName.value = "Radlinski"
        }
    }
}