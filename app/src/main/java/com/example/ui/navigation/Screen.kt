package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    // 5 Main Tabs
    data object Articles : Screen(
        route = "articles",
        title = "مقالات",
        selectedIcon = Icons.Filled.Article,
        unselectedIcon = Icons.Outlined.Article
    )

    data object Gallery : Screen(
        route = "gallery",
        title = "گالری تصاویر",
        selectedIcon = Icons.Filled.PhotoLibrary,
        unselectedIcon = Icons.Outlined.PhotoLibrary
    )

    data object Videos : Screen(
        route = "videos",
        title = "گالری ویدیو",
        selectedIcon = Icons.Filled.VideoLibrary,
        unselectedIcon = Icons.Outlined.VideoLibrary
    )

    data object About : Screen(
        route = "about",
        title = "درباره ما",
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info
    )

    data object Contact : Screen(
        route = "contact",
        title = "تماس و مشاوره",
        selectedIcon = Icons.Filled.ContactPhone,
        unselectedIcon = Icons.Outlined.ContactPhone
    )

    // Sub-screens
    data object ArticleDetail : Screen(
        route = "article_detail/{articleId}",
        title = "جزئیات مقاله"
    ) {
        fun createRoute(articleId: String) = "article_detail/$articleId"
    }

    data object GalleryDetail : Screen(
        route = "gallery_detail/{albumId}",
        title = "تصاویر آلبوم"
    ) {
        fun createRoute(albumId: String) = "gallery_detail/$albumId"
    }

    data object VideoDetail : Screen(
        route = "video_detail/{videoId}",
        title = "پخش ویدیو"
    ) {
        fun createRoute(videoId: String) = "video_detail/$videoId"
    }
}

val bottomNavScreens = listOf(
    Screen.Articles,
    Screen.Gallery,
    Screen.Videos,
    Screen.About,
    Screen.Contact
)
