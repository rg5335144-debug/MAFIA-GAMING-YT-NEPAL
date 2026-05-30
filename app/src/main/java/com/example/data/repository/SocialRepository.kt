package com.example.data.repository

import com.example.data.dao.SocialDao
import com.example.data.model.CommentEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.PostEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class SocialRepository(private val socialDao: SocialDao) {

    val allPosts: Flow<List<PostEntity>> = socialDao.getAllPosts()

    fun getPostsByType(type: String): Flow<List<PostEntity>> {
        return socialDao.getPostsByType(type)
    }

    fun getCommentsForPost(postId: Int): Flow<List<CommentEntity>> {
        return socialDao.getCommentsForPost(postId)
    }

    val notifications: Flow<List<NotificationEntity>> = socialDao.getNotifications()

    suspend fun insertPost(post: PostEntity): Long {
        return socialDao.insertPost(post)
    }

    suspend fun toggleLike(postId: Int) {
        val post = socialDao.getPostById(postId) ?: return
        val isLikedNew = !post.isLiked
        val likesDiff = if (isLikedNew) 1 else -1
        val updatedPost = post.copy(
            isLiked = isLikedNew,
            likesCount = (post.likesCount + likesDiff).coerceAtLeast(0)
        )
        socialDao.updatePost(updatedPost)

        if (isLikedNew) {
            // Simulated real-time interaction feedback
            // Generate a notification from the post author to the active user saying "Thanks for liking!" 
            // Or a notification log update
            val senderAvatar = if (post.authorAvatar.isNotEmpty()) post.authorAvatar else "👤"
            socialDao.insertNotification(
                NotificationEntity(
                    type = "LIKE",
                    senderName = post.authorName,
                    senderAvatar = senderAvatar,
                    targetDetail = "liked your engagement with their post: '${post.content.take(20)}...'"
                )
            )
        }
    }

    suspend fun addComment(postId: Int, content: String, authorName: String, authorAvatar: String) {
        val post = socialDao.getPostById(postId) ?: return
        
        // Save comment
        val comment = CommentEntity(
            postId = postId,
            authorName = authorName,
            authorAvatar = authorAvatar,
            content = content
        )
        socialDao.insertComment(comment)

        // Update post comment count
        val updatedPost = post.copy(
            commentsCount = post.commentsCount + 1
        )
        socialDao.updatePost(updatedPost)

        // Generate a simulated responsive notification!
        // When user leaves a comment, simulated author responds or likes the comment a few seconds later, 
        // which we can trigger in the ViewModel, but here we can add a notification as an instantaneous feedback!
        socialDao.insertNotification(
            NotificationEntity(
                type = "COMMENT",
                senderName = post.authorName,
                senderAvatar = post.authorAvatar,
                targetDetail = "replied to your comment on their post: 'Love that! Thank you!'"
            )
        )
    }

    suspend fun markNotificationAsRead(id: Int) {
        socialDao.markNotificationAsRead(id)
    }

    suspend fun deleteNotification(id: Int) {
        socialDao.deleteNotification(id)
    }

    suspend fun clearNotifications() {
        socialDao.clearAllNotifications()
    }
}
