package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.ClinicTopBar
import com.example.ui.navigation.Screen
import com.example.ui.navigation.bottomNavScreens
import com.example.ui.screens.about.AboutScreen
import com.example.ui.screens.articles.ArticleDetailScreen
import com.example.ui.screens.articles.ArticlesScreen
import com.example.ui.screens.contact.ContactConsultationScreen
import com.example.ui.screens.gallery.GalleryAlbumsScreen
import com.example.ui.screens.gallery.GalleryGridScreen
import com.example.ui.screens.videos.VideoListScreen
import com.example.ui.screens.videos.VideoPlayerScreen
import com.example.ui.viewmodel.ClinicViewModel

@Composable
fun MainClinicApp(
    viewModel: ClinicViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    // Enforce RTL Layout Direction for Persian language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Determine if bottom bar and main top bar should show
        val isBottomTab = bottomNavScreens.any { it.route == currentRoute }

        Scaffold(
            topBar = {
                if (isBottomTab) {
                    val currentScreen = bottomNavScreens.find { it.route == currentRoute } ?: Screen.Articles
                    ClinicTopBar(
                        title = currentScreen.title,
                        canNavigateBack = false
                    )
                }
            },
            bottomBar = {
                if (isBottomTab) {
                    NavigationBar(
                        modifier = Modifier.testTag("clinic_bottom_nav"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        bottomNavScreens.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                modifier = Modifier.testTag("nav_tab_${screen.route}"),
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    val icon = if (isSelected) screen.selectedIcon else screen.unselectedIcon
                                    if (icon != null) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = screen.title
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Articles.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Tab 1: Articles
                    composable(Screen.Articles.route) {
                        ArticlesScreen(
                            viewModel = viewModel,
                            onArticleClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            }
                        )
                    }

                    // Tab 2: Gallery
                    composable(Screen.Gallery.route) {
                        GalleryAlbumsScreen(
                            viewModel = viewModel,
                            onAlbumClick = { albumId ->
                                navController.navigate(Screen.GalleryDetail.createRoute(albumId))
                            }
                        )
                    }

                    // Tab 3: Videos
                    composable(Screen.Videos.route) {
                        VideoListScreen(
                            viewModel = viewModel,
                            onVideoClick = { videoId ->
                                navController.navigate(Screen.VideoDetail.createRoute(videoId))
                            }
                        )
                    }

                    // Tab 4: About
                    composable(Screen.About.route) {
                        AboutScreen(
                            viewModel = viewModel
                        )
                    }

                    // Tab 5: Contact
                    composable(Screen.Contact.route) {
                        ContactConsultationScreen(
                            viewModel = viewModel
                        )
                    }

                    // Detail Screens
                    composable(
                        route = Screen.ArticleDetail.route,
                        arguments = listOf(navArgument("articleId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
                        ArticleDetailScreen(
                            articleId = articleId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.GalleryDetail.route,
                        arguments = listOf(navArgument("albumId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                        GalleryGridScreen(
                            albumId = albumId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.VideoDetail.route,
                        arguments = listOf(navArgument("videoId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                        VideoPlayerScreen(
                            videoId = videoId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
