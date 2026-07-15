package com.adrees.ttdownloader.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

data class DownloadedVideo(
    val id: String,
    val title: String,
    val author: String,
    val fileName: String,
    val coverUrl: String,
    val timestamp: Long
)

object DownloadHelper {
    private const val PREFS_NAME = "tt_downloader_prefs"
    private const val KEY_HISTORY = "download_history"

    fun downloadVideo(
        context: Context,
        id: String,
        videoUrl: String,
        title: String,
        author: String,
        coverUrl: String
    ): Long {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(videoUrl)
            
            // Format filename safely
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_").take(30)
            val fileName = "TT_${cleanTitle}_${System.currentTimeMillis()}.mp4"

            val request = DownloadManager.Request(uri).apply {
                setTitle("Downloading TT Video")
                setDescription(title.take(60))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)
            
            // Save to history
            saveToHistory(
                context,
                DownloadedVideo(
                    id = id,
                    title = title,
                    author = author,
                    fileName = fileName,
                    coverUrl = coverUrl,
                    timestamp = System.currentTimeMillis()
                )
            )

            Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
            return downloadId
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
            return -1L
        }
    }

    fun getHistory(context: Context): List<DownloadedVideo> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyStr = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<DownloadedVideo>()
        try {
            val jsonArray = JSONArray(historyStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    DownloadedVideo(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        author = obj.getString("author"),
                        fileName = obj.getString("fileName"),
                        coverUrl = obj.getString("coverUrl"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    private fun saveToHistory(context: Context, item: DownloadedVideo) {
        val currentHistory = getHistory(context).toMutableList()
        // Prevent duplicates
        currentHistory.removeAll { it.id == item.id }
        currentHistory.add(0, item) // Add to top

        // Limit history to last 50 items
        val limitedHistory = currentHistory.take(50)

        val jsonArray = JSONArray()
        for (video in limitedHistory) {
            val obj = JSONObject().apply {
                put("id", video.id)
                put("title", video.title)
                put("author", video.author)
                put("fileName", video.fileName)
                put("coverUrl", video.coverUrl)
                put("timestamp", video.timestamp)
            }
            jsonArray.put(obj)
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }
    
    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun deleteHistoryItem(context: Context, id: String, deleteFile: Boolean) {
        val currentHistory = getHistory(context).toMutableList()
        val itemToDelete = currentHistory.find { it.id == id }
        if (itemToDelete != null) {
            currentHistory.remove(itemToDelete)
            if (deleteFile) {
                try {
                    val file = java.io.File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                        itemToDelete.fileName
                    )
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val jsonArray = org.json.JSONArray()
            for (video in currentHistory) {
                val obj = org.json.JSONObject().apply {
                    put("id", video.id)
                    put("title", video.title)
                    put("author", video.author)
                    put("fileName", video.fileName)
                    put("coverUrl", video.coverUrl)
                    put("timestamp", video.timestamp)
                }
                jsonArray.put(obj)
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
        }
    }
}
