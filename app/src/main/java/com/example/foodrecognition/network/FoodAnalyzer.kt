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
            // ---- Read image bytes ----
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null || bytes.isEmpty()) {
                Log.e("FoodAnalyzer", "Image bytes are empty")
                return@withContext "No image found"
            }
            Log.d("FoodAnalyzer", "Image bytes size: ${bytes.size}")

            // ---- Encode image to Base64 (Android version) ----
            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)

            // ---- Build Gemini request JSON ----
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

            // ---- Send request to Gemini ----
            val client = OkHttpClient()
            val body = RequestBody.create("application/json".toMediaTypeOrNull(), requestJson.toString())
            val request = Request.Builder()
                //.url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyCS2ywOKbm3ZXuVNPnWWRpeGrpDdJJhK10")
                .url("https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=AIzaSyCS2ywOKbm3ZXuVNPnWWRpeGrpDdJJhK10")

                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val jsonResponse = response.body?.string()
            Log.d("FoodAnalyzer", "Response: $jsonResponse")

            if (jsonResponse.isNullOrEmpty()) {
                return@withContext "Empty response from API"
            }

            // ---- Parse Gemini response safely ----
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

