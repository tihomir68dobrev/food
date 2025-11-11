package com.example.foodrecognition.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.foodrecognition.network.analyzeFood
import kotlinx.coroutines.launch

@Composable
fun ResultScreen(imageUri: Uri) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var resultText by remember { mutableStateOf("Analyzing your meal... 🍽️") }

    LaunchedEffect(imageUri) {
        scope.launch {
            val result = analyzeFood(context, imageUri)
            resultText = result
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(resultText)
    }
}

