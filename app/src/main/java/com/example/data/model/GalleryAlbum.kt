package com.example.data.model

data class GalleryImage(
    val id: String = "",
    val mediaId: String = id,
    val title: String = "",
    val imageUrl: String = "",
    val url: String = imageUrl,
    val caption: String = ""
) {
    // Resolved image URL whether 'url' or 'imageUrl' is supplied
    fun getResolvedUrl(): String = if (imageUrl.isNotBlank()) imageUrl else url
}

data class GalleryAlbum(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val coverImageUrl: String = "",
    val images: List<GalleryImage> = emptyList()
)
