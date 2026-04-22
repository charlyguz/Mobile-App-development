package com.example.carlosguzmanandroidpart2

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val ctx = LocalContext.current

            var dynamicText by remember { mutableStateOf("My old text") }
            var shouldTextsBePresented by remember { mutableStateOf(false) }
            var tfValue by remember { mutableStateOf("") }

            Box(modifier = Modifier.fillMaxSize()) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            Log.i("myapp", "Entry from my application")
                        }
                    ) {
                        Text("Save to LogCat")
                    }

                    Button(
                        onClick = {
                            Toast.makeText(ctx, "Hello There!", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Text("Show Toast")
                    }

                    Button(
                        onClick = {
                            dynamicText =
                                if (dynamicText == "My old text") "My new text" else "My old text"
                        }
                    ) {
                        Text("Change the text")
                    }

                    Text(
                        text = dynamicText,
                        fontSize = 20.sp
                    )

                    Button(
                        onClick = {
                            shouldTextsBePresented = !shouldTextsBePresented
                        }
                    ) {
                        Text(
                            if (shouldTextsBePresented) {
                                "Hide colored texts"
                            } else {
                                "Display colored texts"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = tfValue,
                        onValueChange = { tfValue = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (shouldTextsBePresented) {
                    MyTexts()
                }
            }
        }
    }
}

@Composable
fun MyTexts() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "Hello There!",
            color = Color.Red,
            fontSize = 20.sp,
            letterSpacing = 3.sp
        )
        Text(
            text = "I'm doing so great",
            color = Color.Blue,
            fontSize = 20.sp,
            letterSpacing = 3.sp
        )
        Text(
            text = "And android is so cool",
            color = Color.Magenta,
            fontSize = 20.sp,
            letterSpacing = 3.sp
        )
    }
}