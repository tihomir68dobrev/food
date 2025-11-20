
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.foodrecognition

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodrecognition.data.entities.MealEntity
import com.example.foodrecognition.viewmodel.MealViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ---------------------------
data class FoodItem(
    val name: String,
    val calories: Int,
    var gramsInput: String = ""
)
// ---------------------------

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            MaterialTheme {

                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "camera") {

                    composable("camera") {
                        CameraScreen(navController)
                    }

                    composable("days_history") {
                        DaysHistoryScreen(navController)
                    }

                    composable("history/{day}") { backStack ->
                        val day = backStack.arguments?.getString("day")!!.toLong()
                        HistoryScreenForDay(day)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

// --------------------------------------------------------
// CAMERA SCREEN
// --------------------------------------------------------

@Composable
fun CameraScreen(navController: NavController) {

    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val viewModel: MealViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    )

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var foodList by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
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
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            val photoFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "photo_${System.currentTimeMillis()}.jpg"
            )

            val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                output,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
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
                } ?: Toast.makeText(context, "No photo", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Analyze Food")
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            itemsIndexed(foodList) { index, food ->

                val grams = food.gramsInput

                val caloriesForFood = grams.toDoubleOrNull()?.let {
                    food.calories * it / 100.0
                } ?: 0.0

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {

                        Text(food.name, style = MaterialTheme.typography.titleMedium)
                        Text("Calories / 100g: ${food.calories}")

                        OutlinedTextField(
                            value = grams,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }
                                foodList = foodList.toMutableList().also {
                                    it[index] = it[index].copy(gramsInput = filtered)
                                }
                            },
                            label = { Text("Grams") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text("Calories: %.1f kcal".format(caloriesForFood))
                    }
                }
            }
        }

        val totalCalories = foodList.sumOf { f ->
            f.gramsInput.toDoubleOrNull()?.let { it * f.calories / 100.0 } ?: 0.0
        }

        Text(
            "Total Calories: %.1f".format(totalCalories),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = {
                if (imageUri == null || foodList.isEmpty()) {
                    Toast.makeText(context, "Nothing to save", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val jsonItems = JSONArray().apply {
                    foodList.forEach {
                        put(JSONObject().apply {
                            put("name", it.name)
                            put("calories", it.calories)
                            put("grams", it.gramsInput)
                        })
                    }
                }.toString()

                val meal = MealEntity(
                    timestamp = System.currentTimeMillis(),
                    imagePath = imageUri!!.path,
                    totalCalories = totalCalories.toInt(),
                    itemsJson = jsonItems
                )

                viewModel.saveMeal(meal)
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()

            }) {
                Text("SAVE")
            }

            Button(onClick = {
                navController.navigate("days_history")
            }) {
                Text("HISTORY")
            }
        }
    }
}

// --------------------------------------------------------
// DAYS HISTORY SCREEN (daily summary)
// --------------------------------------------------------
@Composable
fun DaysHistoryScreen(navController: NavController) {

    val context = LocalContext.current
    val viewModel: MealViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    )

    val meals by viewModel.history.collectAsState(initial = emptyList())

    val grouped = meals.groupBy { meal ->
        SimpleDateFormat("yyyyMMdd").format(meal.timestamp)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text("Daily History", style = MaterialTheme.typography.titleLarge)

        LazyColumn {
            grouped.forEach { (dayKey, dayMeals) ->

                val total = dayMeals.sumOf { it.totalCalories }
                val dayReadable =
                    SimpleDateFormat("dd/MM/yyyy").format(dayMeals.first().timestamp)

                item {
                    Card(
                        Modifier.fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                navController.navigate("history/$dayKey")
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Date: $dayReadable")
                            Text("Total calories: $total kcal")
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------
// HISTORY FOR SPECIFIC DAY
// --------------------------------------------------------
@Composable
fun HistoryScreenForDay(dayKey: Long) {

    val context = LocalContext.current
    val viewModel: MealViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application)
    )

    val meals by viewModel.history.collectAsState(initial = emptyList())

    val filteredMeals = meals.filter { meal ->
        SimpleDateFormat("yyyyMMdd").format(meal.timestamp).toLong() == dayKey
    }

    val readableDate = filteredMeals.firstOrNull()?.let {
        SimpleDateFormat("dd/MM/yyyy").format(it.timestamp)
    } ?: ""

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text("Meals for $readableDate", style = MaterialTheme.typography.titleLarge)

        LazyColumn {
            itemsIndexed(filteredMeals) { _, meal ->

                Card(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {

                        val time = SimpleDateFormat("HH:mm").format(meal.timestamp)

                        // -------------------------
                        // PHOTO OF THE MEAL
                        // -------------------------
                        meal.imagePath?.let { path ->
                            val file = File(path)
                            if (file.exists()) {
                                val bmp = android.graphics.BitmapFactory.decodeFile(path)
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }

                        Text("Time: $time")
                        Text("Total calories: ${meal.totalCalories} kcal")

                        Spacer(Modifier.height(6.dp))
                        Text("Items:", style = MaterialTheme.typography.titleMedium)

                        // Show items parsed nicely
                        val jsonArray = JSONArray(meal.itemsJson)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val name = obj.getString("name")
                            val cal = obj.getInt("calories")
                            val g = obj.getString("grams")
                            Text("• $name — $g g — $cal kcal /100g")
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------
// ANALYZE FOOD
// --------------------------------------------------------

suspend fun analyzeFood(photoUri: Uri, context: android.content.Context): List<FoodItem> =
    withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(photoUri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
            val base64Image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)

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
            """

            val body = RequestBody.create("application/json".toMediaTypeOrNull(), json)

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=AIzaSyCS2ywOKbm3ZXuVNPnWWRpeGrpDdJJhK10")
                .post(body)
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (responseBody != null && response.isSuccessful) {
                val jsonObject = JSONObject(responseBody)
                val text = jsonObject
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val clean = text.replace("```json", "").replace("```", "").trim()

                val items = mutableListOf<FoodItem>()
                val foods = JSONArray(clean)

                for (i in 0 until foods.length()) {
                    val obj = foods.getJSONObject(i)
                    items.add(
                        FoodItem(
                            obj.getString("name"),
                            obj.getInt("calories")
                        )
                    )
                }
                items
            } else emptyList()

        } catch (e: Exception) {
            Log.e("analyzeFood", "Error", e)
            emptyList()
        }
    }










