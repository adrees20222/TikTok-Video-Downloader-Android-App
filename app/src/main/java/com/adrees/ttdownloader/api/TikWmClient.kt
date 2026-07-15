package com.adrees.ttdownloader.api

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

data class TTVideoInfo(
    val id: String,
    val title: String,
    val coverUrl: String,
    val videoUrl: String,
    val authorName: String
)

object TikWmClient {
    private val client = OkHttpClient()

    suspend fun fetchVideoInfo(videoUrl: String): Result<TTVideoInfo> = withContext(Dispatchers.IO) {
        val apiBuilder = HttpUrl.Builder()
            .scheme("https")
            .host("www.tikwm.com")
            .encodedPath("/api/")
            .addQueryParameter("url", videoUrl)
            .build()

        val request = Request.Builder()
            .url(apiBuilder)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error: ${response.code}"))
                }
                val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty API response"))
                val json = JSONObject(bodyString)
                val code = json.optInt("code", -1)
                if (code != 0) {
                    val msg = json.optString("msg", "Failed to extract video details.")
                    return@withContext Result.failure(Exception(msg))
                }

                val dataObj = json.optJSONObject("data") ?: return@withContext Result.failure(Exception("API returned no data"))
                val id = dataObj.optString("id", "")
                val title = dataObj.optString("title", "TT Video")
                val cover = dataObj.optString("cover", "")
                var play = dataObj.optString("play", "")
                if (play.isNotEmpty() && !play.startsWith("http")) {
                    play = "https://www.tikwm.com$play"
                }

                val authorObj = dataObj.optJSONObject("author")
                val authorName = authorObj?.optString("nickname") ?: authorObj?.optString("unique_id") ?: "Unknown Creator"

                if (play.isEmpty()) {
                    return@withContext Result.failure(Exception("No watermark-free link found."))
                }

                Result.success(
                    TTVideoInfo(
                        id = id,
                        title = title,
                        coverUrl = cover,
                        videoUrl = play,
                        authorName = authorName
                    )
                )
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
