package com.example.foodrecognition

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodrecognition.ui.screens.CameraScreen
import com.example.foodrecognition.ui.screens.UploadImageScreen
import com.example.foodrecognition.ui.screens.ResultScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                var imageUri by remember { mutableStateOf<Uri?>(null) }

            /*    NavHost(navController, startDestination = "camera") {
                    composable("camera") {
                        CameraScreen(onImageCaptured = { uri ->
                            imageUri = uri
                            navController.navigate("result")
                        })
                    }
                    composable("result") {
                        imageUri?.let { ResultScreen(it) }
                    }
                }

             */

                NavHost(navController, startDestination = "upload") {
                    // Upload image screen (replaces camera)
                    composable("upload") {
                        UploadImageScreen(onImageSelected = { uri ->
                            imageUri = uri
                            navController.navigate("result")
                        })
                    }

                    // Result screen
                    composable("result") {
                        imageUri?.let { ResultScreen(it) }
                    }
                }


            }
        }
    }
}
