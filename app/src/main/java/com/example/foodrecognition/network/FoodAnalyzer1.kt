package com.example.foodrecognition.network

import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.Base64
import okhttp3.MediaType.Companion.toMediaTypeOrNull

suspend fun analyzeFood1(context: Context, imageUri: Uri): String {
    return try {
        // Load image bytes
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val bytes = inputStream?.readBytes()
        if (bytes == null || bytes.isEmpty()) {
            Log.e("FoodAnalyzer", "Image bytes are empty")
            return "No image found"
        }
        Log.d("FoodAnalyzer", "Image bytes size: ${bytes.size}")

        // Encode to Base64
        val base64Image = Base64.getEncoder().encodeToString(bytes)

        // Build request JSON
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "List the foods and their calories per 100g in this image."))
                        put(JSONObject().put("inline_data", JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", base64Image)
                        }))
                    })
                })
            })
        }
        Log.d("FoodAnalyzer", "Request JSON: $requestJson")

        // Make HTTP request
        val client = OkHttpClient()
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), requestJson.toString())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyCS2ywOKbm3ZXuVNPnWWRpeGrpDdJJhK10")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val jsonResponse = response.body?.string()
        Log.d("FoodAnalyzer", "Response: $jsonResponse")

        if (jsonResponse.isNullOrEmpty()) {
            return "Empty response from API"
        }

        // Parse response safely
        val output = JSONObject(jsonResponse)
        val text = output.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")

        text ?: "No food detected in image"

    } catch (e: Exception) {
        Log.e("FoodAnalyzer", "Error analyzing food", e)
        "Error analyzing food: ${e.message}"
    }
}
