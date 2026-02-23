package com.example.composetutorial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.composetutorial.connectivity.ConnectivityStatus
import com.example.composetutorial.data.ItemRepository
import com.example.composetutorial.ui.edit.EditScreen
import com.example.composetutorial.ui.edit.EditViewModel
import com.example.composetutorial.ui.edit.EditViewModelFactory
import com.example.composetutorial.ui.list.ListScreen
import com.example.composetutorial.ui.list.ListViewModel
import com.example.composetutorial.ui.list.ListViewModelFactory
import com.example.composetutorial.ui.login.LoginScreen
import com.example.composetutorial.ui.login.LoginViewModel
import com.example.composetutorial.ui.login.LoginViewModelFactory
import com.example.composetutorial.ui.map.MapScreen
import com.example.composetutorial.ui.sensor.SensorScreen
import com.example.composetutorial.ui.theme.ComposeTutorialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request Notification Permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) !=
                            android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        // Get dependencies from Application Container
        val app = application as ComposeTutorialApplication
        val repository = app.container.repository
        val userPrefs = app.container.userPrefs

        // Network Observation
        val connectivityObserver = app.container.connectivityObserver

        setContent {
            ComposeTutorialTheme {
                // Collect network status
                val status =
                        connectivityObserver
                                .observe()
                                .collectAsState(initial = ConnectivityStatus.Available)
                                .value

                // --- Safe Token Loading (No ANR) ---
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    userPrefs.authToken.collect { token ->
                        val intent =
                                android.content.Intent(
                                        this@MainActivity,
                                        com.example.composetutorial.service.WebSocketService::class
                                                .java
                                )
                        if (token == null) {
                            startDestination = "login"
                            stopService(intent)
                        } else {
                            // Only set if we were null (first load) or explicitly logging in
                            if (startDestination == null) startDestination = "list"

                            // Start Foreground Service to keep WebSocket alive
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                            ) {
                                startForegroundService(intent)
                            } else {
                                startService(intent)
                            }
                        }
                    }
                }

                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    if (startDestination == null) {
                        // Loading Screen / Splash
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    } else {
                        // Main App
                        Column {
                            // Network Status Banner with animation
                            AnimatedVisibility(
                                    visible = status != ConnectivityStatus.Available,
                                    enter = slideInVertically() + fadeIn(),
                                    exit = slideOutVertically() + fadeOut()
                            ) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .background(MaterialTheme.colorScheme.error)
                                                        .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                            text = "No Internet Connection",
                                            color = MaterialTheme.colorScheme.onError
                                    )
                                }
                            }

                            AppNavigation(repository, startDestination!!)
                        }
                    }
                }
            }
        }
    }
}

// Bottom navigation items
sealed class BottomNavItem(val route: String, val icon: @Composable () -> Unit, val label: String) {
    object Items :
            BottomNavItem(
                    "list",
                    { Icon(Icons.Default.List, contentDescription = "Items") },
                    "Items"
            )
    object Sensors :
            BottomNavItem(
                    "sensors",
                    { Icon(Icons.Default.Star, contentDescription = "Sensors") },
                    "Sensors"
            )
    object Map :
            BottomNavItem("map", { Icon(Icons.Default.Place, contentDescription = "Map") }, "Map")
}

@Composable
fun AppNavigation(repository: ItemRepository, startDestination: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom nav only on main screens (not login, edit)
    val showBottomNav = currentRoute in listOf("list", "sensors", "map")

    Scaffold(
            bottomBar = {
                AnimatedVisibility(
                        visible = showBottomNav,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    NavigationBar {
                        listOf(BottomNavItem.Items, BottomNavItem.Sensors, BottomNavItem.Map)
                                .forEach { item ->
                                    NavigationBarItem(
                                            icon = item.icon,
                                            label = { Text(item.label) },
                                            selected = currentRoute == item.route,
                                            onClick = {
                                                if (currentRoute != item.route) {
                                                    navController.navigate(item.route) {
                                                        popUpTo("list") { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                    )
                                }
                    }
                }
            }
    ) { innerPadding ->
        NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
        ) {
            // Login Screen
            composable("login") {
                val viewModel: LoginViewModel =
                        viewModel(factory = LoginViewModelFactory(repository))
                LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            navController.navigate("list") { popUpTo("login") { inclusive = true } }
                        }
                )
            }

            // List Screen
            composable("list") {
                val viewModel: ListViewModel = viewModel(factory = ListViewModelFactory(repository))
                ListScreen(
                        viewModel = viewModel,
                        onItemClick = { itemId -> navController.navigate("edit/$itemId") },
                        onAddClick = {
                            navController.navigate("edit/0") // 0 means new item
                        },
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                )
            }

            // Edit Screen
            composable(
                    route = "edit/{itemId}",
                    arguments = listOf(navArgument("itemId") { type = NavType.IntType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: 0
                val viewModel: EditViewModel =
                        viewModel(factory = EditViewModelFactory(repository, itemId))
                EditScreen(
                        viewModel = viewModel,
                        onSaveSuccess = { navController.popBackStack() },
                        onCancel = { navController.popBackStack() }
                )
            }

            // Sensors Screen
            composable("sensors") { SensorScreen(onBack = { navController.popBackStack() }) }

            // Map Screen (Step 1: Static map with fixed location)
            composable("map") { MapScreen() }
        }
    }
}
