package com.example.data.model

data class GalleryImage(
    val id: String = "",
    val mediaId: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val url: String = "",
    val caption: String = ""
) {
    val resolvedUrl: String
        get() = imageUrl.trim().ifBlank { url.trim() }

    val resolvedMediaId: String
        get() = mediaId.trim().ifBlank { id.trim() }
}

data class GalleryAlbum(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val coverImageUrl: String = "",
    val images: List<GalleryImage> = emptyList()
)

