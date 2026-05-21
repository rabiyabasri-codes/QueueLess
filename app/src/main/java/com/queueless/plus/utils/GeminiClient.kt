package com.queueless.plus.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val API_KEY = "AIzaSyA6XXs4YjI0mdHMEC7oNWCx5CrQsNEJr1U"
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$API_KEY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_CONTEXT = """You are QueueLessBot, the official AI Assistant for the QueueLessPlus Android application. 
QueueLessPlus is a premium queue management app designed to eliminate physical waiting lines in food courts, university cafeterias, and retail shops.

App features and options:
1. For Customers:
   - Browse/search nearby queues (e.g. MM Foods, Mm Foods(Admin Block), Food Court).
   - View live queue statistics: active queues, waiting users, estimated wait times (e.g., ~5 min/person).
   - Join queues digitally and receive realtime status updates/notifications.
   - Scan QR codes on-site to view queue details and join instantly.
   - View menus, place food/retail orders directly, and track order progress/history.
2. For Admins:
   - Create, customize, and manage active queues.
   - Manage menus (add items, set prices, write descriptions).
   - View live queue analytics, track entries, and communicate with waiting users.

Role instructions:
- Be extremely helpful, professional, friendly, and act as the representative of QueueLessPlus.
- Assist users with queries about how the app works, how to join queues, menu details, estimated times, or general questions.
- Keep answers clear, concise, visually formatted in markdown, and optimized for mobile screens.

Question: """

    suspend fun getAssistantResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val fullPrompt = SYSTEM_CONTEXT + prompt

        // Construct JSON Payload for Gemini API
        val contentsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", fullPrompt)
                    })
                })
            })
        }
        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
        }

        val requestBody = requestJson.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    android.util.Log.e("GeminiClient", "Gemini request failed (code=${response.code}, body=$bodyString)")
                    throw Exception("Gemini request failed (code=${response.code}, body=$bodyString)")
                }
                parseGeminiResponse(bodyString)
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiClient", "Error in GeminiClient", e)
            "Sorry, the QueueLess AI Assistant is currently experiencing connection issues. Please try again shortly."
        }
    }

    private fun parseGeminiResponse(responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            val candidates = json.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    parts.getJSONObject(0).getString("text")
                } else {
                    "Sorry, I couldn't generate a response."
                }
            } else {
                "Sorry, I couldn't generate a response."
            }
        } catch (e: Exception) {
            "Sorry, I couldn't parse the response."
        }
    }
}
