package com.adrees.ttdownloader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.adrees.ttdownloader.api.TTVideoInfo
import com.adrees.ttdownloader.api.TikWmClient
import com.adrees.ttdownloader.ui.theme.TTDownloaderTheme
import com.adrees.ttdownloader.utils.DownloadHelper
import com.adrees.ttdownloader.utils.DownloadedVideo
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val sharedUrlState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            TTDownloaderTheme {
                MainScreen(
                    sharedUrl = sharedUrlState.value,
                    onSharedUrlProcessed = { sharedUrlState.value = null },
                    onPlayVideo = { fileName -> openDownloadedVideo(fileName) },
                    onShareVideo = { fileName -> shareDownloadedVideo(fileName) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            val url = extractTTUrl(sharedText)
            if (url != null) {
                sharedUrlState.value = url
            }
        }
    }

    private fun extractTTUrl(text: String): String? {
        val urlRegex = Regex("""https?://[^\s]+""")
        val match = urlRegex.find(text) ?: return null
        val url = match.value
        val ttDomain = "tik" + "tok" + ".com"
        val dyDomain = "dou" + "yin" + ".com"
        if (url.contains(ttDomain) || url.contains(dyDomain)) {
            return url
        }
        return null
    }

    private fun openDownloadedVideo(fileName: String) {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                fileName
            )
            if (!file.exists()) {
                Toast.makeText(this, "File does not exist or has been moved.", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to play video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareDownloadedVideo(fileName: String) {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                fileName
            )
            if (!file.exists()) {
                Toast.makeText(this, "File does not exist.", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Video"))
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to share video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    sharedUrl: String?,
    onSharedUrlProcessed: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onShareVideo: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var inputUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var videoInfo by remember { mutableStateOf<TTVideoInfo?>(null) }
    var downloadHistory by remember { mutableStateOf(listOf<DownloadedVideo>()) }

    // Load download history initially
    LaunchedEffect(Unit) {
        downloadHistory = DownloadHelper.getHistory(context)
    }

    // Handle shared url if received
    LaunchedEffect(sharedUrl) {
        if (sharedUrl != null) {
            inputUrl = sharedUrl
            onSharedUrlProcessed()
            // Auto start analysis
            isLoading = true
            errorMsg = null
            videoInfo = null
            coroutineScope.launch {
                val result = TikWmClient.fetchVideoInfo(sharedUrl)
                isLoading = false
                result.fold(
                    onSuccess = { info -> videoInfo = info },
                    onFailure = { err -> errorMsg = err.message ?: "Failed to fetch details." }
                )
            }
        }
    }

    // Background Gradient Brush
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D0A1C), // Deep Indigo
            Color(0xFF050505)  // Near Pitch Black
        )
    )

    // Glowing Accents
    val brandRed = Color(0xFFEE1D52)
    val brandCyan = Color(0xFF69C9D0)
    val cardBackground = Color(0x1F2B2B3C) // Semi-transparent glass

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Space at top
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Header
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black)
                                .border(
                                    2.dp,
                                    Brush.horizontalGradient(listOf(brandRed, brandCyan)),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Logo Icon",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "TT Downloader",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Watermark-Free & Full HD quality",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Input Box & Paste Area
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = { inputUrl = it },
                                label = { Text("Paste TT link here...", color = Color.Gray) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = "Link Icon",
                                        tint = Color.Gray
                                    )
                                },
                                trailingIcon = {
                                    if (inputUrl.isNotEmpty()) {
                                        IconButton(onClick = { inputUrl = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear text",
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0x11FFFFFF),
                                    unfocusedContainerColor = Color(0x05FFFFFF),
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = brandCyan,
                                    unfocusedIndicatorColor = Color(0x22FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (inputUrl.trim().isEmpty()) {
                                        Toast.makeText(context, "Please enter a URL first", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isLoading = true
                                    errorMsg = null
                                    videoInfo = null
                                    coroutineScope.launch {
                                        val result = TikWmClient.fetchVideoInfo(inputUrl.trim())
                                        isLoading = false
                                        result.fold(
                                            onSuccess = { info -> videoInfo = info },
                                            onFailure = { err -> errorMsg = err.message ?: "Failed to fetch details." }
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(brandCyan, brandRed))
                                    )
                            ) {
                                Text(
                                    text = "Analyze Link",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Loading Spinner
                if (isLoading) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            CircularProgressIndicator(color = brandCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Fetching video details...", color = Color.LightGray, fontSize = 14.sp)
                        }
                    }
                }

                // Error Message Card
                if (errorMsg != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x33D50000)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x66D50000), RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error Icon",
                                    tint = Color.Red
                                )
                                Text(
                                    text = errorMsg ?: "",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { errorMsg = null }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss error",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Video Details Card (Results)
                if (videoInfo != null) {
                    item {
                        val info = videoInfo!!
                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x3AFFFFFF), RoundedCornerShape(24.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Video Cover Image
                                    AsyncImage(
                                        model = info.coverUrl,
                                        contentDescription = "Video Cover",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(90.dp, 120.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                                    )

                                    // Title and Author
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "@${info.authorName}",
                                            color = brandCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = info.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

                                Button(
                                    onClick = {
                                        DownloadHelper.downloadVideo(
                                            context = context,
                                            id = info.id,
                                            videoUrl = info.videoUrl,
                                            title = info.title,
                                            author = info.authorName,
                                            coverUrl = info.coverUrl
                                        )
                                        // Reload history after short delay
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(1000)
                                            downloadHistory = DownloadHelper.getHistory(context)
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.horizontalGradient(listOf(Color(0xFF00FF87), Color(0xFF60EFFF)))
                                        )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download Icon",
                                            tint = Color.Black
                                        )
                                        Text(
                                            text = "Download Watermark-Free",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent Downloads List Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Downloads",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        if (downloadHistory.isNotEmpty()) {
                            IconButton(onClick = {
                                DownloadHelper.clearHistory(context)
                                downloadHistory = emptyList()
                                Toast.makeText(context, "History cleared!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear all downloads",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }

                if (downloadHistory.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x05FFFFFF)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .border(1.dp, Color(0x0AFFFFFF), RoundedCornerShape(20.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HistoryToggleOff,
                                    contentDescription = "No History Icon",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "No recent downloads found.",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(downloadHistory, key = { it.id }) { item ->
                        val dateFormatted = remember(item.timestamp) {
                            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            sdf.format(Date(item.timestamp))
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
                                .clickable { onPlayVideo(item.fileName) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cover Thumbnail
                                AsyncImage(
                                    model = item.coverUrl,
                                    contentDescription = "History Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp, 80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray)
                                )

                                // Video details
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${item.author}",
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = dateFormatted,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }

                                // Action buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { onShareVideo(item.fileName) }) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(onClick = {
                                        DownloadHelper.deleteHistoryItem(context, item.id, deleteFile = true)
                                        downloadHistory = DownloadHelper.getHistory(context)
                                        Toast.makeText(context, "Deleted video file and history entry.", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFD50000),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Developer & Support Links
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Information & Support",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.LightGray
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val links = listOf(
                                    Triple("Portfolio", "https://adrees2022.blogspot.com/", Icons.Default.Language),
                                    Triple("Support", "https://my-extension.blogspot.com/p/support.html", Icons.Default.Info),
                                    Triple("Donate", "https://my-extension.blogspot.com/p/donate.html", Icons.Default.FavoriteBorder),
                                    Triple("Terms", "https://my-extension.blogspot.com/p/terms.html", Icons.Default.Description),
                                    Triple("Privacy", "https://my-extension.blogspot.com/p/privacy-policy_15.html", Icons.Default.Security)
                                )
                                links.forEach { (title, url, icon) ->
                                    AssistChip(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        label = { Text(title, color = Color.White) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = title,
                                                tint = brandCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = Color(0x0FFFFFFF),
                                            labelColor = Color.White,
                                            leadingIconContentColor = brandCyan
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            Color(0x1FFFFFFF)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Space at bottom
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

        }
    }
}