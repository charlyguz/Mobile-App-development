package com.example.api_practice

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.api_practice.ui.theme.APIpracticeTheme
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class Post(val id: Int, val title: String, val body: String)
data class Comment(val id: Int, val name: String, val email: String, val body: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            APIpracticeTheme {
                ApiPracticeApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiPracticeApp() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Explorer", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        ApiPracticeScreen(modifier = Modifier.padding(innerPadding))
    }
}

@Composable
fun ApiPracticeScreen(modifier: Modifier = Modifier) {
    var singlePostText by remember { mutableStateOf<String?>(null) }
    var singlePostLoading by remember { mutableStateOf(false) }
    
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var postsLoading by remember { mutableStateOf(false) }
    
    var expandedPostId by remember { mutableStateOf<Int?>(null) }
    var commentsMap by remember { mutableStateOf<Map<Int, List<Comment>>>(emptyMap()) }
    var loadingCommentsFor by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Basic Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                Log.d("LAB11", "Button clicked")
                                Thread {
                                    val client = OkHttpClient()
                                    val request = Request.Builder().url("https://jsonplaceholder.typicode.com/posts/1").build()
                                    try {
                                        val response = client.newCall(request).execute()
                                        val result = response.body?.string()
                                        Log.d("LAB11", result.toString())
                                    } catch (e: Exception) {
                                        Log.d("LAB11", e.message.toString())
                                    }
                                }.start()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load data")
                        }
                        
                        Button(
                            onClick = {
                                singlePostLoading = true
                                singlePostText = null
                                Thread {
                                    val client = OkHttpClient()
                                    val request = Request.Builder().url("https://jsonplaceholder.typicode.com/posts/1").build()
                                    try {
                                        val response = client.newCall(request).execute()
                                        val result = response.body?.string() ?: "{}"
                                        val json = JSONObject(result)
                                        val title = json.getString("title")
                                        val body = json.getString("body")
                                        
                                        singlePostText = "Title: $title\nBody: $body"
                                    } catch (e: Exception) {
                                        singlePostText = e.message.toString()
                                    } finally {
                                        singlePostLoading = false
                                    }
                                }.start()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load data to UI")
                        }
                    }
                    
                    if (singlePostLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else if (singlePostText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = singlePostText!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Load Multiple Posts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Fetches posts and displays them as interactive cards.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                Log.d("LAB11", "Third Button clicked")
                                Thread {
                                    val client = OkHttpClient()
                                    val request = Request.Builder().url("https://jsonplaceholder.typicode.com/posts").build()
                                    try {
                                        val response = client.newCall(request).execute()
                                        val result = response.body?.string() ?: "[]"
                                        Log.d("LAB11", result.toString())
                                    } catch (e: Exception) {
                                        Log.d("LAB11", e.message.toString())
                                    }
                                }.start()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load many posts")
                        }
                        
                        Button(
                            onClick = {
                                postsLoading = true
                                Thread {
                                    val client = OkHttpClient()
                                    val request = Request.Builder().url("https://jsonplaceholder.typicode.com/posts").build()
                                    try {
                                        val response = client.newCall(request).execute()
                                        val result = response.body?.string() ?: "[]"
                                        val jsonArray = JSONArray(result)
                                        val loadedPosts = mutableListOf<Post>()
                                        for (i in 0 until jsonArray.length()) {
                                            val item = jsonArray.getJSONObject(i)
                                            loadedPosts.add(
                                                Post(
                                                    id = item.getInt("id"),
                                                    title = item.getString("title"),
                                                    body = item.getString("body")
                                                )
                                            )
                                        }
                                        posts = loadedPosts
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        postsLoading = false
                                    }
                                }.start()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load many posts to UI")
                        }
                    }
                }
            }
        }
        
        if (postsLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (posts.isNotEmpty()) {
            item {
                Text(
                    text = "Loaded Posts (${posts.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
        }
        
        items(posts) { post ->
            val isExpanded = expandedPostId == post.id
            val isCommentsLoading = loadingCommentsFor.contains(post.id)
            val postComments = commentsMap[post.id]
            
            val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")
            
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (isExpanded) {
                            expandedPostId = null
                        } else {
                            expandedPostId = post.id
                            if (!commentsMap.containsKey(post.id)) {
                                loadingCommentsFor = loadingCommentsFor + post.id
                                Thread {
                                    val client = OkHttpClient()
                                    val request = Request.Builder()
                                        .url("https://jsonplaceholder.typicode.com/posts/${post.id}/comments")
                                        .build()
                                    try {
                                        val response = client.newCall(request).execute()
                                        val result = response.body?.string() ?: "[]"
                                        val jsonArray = JSONArray(result)
                                        val loadedComments = mutableListOf<Comment>()
                                        for (i in 0 until jsonArray.length()) {
                                            val item = jsonArray.getJSONObject(i)
                                            loadedComments.add(
                                                Comment(
                                                    id = item.getInt("id"),
                                                    name = item.getString("name"),
                                                    email = item.getString("email"),
                                                    body = item.getString("body")
                                                )
                                            )
                                        }
                                        val newMap = commentsMap.toMutableMap()
                                        newMap[post.id] = loadedComments
                                        commentsMap = newMap
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        loadingCommentsFor = loadingCommentsFor - post.id
                                    }
                                }.start()
                            }
                        }
                    },
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${post.id}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = post.title.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = post.body.replace("\n", " "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isExpanded) "Hide Comments" else "View Comments",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.rotate(rotationState)
                        )
                    }
                    
                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            if (isCommentsLoading) {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            } else if (postComments != null) {
                                postComments.forEach { comment ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = "User",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = comment.email.lowercase(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = comment.name.replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = comment.body,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}