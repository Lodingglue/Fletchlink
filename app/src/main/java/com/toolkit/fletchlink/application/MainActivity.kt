package com.toolkit.fletchlink.application

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.toolkit.fletchlink.ui.screens.HomeScreen
import com.toolkit.fletchlink.ui.screens.RealmScreen
import com.toolkit.fletchlink.ui.theme.FletchlinkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FletchlinkTheme {
                SessionCheckScreen()
            }
        }
    }
}

@Composable
fun SessionCheckScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var session by remember { mutableStateOf<StepFullBedrockSession.FullBedrockSession?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val httpClient = MinecraftAuth.createHttpClient()
            val loadedSession = loadSavedSession(context, httpClient)
            if (loadedSession == null || loadedSession.realmsXsts == null) {
                Log.d("MainActivity", "No valid session or missing realmsXsts, redirecting to AuthActivity")
                context.startActivity(AuthActivity.newIntent(context))
                (context as? ComponentActivity)?.finish()
                return@launch
            }
            if (loadedSession.isExpiredOrOutdated()) {
                Log.d("MainActivity", "Session expired or outdated, attempting refresh")
                try {
                    val refreshedSession = BEDROCK_REALMS_AUTH_FLOW.refresh(httpClient, loadedSession)
                    Log.d("MainActivity", "Session refreshed successfully, realmsXsts: ${refreshedSession.realmsXsts != null}")
                    saveSession(context, refreshedSession)
                    session = refreshedSession
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to refresh session: ${e.message}", e)
                    context.startActivity(AuthActivity.newIntent(context))
                    (context as? ComponentActivity)?.finish()
                    return@launch
                }
            } else {
                Log.d("MainActivity", "Valid session loaded: ${loadedSession.mcChain.displayName}")
                session = loadedSession
            }
            isLoading = false
        }
    }

    if (isLoading) {
        LoadingSplashScreen()
    } else if (session != null) {
        MainScreen(session!!)
    }
}

@Composable
private fun LoadingSplashScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Loading FletchLink",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Connecting to your Minecraft account...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MainScreen(session: StepFullBedrockSession.FullBedrockSession) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            RBottomNavigationBar(navController)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(session = session)
            }
            composable("realms") {
                RealmScreen(session = session)
            }
        }
    }
}

@Composable
fun RBottomNavigationBar(navController: androidx.navigation.NavHostController) {
    val items = listOf(
        NavigationItem(
            title = "Home",
            route = "home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        NavigationItem(
            title = "Realms",
            route = "realms",
            selectedIcon = ImageVector.vectorResource(ir.alirezaivaz.tablericons.R.drawable.ic_cube),
            unselectedIcon = ImageVector.vectorResource(ir.alirezaivaz.tablericons.R.drawable.ic_cube)
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    selected = selected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)