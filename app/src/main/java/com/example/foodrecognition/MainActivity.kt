package com.example.foodrecognition

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.foodrecognition.network.analyzeFood
import com.example.foodrecognition.ui.screens.CameraScreen
import com.example.foodrecognition.theme.FoodRecognitionTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodRecognitionTheme {
                var photoUri by remember { mutableStateOf<Uri?>(null) }
                val context = LocalContext.current

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // CameraScreen composable
                    CameraScreen { uri ->
                        Log.d("MainActivity", "Captured photo URI: $uri")
                        photoUri = uri

                        // Call analyzeFood inside coroutine
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                analyzeFood(context, uri)
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Food analysis failed: ${e.message}")
                            }
                        }
                    }

                    // Show captured image
                    photoUri?.let { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "Captured photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Optional: analyze button
                    photoUri?.let { uri ->
                        Button(onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    analyzeFood(context, uri)
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Food analysis failed: ${e.message}")
                                }
                            }
                        }) {
                            Text("Analyze Food")
                        }
                    }
                }
            }
        }
    }
}

