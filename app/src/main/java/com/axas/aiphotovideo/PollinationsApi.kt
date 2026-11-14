package com.axas.aiphotovideo

import android.graphics.Bitmap
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object PollinationsApi {
    private fun connection(url: String, key: String, method: String = "GET"): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 30000
            readTimeout = 300000
            setRequestProperty("Authorization", "Bearer $key")
        }
    }

    fun generateImage(prompt: String, key: String): Bitmap {
        val encoded = URLEncoder.encode(prompt, "UTF-8")
        val conn = connection("https://gen.pollinations.ai/image/$encoded?model=flux&width=1024&height=1024", key)
        try {
            if (conn.responseCode !in 200..299) {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw Exception("AI ծառայության սխալ ${conn.responseCode}: ${error.take(180)}")
            }
            return android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                ?: throw Exception("Ստացված նկարը հնարավոր չեղավ բացել")
        } finally { conn.disconnect() }
    }

    fun animateImage(bitmap: Bitmap, prompt: String, key: String, output: File): File {
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        val boundary = "AnnaAI-${System.currentTimeMillis()}"
        val upload = connection("https://media.pollinations.ai/upload", key, "POST").apply {
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        val mediaUrl: String
        try {
            upload.outputStream.use { out ->
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Disposition: form-data; name=\"file\"; filename=\"anna-ai.jpg\"\r\n".toByteArray())
                out.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
                out.write(bytes)
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }
            if (upload.responseCode !in 200..299) {
                val error = upload.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw Exception("Նկարի վերբեռնման սխալ ${upload.responseCode}: ${error.take(160)}")
            }
            val json = JSONObject(upload.inputStream.bufferedReader().use { it.readText() })
            mediaUrl = json.optString("url").takeIf { it.startsWith("https://") }
                ?: throw Exception("Նկարի հղումը չստացվեց")
        } finally { upload.disconnect() }

        val encodedPrompt = URLEncoder.encode(prompt.ifBlank { "Animate this image naturally, cinematic camera movement" }, "UTF-8")
        val encodedImage = URLEncoder.encode(mediaUrl, "UTF-8")
        val videoUrl = "https://gen.pollinations.ai/video/$encodedPrompt?model=google/veo-3.1-fast&duration=5&image=$encodedImage"
        val video = connection(videoUrl, key)
        try {
            if (video.responseCode !in 200..299) {
                val error = video.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw Exception("AI վիդեոյի սխալ ${video.responseCode}: ${error.take(180)}")
            }
            output.parentFile?.mkdirs()
            video.inputStream.use { input -> output.outputStream().use { input.copyTo(it) } }
            if (!output.exists() || output.length() < 1024) throw Exception("Վիդեոն դատարկ է")
            return output
        } catch (e: Exception) {
            output.delete()
            throw e
        } finally { video.disconnect() }
    }
}
