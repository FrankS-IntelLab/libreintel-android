package com.libreintel.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.libreintel.ui.screens.chat.ChatScreen
import com.libreintel.ui.screens.pdf.PdfViewerScreen
import com.libreintel.ui.screens.settings.SettingsScreen
import com.libreintel.ui.screens.tree.TreeScreen

/**
 * Navigation routes for the app.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Tree : Screen("tree", "Tree", Icons.Default.AccountTree)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Chat : Screen("chat/{nodeId}", "Chat", Icons.Default.Chat) {
        fun createRoute(nodeId: String) = "chat/$nodeId"
    }
    data object PdfViewer : Screen("pdf/{pdfUri}", "PDF", Icons.Default.PictureAsPdf) {
        fun createRoute(pdfUri: String) = "pdf/${java.net.URLEncoder.encode(pdfUri, "UTF-8")}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibreIntelNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Determine if we should show bottom bar
    val showBottomBar = currentDestination?.route?.let { route ->
        route == Screen.Tree.route || route == Screen.Settings.route
    } ?: true
    
    val bottomNavItems = listOf(Screen.Tree, Screen.Settings)
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
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
            startDestination = Screen.Tree.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Tree.route) {
                TreeScreen(
                    onNodeClick = { nodeId ->
                        navController.navigate(Screen.Chat.createRoute(nodeId))
                    },
                    onAddClick = { /* Handled in TreeScreen */ },
                    onExportClick = { /* Handled in TreeScreen */ },
                    onImportClick = { /* Handled in TreeScreen */ }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenPdf = { pdfUri ->
                        navController.navigate(Screen.PdfViewer.createRoute(pdfUri))
                    }
                )
            }
            
            composable(
                route = Screen.Chat.route,
                arguments = listOf(
                    navArgument("nodeId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val nodeId = backStackEntry.arguments?.getString("nodeId") ?: return@composable
                ChatScreen(
                    nodeId = nodeId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.PdfViewer.route,
                arguments = listOf(
                    navArgument("pdfUri") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val pdfUri = backStackEntry.arguments?.getString("pdfUri") ?: return@composable
                val decodedUri = java.net.URLDecoder.decode(pdfUri, "UTF-8")
                PdfViewerScreen(
                    pdfUri = decodedUri,
                    onBack = { navController.popBackStack() },
                    onPushToTree = { text, sourceUrl ->
                        // Navigate to tree and add a new node
                        // For now, pop back to tree - could enhance later
                        navController.popBackStack(Screen.Tree.route, false)
                    }
                )
            }
        }
    }
}