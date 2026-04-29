package com.example.icalculatorwithstaticlayouts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CalculatorViewModel : ViewModel() {

    val display = MutableLiveData("0")

    private var firstOperand: Double? = null
    private var pendingOperator: String? = null
    private var waitingForSecondOperand = false
    private var errorState = false

    fun onDigit(digit: String) {
        if (errorState) reset()

        val current = display.value ?: "0"

        display.value = if (waitingForSecondOperand || current == "0") {
            waitingForSecondOperand = false
            digit
        } else {
            current + digit
        }
    }

    fun onDecimal() {
        if (errorState) reset()

        val current = display.value ?: "0"

        if (waitingForSecondOperand) {
            display.value = "0."
            waitingForSecondOperand = false
            return
        }

        if (!current.contains(".")) {
            display.value = current + "."
        }
    }

    fun onOperator(operator: String) {
        if (errorState) return

        val currentValue = display.value?.toDoubleOrNull() ?: return

        if (firstOperand == null) {
            firstOperand = currentValue
        } else if (!waitingForSecondOperand) {
            val result = calculate(firstOperand!!, currentValue, pendingOperator ?: return)
            if (result == null) {
                showError()
                return
            }
            firstOperand = result
            display.value = formatResult(result)
        }

        pendingOperator = operator
        waitingForSecondOperand = true
    }

    fun onEquals() {
        if (errorState) return

        val first = firstOperand ?: return
        val operator = pendingOperator ?: return
        val second = display.value?.toDoubleOrNull() ?: return

        if (waitingForSecondOperand) return

        val result = calculate(first, second, operator)
        if (result == null) {
            showError()
            return
        }

        display.value = formatResult(result)
        firstOperand = null
        pendingOperator = null
        waitingForSecondOperand = false
    }

    fun onClear() {
        reset()
    }

    private fun calculate(a: Double, b: Double, operator: String): Double? {
        return when (operator) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b == 0.0) null else a / b
            else -> null
        }
    }

    private fun showError() {
        display.value = "Error"
        firstOperand = null
        pendingOperator = null
        waitingForSecondOperand = false
        errorState = true
    }

    private fun reset() {
        display.value = "0"
        firstOperand = null
        pendingOperator = null
        waitingForSecondOperand = false
        errorState = false
    }

    private fun formatResult(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString().trimEnd('0').trimEnd('.')
        }
    }
}
