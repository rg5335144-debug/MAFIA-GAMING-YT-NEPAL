@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.CommentEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.PostEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialApp(
    viewModel: SocialViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Story viewer dialog state
    var selectedStoryIndex by remember { mutableStateOf<Int?>(null) }
    val storiesList by viewModel.stories.collectAsStateWithLifecycle()

    // Active bottom sheet comment states
    var commentPostId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_navigation_bar"),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.updateTab(0) },
                    icon = { Icon(if (currentTab == 0) Icons.Filled.Movie else Icons.Outlined.Movie, contentDescription = "Reels") },
                    label = { Text("Reels") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.updateTab(1) },
                    icon = { Icon(if (currentTab == 1) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.updateTab(2) },
                    icon = { Icon(if (currentTab == 2) Icons.Filled.AddCircle else Icons.Outlined.AddCircle, contentDescription = "Create") },
                    label = { Text("Create") }
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.updateTab(3) },
                    icon = {
                        val notes by viewModel.notifications.collectAsStateWithLifecycle()
                        val unreadCount = notes.count { !it.isRead }
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(if (currentTab == 3) Icons.Filled.Notifications else Icons.Outlined.Notifications, contentDescription = "Notifications")
                        }
                    },
                    label = { Text("Alerts") }
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { viewModel.updateTab(4) },
                    icon = {
                        val profile by viewModel.profileState.collectAsStateWithLifecycle()
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(profile.avatar, fontSize = 12.sp)
                        }
                    },
                    label = { Text("Profile") }
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> ReelsScreen(
                    viewModel = viewModel,
                    onOpenComments = { postId -> commentPostId = postId }
                )
                1 -> FeedScreen(
                    viewModel = viewModel,
                    onOpenStory = { index -> selectedStoryIndex = index },
                    onOpenComments = { postId -> commentPostId = postId }
                )
                2 -> CreateScreen(
                    viewModel = viewModel
                )
                3 -> NotificationScreen(
                    viewModel = viewModel
                )
                4 -> ProfileScreen(
                    viewModel = viewModel
                )
            }

            // High-fidelity full-screen Story Viewer
            selectedStoryIndex?.let { startIndex ->
                StoryViewerDialog(
                    stories = storiesList,
                    startIndex = startIndex,
                    onDismiss = { selectedStoryIndex = null },
                    onStoryWatched = { storyId -> viewModel.markStoryAsRead(storyId) }
                )
            }

            // Bottom Comments Sheet Drawer
            commentPostId?.let { postId ->
                CommentsDrawer(
                    postId = postId,
                    viewModel = viewModel,
                    onDismiss = { commentPostId = null }
                )
            }
        }
    }
}

// ==========================================
// TIKTOK STYLE REELS SCREEN
// ==========================================
@Composable
fun ReelsScreen(
    viewModel: SocialViewModel,
    onOpenComments: (Int) -> Unit
) {
    val reels by viewModel.reelPosts.collectAsStateWithLifecycle()

    if (reels.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { reels.size })

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ReelItem(
                post = reels[page],
                onToggleLike = { viewModel.toggleLike(reels[page].id) },
                onOpenComments = { onOpenComments(reels[page].id) },
                onShare = {
                    // Quick share simulated Toast feedback
                    viewModel.toggleLike(reels[page].id) // Increments interactive state too!
                }
            )
        }

        // Overlay brand caption
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reels Live ⚡",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.testTag("reels_title")
            )
            IconButton(
                onClick = {},
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Search Reels")
            }
        }
    }
}

@Composable
fun ReelItem(
    post: PostEntity,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(0.35f) }
    var heartSpawned by remember { mutableStateOf(false) }

    // Floating double tap gesture animation
    val scaleHeart by animateFloatAsState(
        targetValue = if (heartSpawned) 1.5f else 0.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { if (it > 1.0f) heartSpawned = false }
    )

    // Animated seekbar progression
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(60)
                currentProgress = (currentProgress + 0.0025f) % 1.0f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
            .clickable(
                interactionSource = remember { CustomClickableInteractionSource() },
                indication = null
            ) {
                isPlaying = !isPlaying
            }
    ) {
        // High fidelity simulated video backdrop (Using dynamic images loaded with Coil)
        AsyncImage(
            model = post.mediaUrl,
            contentDescription = "Reel Backdrop",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Backdrop gradient to ensure text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.75f)
                        ),
                        startY = 500f
                    )
                )
        )

        // Waveform/Equalizer dynamic simulation at the right bottom to feel lively
        if (isPlaying) {
            AudioEqualizerBars(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 85.dp, bottom = 45.dp)
            )
        }

        // Overlay HUD controls & comments
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Text Captions Block
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(post.authorAvatar, fontSize = 16.sp)
                    }
                    Column {
                        Text(
                            post.authorName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "@${post.authorUsername}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

                    // Simulated follow indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable {
                                Toast.makeText(context, "Following @${post.authorUsername}", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Follow", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = post.content,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Hashtags accented tags
                if (post.hashtags.isNotEmpty()) {
                    Text(
                        text = post.hashtags,
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Rotating sound clip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = "Track",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Original Sound - @${post.authorUsername}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            // Right sidebar buttons panel (TikTok characteristic overlay icons)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Like Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            if (!post.isLiked) {
                                heartSpawned = true
                            }
                            onToggleLike()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .testTag("reel_like_button")
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like Reel",
                            tint = if (post.isLiked) Color.Red else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        post.likesCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Comment Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onOpenComments,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .testTag("reel_comment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Message,
                            contentDescription = "Open Comments",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        post.commentsCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Share button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            onShare()
                            Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        post.sharesCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Spinning Vinyl record animation
                SpinningVinyl(
                    avatar = post.authorAvatar,
                    isPlaying = isPlaying
                )
            }
        }

        // Center Pause Indicator Flash Overlay
        AnimatedVisibility(
            visible = !isPlaying,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // Double tap heart pop feedback overlay
        if (scaleHeart > 0.1f) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Love pop",
                tint = Color.Red,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(100.dp)
                    .scale(scaleHeart)
            )
        }

        // Progress bar at the very bottom
        LinearProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter),
            color = Color(0xFF00E5FF),
            trackColor = Color.White.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun SpinningVinyl(
    avatar: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .size(42.dp)
            .rotate(currentRotation)
            .border(2.dp, Color.White, CircleShape)
            .background(Color.Black, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.Black, CircleShape)
                .border(6.dp, Color.DarkGray.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(avatar, fontSize = 14.sp)
        }
    }
}

@Composable
fun AudioEqualizerBars(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    @Composable
    fun animatedHeight(initial: Int, target: Int, duration: Int): State<Float> {
        return infiniteTransition.animateFloat(
            initialValue = initial.toFloat(),
            targetValue = target.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "BarHeight"
        )
    }

    val h1 by animatedHeight(8, 28, 480)
    val h2 by animatedHeight(14, 38, 550)
    val h3 by animatedHeight(6, 24, 410)
    val h4 by animatedHeight(12, 32, 600)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barColor = Color(0xFF00E5FF)
        Box(modifier = Modifier.width(3.dp).height(h1.dp).background(barColor, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(3.dp).height(h2.dp).background(barColor, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(3.dp).height(h3.dp).background(barColor, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(3.dp).height(h4.dp).background(barColor, RoundedCornerShape(1.dp)))
    }
}


// ==========================================
// FACEBOOK STYLE COMMUNITY HOME FEED SCREEN
// ==========================================
@Composable
fun FeedScreen(
    viewModel: SocialViewModel,
    onOpenStory: (Int) -> Unit,
    onOpenComments: (Int) -> Unit
) {
    val posts by viewModel.feedPosts.collectAsStateWithLifecycle()
    val storiesList by viewModel.stories.collectAsStateWithLifecycle()
    val activeProfile by viewModel.profileState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App header title & controls
        item {
            FeedHeader()
        }

        // Horizontal Story Grid Row (Fast, fun dynamic stories)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick add my story item
                    item {
                        MyStoryPlaceholderCard(profile = activeProfile) {
                            viewModel.updateTab(2) // Goes to create
                        }
                    }

                    items(storiesList.size) { index ->
                        StoryCard(
                            story = storiesList[index],
                            onClick = { onOpenStory(index) }
                        )
                    }
                }
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 6.dp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        // "What's on your mind?" visual status box builder
        item {
            CreateStatusQuickBox(
                profile = activeProfile,
                onFocusCreate = { viewModel.updateTab(2) }
            )
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 6.dp
            )
        }

        if (posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.SentimentDissatisfied,
                            contentDescription = "Empty",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Nothing in community feed yet. Post something first!",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            items(posts) { post ->
                PostItemCard(
                    post = post,
                    onToggleLike = { viewModel.toggleLike(post.id) },
                    onOpenComments = { onOpenComments(post.id) }
                )
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 6.dp
                )
            }
        }
    }
}

@Composable
fun FeedHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "vibeshare",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.5).sp
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = {},
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = {},
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Filled.Forum, contentDescription = "Messenger", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun StoryCard(
    story: Story,
    onClick: () -> Unit
) {
    val unreadColor = Color(0xFF00E5FF)
    val readColor = Color.LightGray.copy(alpha = 0.5f)

    Card(
        modifier = Modifier
            .size(100.dp, 160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = story.mediaUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Backdrop darkened tint
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                            startY = 120f
                        )
                    )
            )
            // User circular icon with custom unread border
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(36.dp)
                    .border(2.5.dp, if (story.isUnread) unreadColor else readColor, CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(story.userAvatar, fontSize = 14.sp)
            }
            // User Caption Handle
            Text(
                text = story.userName,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MyStoryPlaceholderCard(
    profile: UserProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(100.dp, 160.dp)
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.8f)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(profile.avatar, fontSize = 18.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .offset(y = (-11).dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Story", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            "Create Story",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(y = (-6).dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateStatusQuickBox(
    profile: UserProfile,
    onFocusCreate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFocusCreate() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(profile.avatar, fontSize = 18.sp)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                "What's on your mind, ${profile.name.split(" ")[0]}?",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }

        IconButton(onClick = onFocusCreate) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Add Media", tint = Color(0xFF4CAF50))
        }
    }
}

@Composable
fun PostItemCard(
    post: PostEntity,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().testTag("post_item_card_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = androidx.compose.ui.graphics.RectangleShape
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            // Header Profile row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(post.authorAvatar, fontSize = 18.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Verified Account", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        "@${post.authorUsername} • 2 hrs ago",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = {}) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                }
            }

            // Text Content block
            Text(
                text = post.content,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            // Hashtags
            if (post.hashtags.isNotEmpty()) {
                Text(
                    text = post.hashtags,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                )
            }

            // Attached Media image loaded with Coil
            if (post.mediaUrl.isNotEmpty()) {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = "Post Media Content",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color.LightGray.copy(alpha = 0.1f)),
                    contentScale = ContentScale.Crop
                )
            }

            // Social Engagement summary stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1976D2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ThumbUp, contentDescription = "Likes count", tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                    if (post.likesCount > 0) {
                        Text(
                            text = "${post.likesCount} likes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Text(
                    text = "${post.commentsCount} comments • ${post.sharesCount} shares",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Interactive Click buttons Action Panel (Like, Comment, Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button Action
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleLike() }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like Post",
                        tint = if (post.isLiked) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (post.isLiked) "Liked" else "Like",
                        fontSize = 13.sp,
                        fontWeight = if (post.isLiked) FontWeight.Bold else FontWeight.Medium,
                        color = if (post.isLiked) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Comment Button Action
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenComments() }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Comment", fontSize = 13.sp)
                }

                // Share Button Action
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            Toast.makeText(context, "Direct Share triggered!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 13.sp)
                }
            }
        }
    }
}


// ==========================================
// POST & REEL CREATOR SCREEN
// ==========================================
@Composable
fun CreateScreen(
    viewModel: SocialViewModel
) {
    var contentText by remember { mutableStateOf("") }
    var hashtagText by remember { mutableStateOf("") }
    var mediaUrlText by remember { mutableStateOf("") }
    var mediaTypeSelected by remember { mutableStateOf("POST") } // "POST" or "REEL"
    
    // Preset high fidelity backgrounds preview
    val presetImages = listOf(
        Pair("Midnight Lights", "https://images.unsplash.com/photo-1540959733332-eab4deceeaf7?auto=format&fit=crop&w=600&q=80"),
        Pair("Serene Alps", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=600&q=80"),
        Pair("Cozy Brew", "https://images.unsplash.com/photo-1511216113906-8f57bb83e776?auto=format&fit=crop&w=600&q=80"),
        Pair("Clay Pottery", "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?auto=format&fit=crop&w=600&q=80")
    )

    var shareToInstagram by remember { mutableStateOf(false) }
    var audienceLevel by remember { mutableStateOf("Public") } // "Public", "Friends", "Private"

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val activeProfile by viewModel.profileState.collectAsStateWithLifecycle()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create Social Vibe ✨", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("create_top_bar")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile teaser metadata
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(activeProfile.avatar, fontSize = 20.sp)
                }
                Column {
                    Text(activeProfile.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { audienceLevel = if (audienceLevel == "Public") "Friends" else "Public" },
                            label = { Text(audienceLevel) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (audienceLevel == "Public") Icons.Filled.Public else Icons.Filled.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // Post type selector tabs
            Column {
                Text("Select Format Type:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { mediaTypeSelected = "POST" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (mediaTypeSelected == "POST") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (mediaTypeSelected == "POST") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("select_post_tab")
                    ) {
                        Icon(Icons.Filled.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Standard Post")
                    }

                    Button(
                        onClick = { mediaTypeSelected = "REEL" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (mediaTypeSelected == "REEL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (mediaTypeSelected == "REEL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("select_reel_tab")
                    ) {
                        Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TikTok Reel")
                    }
                }
            }

            // Text status content editor
            OutlinedTextField(
                value = contentText,
                onValueChange = { contentText = it },
                label = { Text("What is on your mind? Tell the community...") },
                placeholder = { Text("Share an inspiring story or short caption...") },
                modifier = Modifier.fillMaxWidth().height(120.dp).testTag("create_caption_input"),
                supportingText = { Text("${contentText.length}/280 characters") },
                maxLines = 5
            )

            // Hashtags selector
            OutlinedTextField(
                value = hashtagText,
                onValueChange = { hashtagText = it },
                label = { Text("Hashtags (Optional)") },
                placeholder = { Text("e.g. #adventure #devlife #funny") },
                leadingIcon = { Icon(Icons.Filled.Tag, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            // Quick backgrounds picker preset
            Column {
                Text("Select Cover Media/Backdrop:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetImages) { item ->
                        val isSelected = mediaUrlText == item.second
                        Card(
                            modifier = Modifier
                                .size(110.dp, 75.dp)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { mediaUrlText = item.second },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = item.second,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f))
                                )
                                Text(
                                    text = item.first,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Custom Media URL target fallback input
            OutlinedTextField(
                value = mediaUrlText,
                onValueChange = { mediaUrlText = it },
                label = { Text("Or Paste Custom Image URL") },
                placeholder = { Text("https://example.com/item.png") },
                leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("custom_image_input")
            )

            // Dynamic Share Toggle options
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cross-post to Instagram", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Instantly mirror this post to feed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(checked = shareToInstagram, onCheckedChange = { shareToInstagram = it })
                }
            }

            // Publish CTA Button
            Button(
                onClick = {
                    if (contentText.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter some status content text first!")
                        }
                        return@Button
                    }
                    viewModel.createPost(
                        content = contentText,
                        mediaUrl = mediaUrlText,
                        mediaType = mediaTypeSelected,
                        hashtags = hashtagText
                    )
                    scope.launch {
                        Toast.makeText(viewModel.getApplication(), "Successfully Published Vibe! ✨", Toast.LENGTH_SHORT).show()
                        // Reset forms
                        contentText = ""
                        hashtagText = ""
                        mediaUrlText = ""
                        // Automatically slide user to the post's target destination feed
                        viewModel.updateTab(if (mediaTypeSelected == "REEL") 0 else 1)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_vibe_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Publish, contentDescription = "Publish")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish to Feed now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}


// ==========================================
// NOTIFICATIONS MANAGEMENT SCREEN
// ==========================================
@Composable
fun NotificationScreen(
    viewModel: SocialViewModel
) {
    val alerts by viewModel.notifications.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Activity Log 🔔", fontWeight = FontWeight.Bold) },
                actions = {
                    if (alerts.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearNotifications() }) {
                            Text("Clear All")
                        }
                    }
                },
                modifier = Modifier.testTag("notifications_top_bar")
            )
        }
    ) { innerPadding ->
        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.NotificationsNone,
                        contentDescription = "Empty Alerts",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "All caught up! No notifications yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Interactions from the community will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(alerts) { alert ->
                    NotificationItemRow(
                        alert = alert,
                        onDelete = { viewModel.deleteNotification(alert.id) }
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
fun NotificationItemRow(
    alert: NotificationEntity,
    onDelete: () -> Unit
) {
    // Determine icon colors based on notification types
    val (iconColor, iconVector) = when (alert.type) {
        "LIKE" -> Pair(Color.Red, Icons.Filled.Favorite)
        "COMMENT" -> Pair(Color(0xFF00E5FF), Icons.Filled.Message)
        "FOLLOW" -> Pair(Color(0xFF9C27B0), Icons.Filled.Person)
        else -> Pair(Color(0xFF4CAF50), Icons.Filled.Share)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_row_${alert.id}")
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatars container with overlapping indicator emblem
        Box(modifier = Modifier.size(48.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .align(Alignment.TopStart)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(alert.senderAvatar, fontSize = 20.sp)
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color.LightGray, CircleShape)
                    .align(Alignment.BottomEnd)
                    .background(iconColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        // Active notification text descriptor content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = alert.senderName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = alert.targetDetail,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "just now",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        // Removal dismiss action
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Dismiss notification",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


// ==========================================
// USER SOCIAL PROFILE SCREEN
// ==========================================
@Composable
fun ProfileScreen(
    viewModel: SocialViewModel
) {
    val activeProfile by viewModel.profileState.collectAsStateWithLifecycle()
    val posts by viewModel.feedPosts.collectAsStateWithLifecycle()
    val reels by viewModel.reelPosts.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }

    // Combined posts created by active user "Alex Miller", or filter matches
    val myPosts = posts.filter { it.authorName == activeProfile.name }
    val myReels = reels.filter { it.authorName == activeProfile.name }

    var selectedGridSubTab by remember { mutableStateOf(0) } // 0: Reels grid, 1: Posts row list

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("@${activeProfile.username}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Edit Profile Settings")
                    }
                },
                modifier = Modifier.testTag("profile_top_bar")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // User Avatar Header Box
            Column(
                modifier = Modifier
                    .fillModifier()
                    .padding(horizontal = 16.dp).padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(activeProfile.avatar, fontSize = 42.sp)
                }

                Text(activeProfile.name, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(
                    "@${activeProfile.username}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Bio Description status
                Text(
                    text = activeProfile.bio,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stats Dashboard Bar Row (Followers, Following, Views count)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            activeProfile.followingCount.toString(),
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text("Following", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            activeProfile.followersCount.toString(),
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text("Followers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val aggregatedLikes = (myPosts + myReels).sumOf { it.likesCount }
                        Text(
                            aggregatedLikes.toString(),
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text("Likes Received", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // CTA action modifiers: Edit Profile button
                Button(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.fillMaxWidth().height(40.dp).testTag("edit_profile_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Tabs toggles: Reels vs Posts
            TabRow(
                selectedTabIndex = selectedGridSubTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedGridSubTab == 0,
                    onClick = { selectedGridSubTab = 0 },
                    text = { Text("Short Reels (${myReels.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedGridSubTab == 1,
                    onClick = { selectedGridSubTab = 1 },
                    text = { Text("Standard Posts (${myPosts.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Grid Layout of matching submissions
            Box(modifier = Modifier.weight(1f)) {
                if (selectedGridSubTab == 0) {
                    // Short reels vertical cards grid
                    if (myReels.isEmpty()) {
                        ProfileSubItemsEmpty(text = "Unleash your creativity. Tap '+' to record your first reel!")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(myReels) { reel ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(0.68f)
                                        .background(Color.DarkGray)
                                        .clickable {
                                            viewModel.updateTab(0) // Goes straight to live feed scrolling
                                        }
                                ) {
                                    AsyncImage(
                                        model = reel.mediaUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Likes count indicators
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                                )
                                            )
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(12.dp))
                                        Text(reel.likesCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Standard visual square grid
                    if (myPosts.isEmpty()) {
                        ProfileSubItemsEmpty(text = "Connect with friends. Post updates, photos, or code snippets!")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(myPosts) { post ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .background(Color.LightGray.copy(alpha = 0.2f))
                                        .clickable {
                                            viewModel.updateTab(1) // Goes to general feed
                                        }
                                ) {
                                    if (post.mediaUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = post.mediaUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize().padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = post.content,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                overflow = TextOverflow.Ellipsis
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

        // Edit profile dialog modal settings
        if (showEditDialog) {
            EditProfileDialog(
                currentTheme = activeProfile,
                onDismiss = { showEditDialog = false },
                onSave = { name, username, bio, avatar ->
                    viewModel.updateProfile(name, username, bio, avatar)
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
fun ProfileSubItemsEmpty(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.FolderCopy, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            Text(
                text = text,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun EditProfileDialog(
    currentTheme: UserProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var editName by remember { mutableStateOf(currentTheme.name) }
    var editUsername by remember { mutableStateOf(currentTheme.username) }
    var editBio by remember { mutableStateOf(currentTheme.bio) }
    var editAvatar by remember { mutableStateOf(currentTheme.avatar) }

    val presetEmojis = listOf("⚡", "🌸", "🏔️", "🎨", "🕺", "📷", "💻", "☕")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Profile ⚡") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_name_input")
                )
                OutlinedTextField(
                    value = editUsername,
                    onValueChange = { editUsername = it },
                    label = { Text("Username Handle") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_username_input")
                )
                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text("Bio description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Select Emoji Avatar:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(presetEmojis) { emoji ->
                            val isSelected = editAvatar == emoji
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { editAvatar = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(editName, editUsername, editBio, editAvatar) },
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save Profile Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


// ==========================================
// DETAILED SOCIAL STORIES VIEWER OVERLAY
// ==========================================
@Composable
fun StoryViewerDialog(
    stories: List<Story>,
    startIndex: Int,
    onDismiss: () -> Unit,
    onStoryWatched: (Int) -> Unit
) {
    var activeStoryIndex by remember { mutableStateOf(startIndex) }
    var currentProgress by remember { mutableStateOf(0.0f) }

    // Dynamic auto advance timer
    LaunchedEffect(activeStoryIndex) {
        currentProgress = 0.0f
        onStoryWatched(stories[activeStoryIndex].id)
        while (currentProgress < 1.0f) {
            delay(40)
            currentProgress += 0.015f
        }
        if (activeStoryIndex < stories.size - 1) {
            activeStoryIndex++
        } else {
            onDismiss()
        }
    }

    val activeStory = stories[activeStoryIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Back media cover loaded with Coil
            AsyncImage(
                model = activeStory.mediaUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Top overlay bar with linear timer progress lines
            Column(
                modifier = Modifier
                    .fillModifier()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Story lines segments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { idx, _ ->
                        val progressValue = when {
                            idx < activeStoryIndex -> 1.0f
                            idx == activeStoryIndex -> currentProgress
                            else -> 0.0f
                        }
                        LinearProgressIndicator(
                            progress = { progressValue },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.35f)
                        )
                    }
                }

                // Story profile metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(activeStory.userAvatar, fontSize = 16.sp)
                        }
                        Column {
                            Text(activeStory.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Posted recently", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // Central overlay tapping logic triggers
            Row(modifier = Modifier.fillMaxSize()) {
                // Left third tap (Previous story)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { CustomClickableInteractionSource() },
                            indication = null
                        ) {
                            if (activeStoryIndex > 0) {
                                activeStoryIndex--
                            } else {
                                onDismiss()
                            }
                        }
                )
                // Middle third tap doesn't disturb
                Spacer(modifier = Modifier.weight(1f))
                // Right third tap (Next story)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { CustomClickableInteractionSource() },
                            indication = null
                        ) {
                            if (activeStoryIndex < stories.size - 1) {
                                activeStoryIndex++
                            } else {
                                onDismiss()
                            }
                        }
                )
            }

            // Bottom caption overlay status detail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeStory.caption,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


// ==========================================
// INTERACTIVE BOTTOM SHEET COMMENTS SLIDE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsDrawer(
    postId: Int,
    viewModel: SocialViewModel,
    onDismiss: () -> Unit
) {
    val comments by viewModel.getCommentsForPost(postId).collectAsStateWithLifecycle(initialValue = emptyList())
    var commentBodyText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("comments_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                .height(480.dp)
        ) {
            // Header title and details count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Conversations (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Sheet")
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Scrollable list comments
            Box(modifier = Modifier.weight(1f)) {
                if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(38.dp), tint = Color.LightGray)
                            Text(
                                "No replies yet. Be the first to spark the feed!",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(comments) { comment ->
                            Row(
                                modifier = Modifier.fillMaxWidth().testTag("comment_item_row_${comment.id}"),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(comment.authorAvatar, fontSize = 14.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("• active", fontSize = 11.sp, color = Color.Gray.copy(alpha = 0.7f))
                                    }
                                    Text(
                                        text = comment.content,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Sticky write comment panel at bottom standard inputs
            Row(
                modifier = Modifier
                    .fillModifier()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentBodyText,
                    onValueChange = { commentBodyText = it },
                    placeholder = { Text("Add comment from profile...") },
                    modifier = Modifier.weight(1f).testTag("comment_input_box"),
                    maxLines = 2,
                    shape = RoundedCornerShape(24.dp)
                )

                IconButton(
                    onClick = {
                        if (commentBodyText.isBlank()) return@IconButton
                        viewModel.addComment(postId, commentBodyText)
                        commentBodyText = ""
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("send_comment_button"),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send Comment",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// CUSTOM UTILITIES
// ==========================================
fun Modifier.fillModifier(): Modifier = this.fillMaxWidth()

class CustomClickableInteractionSource : androidx.compose.foundation.interaction.MutableInteractionSource {
    private val flow = kotlinx.coroutines.flow.MutableSharedFlow<androidx.compose.foundation.interaction.Interaction>(
        extraBufferCapacity = 16,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    override val interactions: kotlinx.coroutines.flow.Flow<androidx.compose.foundation.interaction.Interaction> = flow
    override suspend fun emit(interaction: androidx.compose.foundation.interaction.Interaction) { flow.emit(interaction) }
    override fun tryEmit(interaction: androidx.compose.foundation.interaction.Interaction): Boolean { return flow.tryEmit(interaction) }
}
