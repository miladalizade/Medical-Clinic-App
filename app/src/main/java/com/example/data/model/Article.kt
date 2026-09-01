package com.example.data.model

data class Article(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val category: String = "عمومی",
    val readTimeMinutes: Int = 5,
    val date: String = "",
    val author: String = "دکتر کلینیک",
    val tags: List<String> = emptyList()
)
