package com.example.data.dao

import androidx.room.*
import com.example.data.model.PostEntity
import com.example.data.model.CommentEntity
import com.example.data.model.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialDao {

    @Query("SELECT * FROM posts WHERE mediaType = :mediaType ORDER BY createdAt DESC")
    fun getPostsByType(mediaType: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id")
    fun getPostByIdFlow(id: Int): Flow<PostEntity?>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostById(id: Int): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity): Long

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun getCommentsForPost(postId: Int): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}
