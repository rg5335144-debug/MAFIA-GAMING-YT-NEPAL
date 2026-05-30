package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorName: String,
    val authorUsername: String,
    val authorAvatar: String, // Initial or Emoji/image URL
    val content: String,
    val mediaUrl: String, // Simulated GIF url/mock image or custom canvas tag
    val mediaType: String, // "POST" (Facebook style) or "REEL" (TikTok style)
    val likesCount: Int,
    val isLiked: Boolean = false,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val hashtags: String = ""
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val authorName: String,
    val authorAvatar: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "LIKE", "COMMENT", "FOLLOW", "SHARE"
    val senderName: String,
    val senderAvatar: String,
    val targetDetail: String, // e.g. "your reel 'Night city vibes...'"
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
