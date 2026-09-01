package com.example.data.model

data class ClinicVideo(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val aparatEmbedCode: String = "", // Aparat video ID or iframe embed code
    val category: String = "آموزشی",
    val duration: String = "۵ دقیقه"
)
