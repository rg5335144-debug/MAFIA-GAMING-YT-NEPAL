package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.SocialDao
import com.example.data.model.CommentEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.PostEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [PostEntity::class, CommentEntity::class, NotificationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun socialDao(): SocialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vibeshare_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.socialDao())
                }
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.socialDao())
                }
            }
        }

        private suspend fun populateDatabase(dao: SocialDao) {
            // Pre-seed some TikTok style Reels
            val reel1 = PostEntity(
                id = 1,
                authorName = "Aiko Tanaka",
                authorUsername = "aikotravels",
                authorAvatar = "🌸",
                content = "Midnight rain in Tokyo. Absolute cyberpunk aesthetic 🌌🚦 #tokyo #citylights #cyberpunk",
                mediaUrl = "https://images.unsplash.com/photo-1540959733332-eab4deceeaf7?auto=format&fit=crop&w=1200&q=80", // Tokyo Neon
                mediaType = "REEL",
                likesCount = 1432,
                isLiked = true,
                commentsCount = 4,
                sharesCount = 289,
                hashtags = "#tokyo #citylights #cyberpunk"
            )
            val reel2 = PostEntity(
                id = 2,
                authorName = "Alex Carter",
                authorUsername = "alex_mountain",
                authorAvatar = "⛰️",
                content = "Chasing the sun above the clouds in the Alps 🏔️☀️ Worth the 4 AM wake up call! #wanderlust #alps #adventure",
                mediaUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=80", // Mountains
                mediaType = "REEL",
                likesCount = 824,
                isLiked = false,
                commentsCount = 2,
                sharesCount = 98,
                hashtags = "#wanderlust #alps #adventure"
            )
            val reel3 = PostEntity(
                id = 3,
                authorName = "Crafty Chloe",
                authorUsername = "craft_corners",
                authorAvatar = "🎨",
                content = "Wait for the end... Extremely satisfying clay carving process 🧪✨ #satisfying #crafts #pottery",
                mediaUrl = "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?auto=format&fit=crop&w=1200&q=80", // Pottery
                mediaType = "REEL",
                likesCount = 3891,
                isLiked = false,
                commentsCount = 3,
                sharesCount = 1045,
                hashtags = "#satisfying #crafts #pottery"
            )
            val reel4 = PostEntity(
                id = 4,
                authorName = "Marcus Groove",
                authorUsername = "marcus_dancer",
                authorAvatar = "🕺",
                content = "Lunch break urban dance challenge! Rate the sync in the comments! 💃🔥 #dance #streetview #fyp",
                mediaUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=1200&q=80", // Dance/Urban
                mediaType = "REEL",
                likesCount = 512,
                isLiked = false,
                commentsCount = 2,
                sharesCount = 43,
                hashtags = "#dance #streetview #fyp"
            )

            // Pre-seed some Facebook style posts
            val post1 = PostEntity(
                id = 5,
                authorName = "Sophia Martinez",
                authorUsername = "sophia_m",
                authorAvatar = "📷",
                content = "Finally visited Kyoto! The historic streets and autumn colors are absolutely out of this world. Snapped this during sunset at Yasaka Pagoda 🍁⛩️ Deeply inspired by Japanese hospitality and history.",
                mediaUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&w=1200&q=80", // Kyoto pagoda
                mediaType = "POST",
                likesCount = 423,
                isLiked = false,
                commentsCount = 3,
                sharesCount = 12,
                hashtags = "#travel #kyoto #explore"
            )
            val post2 = PostEntity(
                id = 6,
                authorName = "Devon Lin",
                authorUsername = "dev_lin",
                authorAvatar = "💻",
                content = "Just finalized the new design engine for our Android build in Jetpack Compose! Incorporating strict Material 3 design directives, custom canvas rendering, and reactive state updates. Swipe left to see the performance logs. Android is getting fun again! 🚀⚡",
                mediaUrl = "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=1200&q=80", // Code
                mediaType = "POST",
                likesCount = 189,
                isLiked = true,
                commentsCount = 4,
                sharesCount = 5,
                hashtags = "#android #jetpackcompose #devlife"
            )
            val post3 = PostEntity(
                id = 7,
                authorName = "Emma Cozy",
                authorUsername = "emma_cozy",
                authorAvatar = "☕",
                content = "Rainy afternoon, a fresh cup of hazelnut macchiato, and a good collection of vintage essays. Perfect weekend formulation ☕🌧️ What's everyone else reading today?",
                mediaUrl = "https://images.unsplash.com/photo-1511216113906-8f57bb83e776?auto=format&fit=crop&w=1200&q=80", // Coffee book
                mediaType = "POST",
                likesCount = 312,
                isLiked = false,
                commentsCount = 2,
                sharesCount = 3,
                hashtags = "#cozy #readings #coffeetime"
            )

            // Insert posts
            dao.insertPost(reel1)
            dao.insertPost(reel2)
            dao.insertPost(reel3)
            dao.insertPost(reel4)
            dao.insertPost(post1)
            dao.insertPost(post2)
            dao.insertPost(post3)

            // Pre-seed some comments
            dao.insertComment(CommentEntity(postId = 1, authorName = "Kenji", authorAvatar = "🍜", content = "Reminds me of my evening strolls in Shinjuku! Stunning!"))
            dao.insertComment(CommentEntity(postId = 1, authorName = "Sarah W.", authorAvatar = "✈️", content = "Wow, is this filmed near Shibuya Crossing? Beautiful frame."))
            dao.insertComment(CommentEntity(postId = 1, authorName = "Nox", authorAvatar = "🕶️", content = "Unreal. Looks like straight out of Blade Runner."))
            dao.insertComment(CommentEntity(postId = 1, authorName = "VibeMaster", authorAvatar = "🎧", content = "What track did you use for this? Fits perfectly."))

            dao.insertComment(CommentEntity(postId = 2, authorName = "Emily Stokes", authorAvatar = "🥾", content = "Which peak is this? The view is absolutely insane!"))
            dao.insertComment(CommentEntity(postId = 2, authorName = "ClimberDave", authorAvatar = "🧗", content = "Ah, the crisp mountain air. Kudos to making it up by 4 AM!"))

            dao.insertComment(CommentEntity(postId = 3, authorName = "ClayGirl", authorAvatar = "🏺", content = "Oh my god, the satisfaction is unbelievable. Keep uploading these!"))
            dao.insertComment(CommentEntity(postId = 3, authorName = "Bob Potter", authorAvatar = "🧔", content = "Fabulous clay control constraint, very clean circles."))
            dao.insertComment(CommentEntity(postId = 3, authorName = "MeditationDesk", authorAvatar = "🕯️", content = "Extremely relaxing loop! Perfect ASMR."))

            dao.insertComment(CommentEntity(postId = 5, authorName = "Danielle", authorAvatar = "🍁", content = "Kyoto during autumn is just magical. Adding Yasaka to my list!"))
            dao.insertComment(CommentEntity(postId = 5, authorName = "George K.", authorAvatar = "🎒", content = "Great photography, Sophia! The red maples are gorgeous."))
            dao.insertComment(CommentEntity(postId = 5, authorName = "Yuki", authorAvatar = "🍡", content = "Glad you enjoyed Kyoto! Hope you tried the yatsuhashi sweets."))

            dao.insertComment(CommentEntity(postId = 6, authorName = "KotlinCoder", authorAvatar = "👾", content = "Agreed! Jetpack Compose makes declarative layout writing exceptionally fast."))
            dao.insertComment(CommentEntity(postId = 6, authorName = "Alice Dev", authorAvatar = "👩‍💻", content = "Are you open sourcing this renderer? Would love to study it!"))
            dao.insertComment(CommentEntity(postId = 6, authorName = "LintSlayer", authorAvatar = "⚔️", content = "Very clean performance graph metrics! Nice job Devon."))
            dao.insertComment(CommentEntity(postId = 6, authorName = "ProjectLead", authorAvatar = "📁", content = "Beautiful work Damon, let's demo this on our Monday sprint!"))

            // Pre-seed some Notifications
            dao.insertNotification(NotificationEntity(type = "LIKE", senderName = "Aiko Tanaka", senderAvatar = "🌸", targetDetail = "your latest status update"))
            dao.insertNotification(NotificationEntity(type = "COMMENT", senderName = "Devon Lin", senderAvatar = "💻", targetDetail = "commented on your code blog: 'Awesome approach!'"))
            dao.insertNotification(NotificationEntity(type = "FOLLOW", senderName = "Emma Cozy", senderAvatar = "☕", targetDetail = "started following you"))
            dao.insertNotification(NotificationEntity(type = "SHARE", senderName = "Marcus Groove", senderAvatar = "🕺", targetDetail = "shared your mountain climb reel"))
        }
    }
}
