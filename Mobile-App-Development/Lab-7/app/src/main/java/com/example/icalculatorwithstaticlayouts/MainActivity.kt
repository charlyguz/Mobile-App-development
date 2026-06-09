package com.example.icalculatorwithstaticlayouts

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.icalculatorwithstaticlayouts.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var vm: CalculatorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        vm = ViewModelProvider(this)[CalculatorViewModel::class.java]
        binding.vm = vm
        binding.lifecycleOwner = this

        setButtonListeners()

        // Apply window insets so content is not hidden behind system bars
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

    private fun setButtonListeners() {
        // Digits
        binding.btn0.setOnClickListener { vm.onDigit("0") }
        binding.btn1.setOnClickListener { vm.onDigit("1") }
        binding.btn2.setOnClickListener { vm.onDigit("2") }
        binding.btn3.setOnClickListener { vm.onDigit("3") }
        binding.btn4.setOnClickListener { vm.onDigit("4") }
        binding.btn5.setOnClickListener { vm.onDigit("5") }
        binding.btn6.setOnClickListener { vm.onDigit("6") }
        binding.btn7.setOnClickListener { vm.onDigit("7") }
        binding.btn8.setOnClickListener { vm.onDigit("8") }
        binding.btn9.setOnClickListener { vm.onDigit("9") }

        // Decimal
        binding.btnDot.setOnClickListener { vm.onDecimal() }

        // Operators
        binding.btnPlus.setOnClickListener     { vm.onOperator("+") }
        binding.btnMinus.setOnClickListener    { vm.onOperator("-") }
        binding.btnMultiply.setOnClickListener { vm.onOperator("*") }
        binding.btnDivide.setOnClickListener   { vm.onOperator("/") }

        // Actions
        binding.btnEquals.setOnClickListener  { vm.onEquals() }
        binding.btnClear.setOnClickListener   { vm.onClear() }
        binding.btnNegate.setOnClickListener  { vm.onNegate() }
        binding.btnPercent.setOnClickListener { vm.onPercent() }
    }
}