package com.example.carremote.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

sealed class CarCommandResult {
    data object Success : CarCommandResult()
    data class Failure(val message: String) : CarCommandResult()
}

class CarApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private fun hmacSha256(secret: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val bytes = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun sendCommand(
        ipAddress: String,
        sharedSecret: String,
        command: String
    ): CarCommandResult {
        return try {
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val nonce = UUID.randomUUID().toString()
            val message = "$command:$timestamp:$nonce"
            val signature = hmacSha256(sharedSecret, message)

            val url = "http://$ipAddress/$command?ts=$timestamp&nonce=$nonce&sig=$signature"

            val request = Request.Builder()
                .url(url)
                .post("".toRequestBody(null))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    CarCommandResult.Success
                } else {
                    CarCommandResult.Failure("فشل الطلب: ${response.code}")
                }
            }
        } catch (e: Exception) {
            CarCommandResult.Failure("تعذر الاتصال بالجهاز: ${e.message}")
        }
    }

    suspend fun checkStatus(ipAddress: String): Boolean {
        return try {
            val request = Request.Builder().url("http://$ipAddress/status").get().build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}
