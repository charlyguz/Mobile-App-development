package com.example.icalculatorwithstaticlayouts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CalculatorViewModel : ViewModel() {

    /** The main number shown in large text */
    val display = MutableLiveData("0")

    /** The secondary expression line (e.g. "12 +") shown in small text above */
    val expression = MutableLiveData("")

    private var firstOperand: Double? = null
    private var pendingOperator: String? = null
    private var waitingForSecondOperand = false
    private var errorState = false

    // ── Digit input ──────────────────────────────────────────────────────────

    fun onDigit(digit: String) {
        if (errorState) reset()

        val current = display.value ?: "0"

        display.value = if (waitingForSecondOperand || current == "0") {
            waitingForSecondOperand = false
            digit
        } else {
            // Guard against excessively long numbers
            if (current.replace("-", "").replace(".", "").length >= 9) return
            current + digit
        }
    }

    // ── Decimal point ────────────────────────────────────────────────────────

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

    // ── Operators ────────────────────────────────────────────────────────────

    fun onOperator(operator: String) {
        if (errorState) return

        val currentValue = display.value?.toDoubleOrNull() ?: return

        if (firstOperand == null) {
            firstOperand = currentValue
        } else if (!waitingForSecondOperand) {
            val result = calculate(firstOperand!!, currentValue, pendingOperator ?: return)
            if (result == null) { showError(); return }
            firstOperand = result
            display.value = formatResult(result)
        }

        pendingOperator = operator
        waitingForSecondOperand = true

        // Show expression line: e.g. "12 +"
        val operatorDisplay = when (operator) {
            "*" -> "×"
            "/" -> "÷"
            "-" -> "−"
            else -> operator
        }
        expression.value = "${formatResult(firstOperand!!)} $operatorDisplay"
    }

    // ── Equals ───────────────────────────────────────────────────────────────

    fun onEquals() {
        if (errorState) return

        val first  = firstOperand   ?: return
        val op     = pendingOperator ?: return
        val second = display.value?.toDoubleOrNull() ?: return

        if (waitingForSecondOperand) return

        val result = calculate(first, second, op)
        if (result == null) { showError(); return }

        // Show full expression in small line, result in large
        val opDisplay = when (op) {
            "*" -> "×"
            "/" -> "÷"
            "-" -> "−"
            else -> op
        }
        expression.value = "${formatResult(first)} $opDisplay ${formatResult(second)} ="
        display.value = formatResult(result)

        firstOperand         = null
        pendingOperator      = null
        waitingForSecondOperand = false
    }

    // ── Clear ────────────────────────────────────────────────────────────────

    fun onClear() {
        reset()
    }

    // ── ± Negate ─────────────────────────────────────────────────────────────

    fun onNegate() {
        if (errorState) return
        val current = display.value?.toDoubleOrNull() ?: return
        val negated = -current
        display.value = formatResult(negated)
    }

    // ── % Percent ────────────────────────────────────────────────────────────

    fun onPercent() {
        if (errorState) return
        val current = display.value?.toDoubleOrNull() ?: return
        val result = if (firstOperand != null) {
            // iOS-style: percent of firstOperand (e.g. 200 + 15% → 200 + 30)
            firstOperand!! * (current / 100.0)
        } else {
            current / 100.0
        }
        display.value = formatResult(result)
    }

    // ── Private helpers ──────────────────────────────────────────────────────

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
        expression.value = ""
        firstOperand         = null
        pendingOperator      = null
        waitingForSecondOperand = false
        errorState = true
    }

    private fun reset() {
        display.value = "0"
        expression.value = ""
        firstOperand         = null
        pendingOperator      = null
        waitingForSecondOperand = false
        errorState = false
    }

    private fun formatResult(value: Double): String {
        // Avoid scientific notation for large-ish numbers
        if (value % 1.0 == 0.0 && kotlin.math.abs(value) < 1_000_000_000.0) {
            return value.toLong().toString()
        }
        // Trim trailing zeros for decimals
        val str = value.toBigDecimal().stripTrailingZeros().toPlainString()
        return str
    }
}
