package com.example

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.MediaStore
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.speech.RecognizerIntent
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.PickVisualMediaRequest
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.data.*
import com.example.network.RetrofitClient
import com.example.ui.*
import com.example.ui.theme.Gold
import com.example.ui.theme.KortexTheme
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.ClearCredentialStateRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.appCheck
import kotlinx.coroutines.tasks.await
import android.app.Activity
import android.util.Log

object AppConstants {
    val LANGUAGES = listOf(
        "Hinglish", "Hindi", "English", "Bengali", "Marathi", "Telugu", "Tamil", "Gujarati",
        "Kannada", "Malayalam", "Odia", "Punjabi", "Assamese", "Urdu", "Bhojpuri", "Maithili",
        "Rajasthani", "Kashmiri", "Konkani", "Sindhi", "Dogri", "Manipuri (Meitei)", "Bodo",
        "Santali", "Nepali", "Sanskrit"
    )
    val BOARDS = listOf(
        "CBSE", "ICSE/ISC", "NIOS", "Andhra Pradesh (BIEAP)", "Arunachal Pradesh (SBSE)",
        "Assam (SEBA/AHSEC)", "Bihar (BSEB)", "Chhattisgarh (CGBSE)", "Goa (GBSHSE)",
        "Gujarat (GSEB)", "Haryana (BSEH)", "Himachal Pradesh (HPBOSE)", "Jharkhand (JAC)",
        "Jammu & Kashmir (JKBOSE)", "Karnataka (KSEEB)", "Kerala (SCERT/DHSE)",
        "Madhya Pradesh (MPBSE)", "Maharashtra (MSBSHSE)", "Manipur (BSEM/COHSEM)",
        "Meghalaya (MBOSE)", "Mizoram (MBSE)", "Nagaland (NBSE)", "Odisha (BSE/CHSE)",
        "Punjab (PSEB)", "Rajasthan (RBSE)", "Sikkim (SBOSE/HSLC)", "Tamil Nadu (TNBSE)",
        "Telangana (TSBIE)", "Tripura (TBSE)", "Uttar Pradesh (UPMSP)", "Uttarakhand (UBSE)",
        "West Bengal (WBBSE/WBCHSE)", "Puducherry (DSE)"
    )
    val SCHOOL_CLASSES = (6..12).map { "Class $it" }
    val DEGREES = listOf(
        "B.A.", "B.Sc.", "B.Com", "BBA", "BCA", "B.Tech/B.E.", "LLB", "B.Pharm", "MBBS",
        "BDS", "B.Arch", "B.Ed", "BHM", "B.Sc. Nursing", "BFA/BVA", "B.Des", "BSW",
        "M.A.", "M.Sc.", "MBA", "M.Tech", "MD/MS"
    )
    val CLASSES = SCHOOL_CLASSES + DEGREES
    val SUBJECTS = listOf(
        "Math", "Physics", "Chemistry", "Biology", "English", "History", "Geography",
        "Civics", "Economics", "Computer Science", "Accounting", "Business Studies",
        "Social Science", "Hindi", "Sanskrit", "General Knowledge"
    )
}

fun Uri.toBitmap(context: android.content.Context): Bitmap? {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, this)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, this)
        }
        
        // Resize if too large for API (Gemini has limits, and it saves memory)
        val maxDimension = 1024
        if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = maxDimension.toFloat() / Math.max(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        null
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(this, intent)
        setContent {
            KortexTheme {
                KortexApp()
            }
        }
    }

    private fun configureAppCheck(context: android.content.Context, intent: Intent) {
        // In AI Studio / Debug environments, always use Debug provider
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
    }
}

@Composable
fun KortexApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember {
        AppRepository(
            context,
            database.doubtDao(),
            database.streakDao(),
            database.goalDao(),
            database.userProfileDao(),
            RetrofitClient.service
        )
    }
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(repository))
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsStateWithLifecycle(initialValue = null)
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle(initialValue = null)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val isNewUser by viewModel.isNewUser.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(authState) {
        if (authState != null) {
            viewModel.checkUserExists()
        }
    }

    if (authState == null) {
        NavHost(navController = navController, startDestination = Screen.Splash) {
            composable<Screen.Splash> {
                SplashScreen { navController.navigate(Screen.Onboarding) }
            }
            composable<Screen.Onboarding> {
                OnboardingScreen { navController.navigate(Screen.Login) }
            }
            composable<Screen.Login> {
                LoginScreen(viewModel, navController)
            }
            composable<Screen.PhoneAuth> { backStackEntry ->
                val isSignUp = backStackEntry.arguments?.getBoolean("isSignUp") ?: true
                PhoneAuthScreen(viewModel, navController, isSignUp)
            }
            composable<Screen.OTP> {
                OTPVerifyScreen(viewModel, navController)
            }
        }
    } else if (isNewUser == null) {
        // Loading state
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    viewModel = viewModel,
                    navController = navController,
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                bottomBar = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    
                    if (shouldShowBottomBar(currentRoute)) {
                        BottomNavigationBar(navController)
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = if (userProfile?.isOnboarded == true) Screen.Home else Screen.Setup,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable<Screen.Setup> {
                        SetupScreen(viewModel) { navController.navigate(Screen.Home) {
                            popUpTo(Screen.Setup) { inclusive = true }
                        } }
                    }
                    composable<Screen.Home> {
                        HomeScreen(viewModel, navController, onOpenDrawer = { scope.launch { drawerState.open() } })
                    }
                    composable<Screen.Chat> {
                        ChatScreen(viewModel, navController)
                    }
                    composable<Screen.AskDoubt> {
                        AskDoubtScreen(viewModel, navController)
                    }
                    composable<Screen.Solution> {
                        SolutionScreen(viewModel, navController)
                    }
                    composable<Screen.MyDoubts> {
                        MyDoubtsScreen(viewModel, navController)
                    }
                    composable<Screen.Streak> {
                        StreakScreen(viewModel, navController)
                    }
                    composable<Screen.Profile> {
                        ProfileScreen(viewModel, navController)
                    }
                    composable<Screen.Models> {
                        ModelsScreen(viewModel, navController)
                    }
                    composable<Screen.Notifications> {
                        NotificationsScreen(viewModel, navController)
                    }
                    composable<Screen.DoubtDetail> { backStackEntry ->
                        val doubtId = backStackEntry.arguments?.getLong("doubtId") ?: 0L
                        DoubtDetailScreen(doubtId, viewModel, navController)
                    }
                    composable<Screen.StudyGroups> {
                        StudyGroupsScreen(viewModel, navController)
                    }
                    composable<Screen.Visuals> {
                        VisualsScreen(viewModel, navController)
                    }
                    composable<Screen.Library> {
                        LibraryScreen(viewModel, navController)
                    }
                }
            }
        }
    }
}

fun shouldShowBottomBar(route: String?): Boolean {
    return route?.contains("Home") == true ||
            route?.contains("MyDoubts") == true ||
            route?.contains("Streak") == true ||
            route?.contains("Notifications") == true ||
            route?.contains("Library") == true ||
            route?.contains("Profile") == true
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Triple(Screen.Home, Icons.Default.Home, "Home"),
        Triple(Screen.MyDoubts, Icons.Default.Description, "Doubts"),
        Triple(Screen.Streak, Icons.Default.Whatshot, "Streak"),
        Triple(Screen.Notifications, Icons.Default.Notifications, "Alerts"),
        Triple(Screen.Profile, Icons.Default.Person, "Profile")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentIndex = items.indexOfFirst { (screen, _, _) ->
        currentDestination?.route?.contains(screen::class.simpleName ?: "") == true
    }.coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(28.dp)
            )
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                clip = false,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
    ) {
        // Glass Background with Morphing Shapes
        val infiniteTransition = rememberInfiniteTransition(label = "glass_nav")
        val animX by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "x"
        )
        
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().blur(30.dp)) {
            drawCircle(
                color = Color(0xFF4C9BFF).copy(alpha = 0.15f),
                radius = size.width * 0.4f,
                center = androidx.compose.ui.geometry.Offset(size.width * animX, size.height * 0.5f)
            )
            drawCircle(
                color = Color(0xFF00C853).copy(alpha = 0.1f),
                radius = size.width * 0.3f,
                center = androidx.compose.ui.geometry.Offset(size.width * (1f - animX), size.height * 0.8f)
            )
        }
        // Morphing Pill
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val itemWidth = maxWidth / items.size
            val pillOffset by animateDpAsState(
                targetValue = itemWidth * currentIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "pillOffset"
            )

            Box(
                modifier = Modifier
                    .offset(x = pillOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp))
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, (screen, icon, label) ->
                val isSelected = currentIndex == index
                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "iconColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val route = when(screen) {
                                is Screen.Home -> "com.example.Screen.Home"
                                is Screen.AskDoubt -> "com.example.Screen.AskDoubt"
                                is Screen.Chat -> "com.example.Screen.Chat"
                                is Screen.Profile -> "com.example.Screen.Profile"
                                else -> screen.toString()
                            }
                            navController.navigate(screen) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = animatedColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = animatedColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppDrawer(
    viewModel: MainViewModel,
    navController: NavHostController,
    onClose: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val doubts by viewModel.allDoubts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAccountSheet by remember { mutableStateOf(false) }

    if (showAccountSheet) {
        AppAccountSheet(
            viewModel = viewModel,
            navController = navController,
            onDismiss = { showAccountSheet = false }
        )
    }

    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFCFE0D2))
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text("Kortex AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }

            Surface(
                color = Color(0xFF20281F),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp, 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }

            DrawerItem(Icons.Default.Add, "New Doubt") { onClose(); navController.navigate(Screen.AskDoubt) }
            DrawerItem(Icons.Default.Folder, "My Doubts") { onClose(); navController.navigate(Screen.MyDoubts) }
            DrawerItem(Icons.Default.Bookmark, "Saved Solutions") { onClose(); navController.navigate(Screen.MyDoubts) }
            DrawerItem(Icons.Default.AutoAwesome, "AI Visuals") { onClose(); navController.navigate(Screen.Visuals) }
            DrawerItem(Icons.Default.Groups, "Study Groups", hasProBadge = true) { onClose(); navController.navigate(Screen.StudyGroups) }
            DrawerItem(Icons.Default.Download, "Download APK") { 
                onClose()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ranjeet-alamani/Kortex-AI/releases"))
                context.startActivity(intent)
            }
            DrawerItem(Icons.Default.LibraryBooks, "Assets Library") { onClose(); navController.navigate(Screen.Library) }
            DrawerItem(Icons.Default.Settings, "Settings") { onClose(); navController.navigate(Screen.Profile) }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Doubts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.SwapVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(doubts.take(5)) { doubt ->
                    RecentDoubtItem(doubt) { onClose(); navController.navigate(Screen.DoubtDetail(doubt.id)) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAccountSheet = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userProfile?.name?.take(2)?.uppercase() ?: "RK",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF0A4A35)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(userProfile?.name ?: "Rohit Kumar", style = MaterialTheme.typography.titleSmall)
                    Text(if (userProfile?.isPro == true) "Pro" else "Free", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DrawerItem(icon: ImageVector, label: String, hasProBadge: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFFEEF2EA))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFEEF2EA), modifier = Modifier.weight(1f))
        if (hasProBadge) {
            Surface(
                color = Color(0xFF4C9BFF).copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4C9BFF).copy(alpha = 0.35f))
            ) {
                Text(
                    "Pro",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4C9BFF)
                )
            }
        }
    }
}

@Composable
fun RecentDoubtItem(doubt: Doubt, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant))
        Text(doubt.question, style = MaterialTheme.typography.bodySmall, color = Color(0xFFDBE6DD), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppAccountSheet(
    viewModel: MainViewModel,
    navController: NavHostController,
    onDismiss: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                userProfile?.mobileNumber ?: "rohit.k@email.com",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            AccountSheetItem(Icons.Default.Settings, "Settings") { onDismiss(); navController.navigate(Screen.Profile) }
            AccountSheetItem(Icons.Default.Language, "Language") { onDismiss(); navController.navigate(Screen.Setup) }
            AccountSheetItem(Icons.Default.HelpOutline, "Get Help") { /* Help */ }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.09f), modifier = Modifier.padding(vertical = 6.dp))
            
            AccountSheetItem(Icons.Default.Upgrade, "Upgrade to Pro") { onDismiss(); navController.navigate(Screen.Models) }
            AccountSheetItem(Icons.Default.CardGiftcard, "Invite Friends") { /* Invite */ }
            AccountSheetItem(Icons.Default.Info, "Learn more about Kortex AI") { /* Learn more */ }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.09f), modifier = Modifier.padding(vertical = 6.dp))
            
            AccountSheetItem(Icons.Default.Logout, "Log out", color = Color(0xFFFF6B6B)) { 
                onDismiss()
                viewModel.signOut()
            }
        }
    }
}

@Composable
fun AccountSheetItem(icon: ImageVector, label: String, color: Color = Color(0xFFEEF2EA), onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = color)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- Screens ---

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onFinish()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "KORTEX AI",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "Learn in your own language",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    var currentSlide by remember { mutableIntStateOf(0) }
    val slides = listOf(
        OnboardingSlide(
            "Snap a photo, get your answer",
            "Ask your question by photo or voice — get simple step-by-step explanations in Hindi, Hinglish, English, or your regional language.",
            "📸",
            MaterialTheme.colorScheme.tertiary
        ),
        OnboardingSlide(
            "Learn in your own language",
            "Kortex AI supports Hindi, Hinglish, and major regional languages to make learning easy for everyone.",
            "🌐",
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        OnboardingSlide(
            "Consistency is key",
            "Build a study habit with our streak system and micro-goals. Solve doubts every day and stay on track.",
            "🔥",
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(slides[currentSlide].color),
                contentAlignment = Alignment.Center
            ) {
                Text(slides[currentSlide].emoji, fontSize = 80.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                slides[currentSlide].title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                slides[currentSlide].subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(slides.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentSlide) 18.dp else 6.dp, 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (index == currentSlide) MaterialTheme.colorScheme.onSurface else Color.LightGray)
                    )
                }
            }
            
            Button(
                onClick = {
                    if (currentSlide < slides.size - 1) {
                        currentSlide++
                    } else {
                        onContinue()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (currentSlide < slides.size - 1) "Next" else "Get Started")
            }
        }
    }
}

data class OnboardingSlide(val title: String, val subtitle: String, val emoji: String, val color: Color)

@Composable
fun LoginScreen(viewModel: MainViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()

    fun onGoogleSignInClicked() {
        val clientId = context.getString(R.string.default_web_client_id)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(context as Activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    viewModel.signInWithGoogle(googleIdToken)
                }
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.White)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Welcome to Kortex AI", style = MaterialTheme.typography.headlineMedium)
        Text("Your personal study assistant", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { navController.navigate(Screen.PhoneAuth(isSignUp = true)) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Create New Account", style = MaterialTheme.typography.labelLarge)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { navController.navigate(Screen.PhoneAuth(isSignUp = false)) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Sign In", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text("or continue with", modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = { onGoogleSignInClicked() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isAuthenticating
        ) {
            if (isAuthenticating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Google")
                }
            }
        }
    }
}

@Composable
fun PhoneAuthScreen(viewModel: MainViewModel, navController: NavHostController, isSignUp: Boolean) {
    var phoneNumber by remember { mutableStateOf("") }
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val isOtpSent by viewModel.isOtpSent.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val context = LocalContext.current as Activity

    LaunchedEffect(isOtpSent) {
        if (isOtpSent) {
            navController.navigate(Screen.OTP)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (isSignUp) "Create Account" else "Sign In",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            if (isSignUp) "Enter your mobile number to get started" else "Welcome back! Enter your number to sign in",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { input -> if (input.length <= 10 && input.all { char -> char.isDigit() }) phoneNumber = input },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Mobile Number") },
            prefix = { Text("+91 ") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
        )
        
        authError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.sendOtp(phoneNumber, context) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = phoneNumber.length == 10 && !isAuthenticating
        ) {
            if (isAuthenticating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.surface)
            } else {
                Text("Get OTP")
            }
        }
    }
}

@Composable
fun OTPVerifyScreen(viewModel: MainViewModel, navController: NavHostController) {
    var otpCode by remember { mutableStateOf("") }
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val cooldown by viewModel.cooldown.collectAsStateWithLifecycle()
    val context = LocalContext.current as Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Verify OTP", style = MaterialTheme.typography.headlineMedium)
        Text("Enter the 6-digit code sent to your number", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = otpCode,
            onValueChange = { input -> if (input.length <= 6 && input.all { char -> char.isDigit() }) otpCode = input },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("OTP Code") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )

        authError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.verifyOtp(otpCode) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = otpCode.length == 6 && !isAuthenticating
        ) {
            if (isAuthenticating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.surface)
            } else {
                Text("Verify & Continue")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        if (cooldown > 0) {
            Text("Resend OTP in ${cooldown}s", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            TextButton(onClick = { /* Resend logic */ }) {
                Text("Resend OTP", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(viewModel: MainViewModel, onFinish: () -> Unit) {
    var name by remember { mutableStateOf("Rohit Kumar") }
    var language by remember { mutableStateOf("Hinglish") }
    var board by remember { mutableStateOf("Maharashtra Board") }
    var className by remember { mutableStateOf("Class 10") }

    val languages = AppConstants.LANGUAGES
    val boards = AppConstants.BOARDS
    val classes = AppConstants.CLASSES

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        item {
            Text("Set up your profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Full Name", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Choose Language", style = MaterialTheme.typography.labelLarge)
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                languages.forEach { lang ->
                    FilterChip(
                        selected = language == lang,
                        onClick = { language = lang },
                        label = { Text(lang) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Board", style = MaterialTheme.typography.labelLarge)
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                boards.forEach { b ->
                    FilterChip(
                        selected = board == b,
                        onClick = { board = b },
                        label = { Text(b) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Class", style = MaterialTheme.typography.labelLarge)
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                classes.forEach { c ->
                    FilterChip(
                        selected = className == c,
                        onClick = { className = c },
                        label = { Text(c) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.saveProfile(name, language, board, className, "+919800000000", isOnboarded = true)
                    onFinish()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel, navController: NavHostController, onOpenDrawer: () -> Unit) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val doubts by viewModel.allDoubts.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                    Text("Hello 👋", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                }
                Text(userProfile?.name ?: "Guest", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(start = 34.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("🔥 5-day streak", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.MyDoubts) }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search your doubt or topic...", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .clickable { navController.navigate(Screen.AskDoubt) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("📷 Ask a Doubt Now", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clickable { navController.navigate(Screen.Visuals) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.onTertiary)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Infographics", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("Generate visual study notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clickable { navController.navigate(Screen.Chat) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.tertiary), contentAlignment = Alignment.Center) {
                        Text("🤖", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chat with Kortex AI", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Using Kortex 0.5 · Free", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        item {
            Text("Subjects", modifier = Modifier.padding(18.dp, 18.dp, 18.dp, 8.dp), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            val subjects = listOf("Math", "Science", "English", "Social Sci", "Hindi", "More")
            val icons = listOf("📐", "🧪", "📖", "🌍", "✍️", "➕")
            
            Row(modifier = Modifier.padding(horizontal = 18.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SubjectGrid(subjects.take(3), icons.take(3))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SubjectGrid(subjects.drop(3), icons.drop(3))
                }
            }
        }

        item {
            Text("Recent Doubts", modifier = Modifier.padding(18.dp, 24.dp, 18.dp, 8.dp), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }

        items(doubts.take(3)) { doubt ->
            DoubtCard(doubt) { navController.navigate(Screen.DoubtDetail(doubt.id)) }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun SubjectGrid(names: List<String>, icons: List<String>) {
    names.forEachIndexed { index, name ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(icons[index], style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun DoubtCard(doubt: Doubt, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(doubt.question, style = MaterialTheme.typography.titleSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                Text("${doubt.subject} • 2 hours ago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            ) {
                Text(doubt.status, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF0F5B2E))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MainViewModel, navController: NavHostController) {
    var message by remember { mutableStateOf("") }
    val isSolving by viewModel.isSolving.collectAsStateWithLifecycle()
    val doubts by viewModel.allDoubts.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    var selectedLanguage by remember { mutableStateOf(userProfile?.language ?: "Hinglish") }
    var selectedClass by remember { mutableStateOf(userProfile?.className ?: "Class 10") }
    var selectedBoard by remember { mutableStateOf(userProfile?.board ?: "CBSE") }
    var isWebSearchEnabled by remember { mutableStateOf(false) }
    
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showClassPicker by remember { mutableStateOf(false) }
    var showBoardPicker by remember { mutableStateOf(false) }
    var showSubjectPicker by remember { mutableStateOf(false) }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                message = results[0]
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedBitmap = uri.toBitmap(context)
        }
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            attachedBitmap = bitmap
        }
    }

    if (showLanguagePicker) {
        GenericPickerSheet(
            title = "Select Language",
            items = AppConstants.LANGUAGES,
            onDismiss = { showLanguagePicker = false },
            onSelect = { selectedLanguage = it; showLanguagePicker = false }
        )
    }
    if (showClassPicker) {
        GenericPickerSheet(
            title = "Select Class/Degree",
            items = AppConstants.CLASSES,
            onDismiss = { showClassPicker = false },
            onSelect = { selectedClass = it; showClassPicker = false }
        )
    }
    if (showBoardPicker) {
        GenericPickerSheet(
            title = "Select Board",
            items = AppConstants.BOARDS,
            onDismiss = { showBoardPicker = false },
            onSelect = { selectedBoard = it; showBoardPicker = false }
        )
    }
    if (showSubjectPicker) {
        GenericPickerSheet(
            title = "Select Subject",
            items = AppConstants.SUBJECTS,
            onDismiss = { showSubjectPicker = false },
            onSelect = { 
                message += " [Subject: $it]"
                showSubjectPicker = false 
            }
        )
    }

    if (showAttachmentSheet) {
        AttachmentSheet(
            onDismiss = { showAttachmentSheet = false },
            onTakePhoto = { takePhotoLauncher.launch(null) },
            onPickImage = { pickImageLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
            onVoiceNote = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                }
                voiceLauncher.launch(intent)
            },
            onChooseSubject = { showSubjectPicker = true },
            isWebSearchEnabled = isWebSearchEnabled,
            onWebSearchToggle = { isWebSearchEnabled = it }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Kortex AI Chat", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            }
        )
        
        // Selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatSelector(selectedLanguage, Modifier.weight(1f)) { showLanguagePicker = true }
            ChatSelector(selectedClass, Modifier.weight(1f)) { showClassPicker = true }
            ChatSelector(selectedBoard, Modifier.weight(1f)) { showBoardPicker = true }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
            reverseLayout = true
        ) {
            items(doubts) { doubt ->
                ChatBubble(doubt) { viewModel.markAsSolved(doubt) }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("Hi ${userProfile?.name ?: "Rohit"}! I'm your Kortex AI tutor. Ask me anything from your syllabus. 👋", 
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }

        if (isSolving) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
        }

        attachedBitmap?.let {
            Box(modifier = Modifier.padding(start = 14.dp, bottom = 4.dp).size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f))) {
                androidx.compose.foundation.Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                IconButton(onClick = { attachedBitmap = null }, modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }

        // Composer
        Surface(
            modifier = Modifier.padding(12.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shadowElevation = 8.dp
        ) {
            Column {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showAttachmentSheet = true }) { 
                        Icon(Icons.Default.Add, null, tint = Color.White) 
                    }
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a message...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        maxLines = 4
                    )
                    
                    if (isSolving) {
                        IconButton(onClick = { viewModel.stopSolving() }) { 
                            Icon(Icons.Default.Stop, null, tint = Color.White) 
                        }
                    } else {
                        IconButton(onClick = { 
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            }
                            voiceLauncher.launch(intent)
                        }) { 
                            Icon(Icons.Default.Mic, null, tint = Color(0xFFCFE0D2)) 
                        }
                        IconButton(
                            onClick = {
                                if (message.isNotBlank() || attachedBitmap != null) {
                                    viewModel.solveDoubt(message, "General", attachedBitmap)
                                    message = ""
                                    attachedBitmap = null
                                }
                            },
                            enabled = message.isNotBlank() || attachedBitmap != null
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                
                // Model Switcher inside bar
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showModelPicker = true }
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (userProfile?.isPro == true) "Kortex 2.0 Pro" else "Kortex 0.5",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            if (userProfile?.isPro == true) "Subscription" else "Free",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    }
                    
                    if (showModelPicker) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = (-40).dp)
                                .width(220.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF182119),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            shadowElevation = 12.dp
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                ModelPickerItem("⚡ Kortex 0.5", "Free", userProfile?.isPro == false) {
                                    viewModel.setPro(false)
                                    showModelPicker = false
                                }
                                ModelPickerItem("👑 Kortex 2.0 Pro", "Subscription", userProfile?.isPro == true) {
                                    viewModel.setPro(true)
                                    showModelPicker = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModelPickerItem(name: String, tier: String, isActive: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(11.dp, 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = Color(0xFFE8F2EA), style = MaterialTheme.typography.bodySmall)
        Text(tier, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
    }
}

@Composable
fun ChatSelector(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ChatBubble(doubt: Doubt, onMarkAsSolved: () -> Unit) {
    // ... rest of the improved ChatBubble ...
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // User Question
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                .padding(12.dp, 14.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (doubt.imagePath != null) {
                    Box(modifier = Modifier.size(160.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, null, tint = Color.White.copy(alpha = 0.5f))
                        Text("Photo attached", color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(doubt.question, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
            }
        }
        
        if (doubt.solution.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            // AI Response
            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                    .padding(12.dp, 14.dp)
            ) {
                Column {
                MarkdownText(
                    markdown = doubt.solution,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (doubt.status == "Solved") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Solved & Streak updated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    } else {
                        Button(
                            onClick = onMarkAsSolved,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
                        ) {
                            Icon(Icons.Default.Done, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark as Solved", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onVoiceNote: () -> Unit,
    onChooseSubject: () -> Unit,
    isWebSearchEnabled: Boolean,
    onWebSearchToggle: (Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Web Search", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isWebSearchEnabled,
                        onCheckedChange = onWebSearchToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
            
            AttachmentItem(Icons.Default.AttachFile, "Add files or photos") { onPickImage(); onDismiss() }
            AttachmentItem(Icons.Default.PhotoCamera, "Take a photo") { onTakePhoto(); onDismiss() }
            AttachmentItem(Icons.Default.Folder, "Add to My Doubts") { onDismiss() }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.09f), modifier = Modifier.padding(vertical = 6.dp))
            
            AttachmentItem(Icons.Default.LibraryBooks, "Choose Subject") { onChooseSubject(); onDismiss() }
            AttachmentItem(Icons.Default.Image, "Choose from Gallery") { onPickImage(); onDismiss() }
            AttachmentItem(Icons.Default.Mic, "Voice Note") { onVoiceNote(); onDismiss() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericPickerSheet(
    title: String,
    items: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .padding(16.dp, 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color(0xFFEEF2EA))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@Composable
fun AskDoubtScreen(viewModel: MainViewModel, navController: NavHostController) {
    var question by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Math") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    val isSolving by viewModel.isSolving.collectAsStateWithLifecycle()
    val solution by viewModel.solutionState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            selectedImageUri = null
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            selectedBitmap = null
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                question = results[0]
            }
        }
    }

    LaunchedEffect(solution) {
        if (solution != null && solution != "Problem solving cancelled.") {
            navController.navigate(Screen.Solution)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(title = { Text("Ask a Doubt") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        })
        
        LazyColumn(modifier = Modifier.padding(24.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                        .clickable { takePhotoLauncher.launch(null) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (selectedImageUri != null) {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Take a photo of your question", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    
                    if (selectedBitmap != null || selectedImageUri != null) {
                        IconButton(
                            onClick = { selectedBitmap = null; selectedImageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickImageLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    OutlinedButton(
                        onClick = { 
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your question")
                            }
                            voiceLauncher.launch(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Mic, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Voice", style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Or Type your question", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    placeholder = { Text("What is your doubt? e.g. Solve x^2 + 5x + 6 = 0", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                Text("Subject", style = MaterialTheme.typography.labelLarge)
                var showSubjectPicker by remember { mutableStateOf(false) }
                if (showSubjectPicker) {
                    GenericPickerSheet(
                        title = "Select Subject",
                        items = AppConstants.SUBJECTS,
                        onDismiss = { showSubjectPicker = false },
                        onSelect = { subject = it; showSubjectPicker = false }
                    )
                }
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showSubjectPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(subject, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { 
                        val finalBitmap = selectedBitmap ?: selectedImageUri?.toBitmap(context)
                        viewModel.solveDoubt(question, subject, finalBitmap) 
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isSolving && (question.isNotBlank() || selectedBitmap != null || selectedImageUri != null)
                ) {
                    if (isSolving) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.surface, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Solving...")
                        }
                    } else {
                        Text("Get AI Solution")
                    }
                }
                if (isSolving) {
                    TextButton(onClick = { viewModel.stopSolving() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun SolutionScreen(viewModel: MainViewModel, navController: NavHostController) {
    val solution by viewModel.solutionState.collectAsStateWithLifecycle()
    val doubts by viewModel.allDoubts.collectAsStateWithLifecycle()
    val latestDoubt = doubts.firstOrNull()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(title = { Text("AI Solution", style = MaterialTheme.typography.titleLarge) }, navigationIcon = {
            IconButton(onClick = { viewModel.clearSolution(); navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        })
        
        Column(modifier = Modifier.padding(24.dp).weight(1f).verticalScroll(rememberScrollState())) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kortex Solution", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    MarkdownText(
                        markdown = solution ?: "No solution found.",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { 
                    latestDoubt?.let { viewModel.markAsSolved(it) }
                    viewModel.clearSolution()
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("✓ Mark as Solved & Home", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun MyDoubtsScreen(viewModel: MainViewModel, navController: NavHostController) {
    val doubts by viewModel.allDoubts.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("My Doubts") })
        if (doubts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", style = MaterialTheme.typography.displayMedium)
                    Text("No doubts yet", style = MaterialTheme.typography.titleLarge)
                    Text("Ask your first question and get an instant answer.", textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    Button(onClick = { navController.navigate(Screen.AskDoubt) }) { Text("Ask a Doubt") }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(18.dp)) {
                items(doubts) { doubt ->
                    DoubtCard(doubt) { navController.navigate(Screen.DoubtDetail(doubt.id)) }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun StreakScreen(viewModel: MainViewModel, navController: NavHostController) {
    val streaks by viewModel.allStreaks.collectAsStateWithLifecycle()
    val goals by viewModel.todayGoals.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(18.dp)) {
        item {
            TopAppBar(title = { Text("Study Streak") })
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥", style = MaterialTheme.typography.displayMedium)
                    Text("${streaks.size} Days", style = MaterialTheme.typography.headlineSmall)
                    Text("Keep your streak going!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Today's Micro-Goals", style = MaterialTheme.typography.titleMedium)
        }
        items(goals) { goal ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.toggleGoal(goal) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (goal.isCompleted) "✅" else "⬜")
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(goal.title, textDecoration = if (goal.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: MainViewModel, navController: NavHostController) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateProfilePicture(it.toString()) }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(title = { Text("Profile") })
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { photoPickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profile?.profilePicture != null) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(profile?.profilePicture),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(profile?.name?.take(2)?.uppercase() ?: "RK", style = MaterialTheme.typography.headlineSmall)
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }
            
            Text(profile?.name ?: "Rohit Kumar", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
            Text(profile?.mobileNumber ?: "rohit.k@email.com", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(32.dp))
            profile?.let {
                ProfileItem("Board / Class", "${it.board}, ${it.className}")
                ProfileItem("Preferred Language", it.language)
                ProfileItem("AI Model", if (it.isPro) "Kortex 2.0 Pro" else "Kortex 0.5")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { viewModel.signOut() }, 
                modifier = Modifier.fillMaxWidth(), 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = Color.Red),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1C1BB))
            ) {
                Text("Log out")
            }
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NotificationsScreen(viewModel: MainViewModel, navController: NavHostController) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(title = { Text("Notifications") })
        
        if (notifications.isEmpty()) {
            EmptyState(
                icon = Icons.Default.NotificationsNone,
                message = "No notifications yet",
                buttonText = "Back to Home",
                onAction = { navController.navigate(Screen.Home) }
            )
        } else {
            LazyColumn(modifier = Modifier.padding(18.dp)) {
                items(notifications) { notification ->
                    NotificationCard(notification) {
                        navController.navigate(Screen.Home) // Simplified
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: Notification, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, null, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(notification.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(notification.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun DoubtDetailScreen(doubtId: Long, viewModel: MainViewModel, navController: NavHostController) {
    val doubts by viewModel.allDoubts.collectAsStateWithLifecycle()
    val doubt = doubts.find { it.id == doubtId }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Doubt Detail") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            }
        )
        
        if (doubt == null) {
            EmptyState(icon = Icons.Default.Search, message = "Doubt not found", onAction = { navController.popBackStack() })
        } else {
            LazyColumn(modifier = Modifier.padding(18.dp)) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Text(doubt.status, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF0F5B2E))
                    }
                    Text(doubt.subject, style = MaterialTheme.typography.headlineSmall)
                    Text("Asked on ${SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(doubt.timestamp))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Original Question", style = MaterialTheme.typography.titleSmall)
                            Text(doubt.question, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Concept Tags", style = MaterialTheme.typography.titleSmall)
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = true, onClick = {}, label = { Text(doubt.subject) })
                                FilterChip(selected = false, onClick = {}, label = { Text("AI Solved") })
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Solution", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = doubt.solution,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate(Screen.Chat) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("🔁 Ask a follow-up", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun StudyGroupsScreen(viewModel: MainViewModel, navController: NavHostController) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val groups = listOf(
        StudyGroup(1, "Class 10 Math Wizards", 156),
        StudyGroup(2, "Board Exam Prep 2026", 1240),
        StudyGroup(3, "Science Olympiad Squad", 89),
        StudyGroup(4, "English Grammar Pros", 442)
    )

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Study Groups") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            }
        )
        
        if (userProfile?.isPro == false) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👑", style = MaterialTheme.typography.displayMedium)
                    Text("Study Groups is a Pro Feature", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Join peer study groups and find accountability partners with Kortex Pro.",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { navController.navigate(Screen.Models) }) { Text("Upgrade to Pro") }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(18.dp)) {
                item {
                    Text("Peer Groups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                items(groups) { group ->
                    StudyGroupCard(group)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun StudyGroupCard(group: StudyGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Text(group.name.take(1), style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("${group.members} members", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Button(onClick = { /* Join */ }, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text("Join", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, message: String, buttonText: String? = null, onAction: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "Try asking a doubt or searching for a topic.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
            if (buttonText != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(12.dp)) { Text(buttonText) }
            }
        }
    }
}
@Composable
fun ModelsScreen(viewModel: MainViewModel, navController: NavHostController) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(title = { Text("Kortex AI Assistant") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        })
        
        LazyColumn(modifier = Modifier.padding(18.dp)) {
            item {
                Text("Choose the AI model that fits your needs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Surface(color = MaterialTheme.colorScheme.tertiary, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        "🔄 Both models are continuously updated — new syllabus content, current affairs, and reasoning improvements roll out automatically.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                ModelCard(
                    title = "Kortex 0.5",
                    tier = "Free",
                    icon = "⚡",
                    description = "Solid general knowledge for everyday doubts across all subjects. Stays updated with current news.",
                    features = listOf("Answers questions on any topic", "Knows today's date", "Step-by-step explanations"),
                    isActive = profile?.isPro == false,
                    onSelect = { viewModel.setPro(false) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                ModelCard(
                    title = "Kortex 2.0 Pro",
                    tier = "Subscription",
                    icon = "👑",
                    description = "Pro-level reasoning built for tough, multi-step problems. Advanced exam-oriented logic.",
                    features = listOf("Advanced reasoning", "Deep exam-oriented solutions", "Unlimited doubt solving", "Priority speed"),
                    isActive = profile?.isPro == true,
                    isPro = true,
                    onSelect = { viewModel.setPro(true) }
                )
            }
        }
    }
}

@Composable
fun ModelCard(
    title: String,
    tier: String,
    icon: String,
    description: String,
    features: List<String>,
    isActive: Boolean,
    isPro: Boolean = false,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        border = if (!isPro) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(if (isPro) Gold else MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = if (isPro) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                    Surface(shape = RoundedCornerShape(20.dp), color = if (isPro) Gold else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)) {
                        Text(tier, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (isActive) {
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)) {
                        Text("✓ Active", modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF0F5B2E))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(description, style = MaterialTheme.typography.labelSmall, color = if (isPro) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            features.forEach { feat ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(if (isPro) "✨" else "✅", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(feat, style = MaterialTheme.typography.labelSmall, color = if (isPro) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPro) Gold else Color.Transparent,
                    contentColor = if (isPro) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(14.dp),
                border = if (!isPro) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Text(if (isActive) "Currently Selected" else if (isPro) "Upgrade to Pro — ₹99/mo" else "Select")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualsScreen(viewModel: MainViewModel, navController: NavHostController) {
    var topic by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Science") }
    val isGenerating by viewModel.isGeneratingVisual.collectAsStateWithLifecycle()
    val generatedImage by viewModel.generatedInfographic.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("AI Visual Explorer") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            }
        )

        LazyColumn(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            item {
                Text("Transform concepts into visual infographics instantly.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. How Photosynthesis works") },
                    label = { Text("Concept or Topic") },
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (topic.isNotBlank()) {
                            IconButton(onClick = { topic = "" }) { Icon(Icons.Default.Clear, null) }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedSubject == "Science",
                        onClick = { selectedSubject = "Science" },
                        label = { Text("Science") }
                    )
                    FilterChip(
                        selected = selectedSubject == "Commerce",
                        onClick = { selectedSubject = "Commerce" },
                        label = { Text("Commerce") }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { viewModel.generateInfographic(topic, selectedSubject) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isGenerating && topic.isNotBlank()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Generating Visual...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Infographic")
                    }
                }
            }

            if (isGenerating || generatedImage != null) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Result", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGenerating) {
                            DiffusionLoop()
                        } else if (generatedImage != null) {
                            androidx.compose.foundation.Image(
                                bitmap = generatedImage!!.asImageBitmap(),
                                contentDescription = "Generated Infographic",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            
                            // Share/Download buttons
                            Row(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FloatingActionButton(
                                    onClick = { 
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Check out this AI-generated infographic on $topic! #KortexAI")
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Share, null)
                                }
                                
                                FloatingActionButton(
                                    onClick = { /* Download logic - placeholder */ },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Download, null)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Try these topics", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                val suggestions = if (selectedSubject == "Science") {
                    listOf("Water Cycle", "Plant Cell", "Human Heart", "Renewable Energy")
                } else {
                    listOf("GST Flow", "Market Cycle", "Balance Sheet", "Supply & Demand")
                }
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        AssistChip(
                            onClick = { topic = suggestion },
                            label = { Text(suggestion) },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun DiffusionLoop() {
    val infiniteTransition = rememberInfiniteTransition(label = "diffusion")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val color1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val color2 = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    val color3 = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val time = progress * 2 * Math.PI.toFloat()
            
            // Draw morphing gradients
            drawCircle(
                color = color1,
                radius = (width * 0.5f) * (1f + 0.1f * Math.sin(time.toDouble()).toFloat()),
                center = androidx.compose.ui.geometry.Offset(
                    x = width * (0.4f + 0.1f * Math.cos(time.toDouble()).toFloat()),
                    y = height * (0.4f + 0.1f * Math.sin(time.toDouble()).toFloat())
                )
            )
            
            drawCircle(
                color = color2,
                radius = (width * 0.45f) * (1f + 0.1f * Math.cos(time.toDouble() * 1.5).toFloat()),
                center = androidx.compose.ui.geometry.Offset(
                    x = width * (0.6f + 0.1f * Math.sin(time.toDouble() * 1.2).toFloat()),
                    y = height * (0.6f + 0.1f * Math.cos(time.toDouble() * 1.3).toFloat())
                )
            )
            
            drawCircle(
                color = color3,
                radius = (width * 0.4f) * (1f + 0.1f * Math.sin(time.toDouble() * 0.8).toFloat()),
                center = androidx.compose.ui.geometry.Offset(
                    x = width * (0.5f + 0.2f * Math.cos(time.toDouble() * 0.5).toFloat()),
                    y = height * (0.5f + 0.2f * Math.sin(time.toDouble() * 0.7).toFloat())
                )
            )
        }
        
        // Blurred overlay for "diffusion" feel
        Box(modifier = Modifier.fillMaxSize().blur(60.dp).background(MaterialTheme.colorScheme.background.copy(alpha = 0.2f)))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(80.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Icon(
                    Icons.Default.AutoAwesome, 
                    null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Synthesizing visual concepts...",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Step ${((progress * 100).toInt())} of 100",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(title: @Composable () -> Unit, navigationIcon: @Composable () -> Unit = {}) {
    CenterAlignedTopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        modifier = Modifier.blur(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 20.dp else 0.dp)
    )
}

@Composable
fun LibraryScreen(viewModel: MainViewModel, navController: NavHostController) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Assets Library") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            }
        )

        LazyColumn(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            item {
                Text("Educational Assets", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text("Explore infographics and application binaries.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                AssetSectionHeader("Science Resources", "🧪")
                AssetItemCard("Human Digestive System", "High-quality diagram for biological studies.")
                AssetItemCard("Laws of Motion", "Physics fundamentals explained visually.")
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                AssetSectionHeader("Commerce Resources", "📊")
                AssetItemCard("Stock Market Basics", "Financial literacy for beginners.")
                AssetItemCard("Principles of Management", "Core business strategies.")
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                AssetSectionHeader("App Binaries (APK)", "📱")
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ranjeet-alamani/Kortex-AI/releases"))
                        context.startActivity(intent)
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Download Latest APK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Access all versions on GitHub Assets library", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun AssetSectionHeader(title: String, emoji: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Text(emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun AssetItemCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { /* View logic */ }) {
                Icon(Icons.Default.Visibility, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}
