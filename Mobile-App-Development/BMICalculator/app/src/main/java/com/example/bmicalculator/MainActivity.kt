package com.example.bmicalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bmicalculator.ui.theme.BMICalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BMICalculatorTheme {
                BMICalculatorScreen()
            }
        }
    }
}

// ─── Data ────────────────────────────────────────────────────────────────────

data class BmiCategory(
    val label: String,
    val emoji: String,
    val color: Color,
    val range: String
)

val categories = listOf(
    BmiCategory("Underweight", "🦴", Color(0xFF64B5F6), "< 18.5"),
    BmiCategory("Healthy",     "💪", Color(0xFF66BB6A), "18.5 – 24.9"),
    BmiCategory("Overweight",  "⚠️", Color(0xFFFFA726), "25 – 29.9"),
    BmiCategory("Obesity",     "🚨", Color(0xFFEF5350), "≥ 30")
)

fun categoryIndex(bmi: Double) = when {
    bmi < 18.5 -> 0
    bmi < 25.0 -> 1
    bmi < 30.0 -> 2
    else       -> 3
}

// ─── Input sanitizer ─────────────────────────────────────────────────────────

fun sanitize(input: String): String = input.trim().replace(',', '.')

fun validateInputs(weightRaw: String, heightRaw: String): String? {
    if (weightRaw.isBlank() || heightRaw.isBlank())
        return "Please fill in both fields."

    val weight = sanitize(weightRaw).toDoubleOrNull()
        ?: return "Weight must be a number (e.g. 70 or 70.5)."
    val height = sanitize(heightRaw).toDoubleOrNull()
        ?: return "Height must be a number (e.g. 1.75)."

    if (weight <= 0 || height <= 0)
        return "Values must be greater than zero."
    if (weight > 500)
        return "Weight seems unrealistic (max 500 kg)."
    if (height < 0.5 || height > 2.72)
        return "Enter height in meters — e.g. 1.75, not 175."

    return null // no error
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun BMICalculatorScreen() {
    var weightInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    var bmiResult   by remember { mutableStateOf<Double?>(null) }
    var errorMsg    by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFE8EAF6), Color(0xFFF5F5F5))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "BMI",
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3F51B5)
            )
            Text(
                text = "Calculator",
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF5C6BC0),
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(36.dp))

            // Input card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    StyledTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = "Weight (kg)",
                        placeholder = "e.g. 70 or 70,5"
                    )

                    Spacer(Modifier.height(12.dp))

                    StyledTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = "Height (m)",
                        placeholder = "e.g. 1.75 or 1,75"
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Calculate button
            Button(
                onClick = {
                    val err = validateInputs(weightInput, heightInput)
                    if (err != null) {
                        errorMsg = err
                        bmiResult = null
                    } else {
                        errorMsg = ""
                        val w = sanitize(weightInput).toDouble()
                        val h = sanitize(heightInput).toDouble()
                        bmiResult = w / (h * h)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                Text(
                    "Calculate my BMI",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Error
            AnimatedVisibility(
                visible = errorMsg.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("❌  ", fontSize = 16.sp)
                        Text(errorMsg, color = Color(0xFFC62828), fontSize = 14.sp)
                    }
                }
            }

            // Result
            AnimatedVisibility(
                visible = bmiResult != null,
                enter = fadeIn() + expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                bmiResult?.let { bmi ->
                    Spacer(Modifier.height(24.dp))
                    BMIResultCard(bmi)
                }
            }
        }
    }
}

// ─── Custom TextField ─────────────────────────────────────────────────────────

@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Color.LightGray) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF3F51B5),
            focusedLabelColor = Color(0xFF3F51B5)
        ),
        singleLine = true
    )
}

// ─── Result Card ─────────────────────────────────────────────────────────────

@Composable
fun BMIResultCard(bmi: Double) {
    val idx = categoryIndex(bmi)
    val cat = categories[idx]

    // Animated BMI number counting up
    val animatedBmi by animateFloatAsState(
        targetValue = bmi.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = EaseOut),
        label = "bmi_anim"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big emoji
            Text(cat.emoji, fontSize = 56.sp)

            Spacer(Modifier.height(8.dp))

            // BMI value (animated)
            Text(
                text = String.format("%.1f", animatedBmi),
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                color = cat.color
            )
            Text(
                "Your BMI",
                fontSize = 13.sp,
                color = Color.Gray,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(20.dp))

            // BMI bar with marker
            BMIBar(bmi = bmi)

            Spacer(Modifier.height(20.dp))

            // Diagnosis badge
            Box(
                modifier = Modifier
                    .background(cat.color, RoundedCornerShape(50.dp))
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Text(
                    text = cat.label,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Range: ${cat.range}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

// ─── BMI Bar ─────────────────────────────────────────────────────────────────

@Composable
fun BMIBar(bmi: Double) {
    // Map BMI to a 0–1 fraction across a 10–40 display range
    val minBmi = 10f
    val maxBmi = 40f
    val fraction = ((bmi.toFloat() - minBmi) / (maxBmi - minBmi)).coerceIn(0f, 1f)

    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 900, easing = EaseOut),
        label = "bar_anim"
    )

    Column {
        // Colored bar
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                categories.forEach { cat ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(cat.color)
                    )
                }
            }

            // Marker
            val markerOffsetDp = maxWidth * animatedFraction
            Box(
                modifier = Modifier
                    .offset(x = markerOffsetDp - 7.dp)
                    .size(14.dp)
                    .background(Color.White, RoundedCornerShape(50))
                    .align(Alignment.CenterStart)
            )
        }

        Spacer(Modifier.height(4.dp))

        // Labels
        Row(modifier = Modifier.fillMaxWidth()) {
            categories.forEach { cat ->
                Text(
                    text = cat.label,
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
                    color = cat.color,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}