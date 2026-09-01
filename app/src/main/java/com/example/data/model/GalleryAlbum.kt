package com.example.data.model

data class GalleryImage(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val caption: String = ""
)

data class GalleryAlbum(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val coverImageUrl: String = "",
    val images: List<GalleryImage> = emptyList()
)
