package com.example.foodrecognition.network

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

suspend fun analyzeFood(context: Context, imageUri: Uri): String =
    withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null || bytes.isEmpty()) {
                Log.e("FoodAnalyzer", "Image bytes are empty")
                return@withContext "No image found"
            }

            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "List all foods in this image and their calories per 100g. Format like: Food - Calories"))
                            put(JSONObject().put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            }))
                        })
                    })
                })
            }

            val client = OkHttpClient()
            val body = RequestBody.create("application/json".toMediaTypeOrNull(), requestJson.toString())
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=AIzaSyCS2ywOKbm3ZXuVNPnWWRpeGrpDdJJhK10")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val jsonResponse = response.body?.string()
            Log.d("FoodAnalyzer", "Response: $jsonResponse")

            val output = JSONObject(jsonResponse ?: return@withContext "Empty response from API")
            output.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?: "No food detected in image"
        } catch (e: Exception) {
            Log.e("FoodAnalyzer", "Error analyzing food", e)
            "Error analyzing food: ${e.message}"
        }
    }
