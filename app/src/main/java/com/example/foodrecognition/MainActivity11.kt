package com.example.foodrecognition

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull

/*
data class FoodItem(val name: String, val calories: Int)

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            MaterialTheme {
                CameraScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var foodList by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val imageCapture = remember { ImageCapture.Builder().build() }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            Toast.makeText(context, "Camera permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (exc: Exception) {
                    Log.e("CameraScreen", "Use case binding failed", exc)
                }

                previewView
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Button(onClick = {
            val photoFile = File(
                context.cacheDir,
                "photo_${System.currentTimeMillis()}.jpg"
            )

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("Camera", "Photo capture failed: ${exc.message}", exc)
                        Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        imageUri = Uri.fromFile(photoFile)
                        Toast.makeText(context, "Photo saved", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }) {
            Text("Capture Photo")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                imageUri?.let { uri ->
                    coroutineScope.launch {
                        foodList = analyzeFood(uri, context)
                    }
                } ?: Toast.makeText(context, "No photo taken yet", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Analyze Food")
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            items(foodList) { food ->
                Text("${food.name} - ${food.calories} cal / 100g")
            }
        }
    }
}

suspend fun analyzeFood(photoUri: Uri, context: android.content.Context): List<FoodItem> =
    withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(photoUri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
            val base64Image =
                Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)

            val json = """
            {
              "contents": [
                {
                  "parts": [
                    { "text": "List each food in this image and its calories per 100g. Return JSON array like [{name: 'Apple', calories: 52}]" },
                    {
                      "inline_data": {
                        "mime_type": "image/jpeg",
                        "data": "$base64Image"
                      }
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

            val body = RequestBody.create("application/json".toMediaTypeOrNull(), json)

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=AIzaSyCS2ywOKbm3ZXuVNPnWWRpeGrpDdJJhK10")
                .post(body)
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("API_RESPONSE", responseBody ?: "null")

            if (responseBody != null && response.isSuccessful) {
                val jsonObject = JSONObject(responseBody)
                val text = jsonObject
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                Log.d("API_PARSED", text)

                // Parse response JSON (from Gemini text)
                val items = mutableListOf<FoodItem>()
                val clean = text.replace("```json", "")
                    .replace("```", "")
                    .trim()

                val foods = org.json.JSONArray(clean)
                for (i in 0 until foods.length()) {
                    val obj = foods.getJSONObject(i)
                    items.add(FoodItem(obj.getString("name"), obj.getInt("calories")))
                }
                items
            } else {
                Log.e("API_ERROR", "Response failed or empty")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("analyzeFood", "Error analyzing food", e)
            emptyList()
        }
    } */
