package com.queueless.plus.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object HuggingFaceClient {
    private const val API_URL = "https://api-inference.huggingface.co/models/facebook/blenderbot-400M-distill"
    private const val API_KEY = "YOUR_HUGGING_FACE_API_KEY"
    private val client = OkHttpClient()

    suspend fun getAssistantResponse(prompt: String): String {
        val requestJson = JSONObject().apply {
            put("inputs", prompt)
            put("parameters", JSONObject().apply {
                put("max_new_tokens", 150)
                put("temperature", 0.7)
            })
        }

        val requestBody = requestJson.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .post(requestBody)
            .build()

        val responseString = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Hugging Face request failed (code=${response.code})")
                }
                response.body?.string().orEmpty()
            }
        }

        return parseResponse(responseString)
    }

    private fun parseResponse(responseBody: String): String {
        if (responseBody.isBlank()) {
            return "Sorry, I couldn't generate a response."
        }

        return try {
            val parsed = JSONObject(responseBody)
            when {
                parsed.has("generated_text") -> parsed.getString("generated_text")
                parsed.has("error") -> "Sorry, the AI model returned an error."
                else -> extractFromArray(responseBody)
            }
        } catch (jsonException: Exception) {
            extractFromArray(responseBody)
        }
    }

    private fun extractFromArray(responseBody: String): String {
        return try {
            val array = JSONArray(responseBody)
            if (array.length() > 0) {
                val first = array.getJSONObject(0)
                when {
                    first.has("generated_text") -> first.getString("generated_text")
                    first.has("error") -> "Sorry, the AI model returned an error."
                    else -> first.optString("generated_text", "Sorry, I couldn't generate a response.")
                }
            } else {
                "Sorry, I couldn't generate a response."
            }
        } catch (e: Exception) {
            "Sorry, I couldn't generate a response."
        }
    }
}
