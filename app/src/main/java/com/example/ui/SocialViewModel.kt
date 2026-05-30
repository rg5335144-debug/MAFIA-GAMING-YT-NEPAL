package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.CommentEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.PostEntity
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UserProfile(
    val name: String = "Alex Miller",
    val username: String = "alexm_creative",
    val avatar: String = "⚡",
    val bio: String = "Design engineer & short-form creator. Building high-fidelity mobile systems and exploring immersive visuals. 🚀",
    val followersCount: Int = 1250,
    val followingCount: Int = 482,
    val viewsCount: Int = 84200
)

data class Story(
    val id: Int,
    val userName: String,
    val userAvatar: String,
    val mediaUrl: String,
    val isUnread: Boolean = true,
    val caption: String = ""
)

class SocialViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = SocialRepository(database.socialDao())

    // Profile States
    private val _profileState = MutableStateFlow(UserProfile())
    val profileState: StateFlow<UserProfile> = _profileState.asStateFlow()

    // Active Tab Navigation
    private val _currentTab = MutableStateFlow(0) // 0: Reels (TikTok), 1: Feed (Facebook), 2: Create, 3: Notifications, 4: Profile
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Flow for Standard Facebook-style Posts
    val feedPosts: StateFlow<List<PostEntity>> = repository.getPostsByType("POST")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Flow for TikTok-style Reels
    val reelPosts: StateFlow<List<PostEntity>> = repository.getPostsByType("REEL")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Flow for Notifications
    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Stories (dynamic & interactive)
    private val _stories = MutableStateFlow(
        listOf(
            Story(1, "Aiko Tanaka", "🌸", "https://images.unsplash.com/photo-1540959733332-eab4deceeaf7?auto=format&fit=crop&w=600&q=80", true, "Good morning Tokyo! 🚦"),
            Story(2, "Alex Carter", "⛰️", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=600&q=80", true, "First ray of sunrise! 🏔️"),
            Story(3, "Crafty Chloe", "🎨", "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?auto=format&fit=crop&w=600&q=80", true, "Fresh clay batches done! 🏺"),
            Story(4, "Devon Lin", "💻", "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=600&q=80", true, "Compiling at 4 AM... 😴")
        )
    )
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    fun updateTab(index: Int) {
        _currentTab.value = index
    }

    fun updateProfile(name: String, username: String, bio: String, avatar: String) {
        _profileState.update {
            it.copy(name = name, username = username, bio = bio, avatar = avatar)
        }
    }

    fun toggleLike(postId: Int) {
        viewModelScope.launch {
            repository.toggleLike(postId)
        }
    }

    fun addComment(postId: Int, content: String) {
        if (content.isBlank()) return
        val profile = _profileState.value
        viewModelScope.launch {
            repository.addComment(
                postId = postId,
                content = content,
                authorName = profile.name,
                authorAvatar = profile.avatar
            )
        }
    }

    fun getCommentsForPost(postId: Int): Flow<List<CommentEntity>> {
        return repository.getCommentsForPost(postId)
    }

    fun createPost(content: String, mediaUrl: String, mediaType: String, hashtags: String) {
        val profile = _profileState.value
        val formattedHashtags = hashtags.trim().split(" ")
            .filter { it.startsWith("#") || it.isNotEmpty() }
            .joinToString(" ") { if (it.startsWith("#")) it else "#$it" }

        val imageSource = if (mediaUrl.isBlank()) {
            // Pick a high-quality aesthetic placeholder
            if (mediaType == "REEL") {
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1200&q=80"
            } else {
                "https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&w=1200&q=80"
            }
        } else {
            mediaUrl
        }

        viewModelScope.launch {
            val newPost = PostEntity(
                authorName = profile.name,
                authorUsername = profile.username,
                authorAvatar = profile.avatar,
                content = content,
                mediaUrl = imageSource,
                mediaType = mediaType,
                likesCount = 0,
                commentsCount = 0,
                sharesCount = 0,
                hashtags = formattedHashtags
            )
            repository.insertPost(newPost)
        }
    }

    fun markStoryAsRead(storyId: Int) {
        _stories.update { list ->
            list.map {
                if (it.id == storyId) it.copy(isUnread = false) else it
            }
        }
    }

    fun deleteNotification(id: Int) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }
}
