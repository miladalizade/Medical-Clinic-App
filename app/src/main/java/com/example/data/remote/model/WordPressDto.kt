package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RenderedTextDto(
    @Json(name = "rendered") val rendered: String? = null
)

@JsonClass(generateAdapter = true)
data class MediaDetailsDto(
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null,
    @Json(name = "file") val file: String? = null
)

@JsonClass(generateAdapter = true)
data class WordPressMediaDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "source_url") val sourceUrl: String? = null,
    @Json(name = "title") val title: RenderedTextDto? = null,
    @Json(name = "caption") val caption: RenderedTextDto? = null,
    @Json(name = "alt_text") val altText: String? = null,
    @Json(name = "media_type") val mediaType: String? = null,
    @Json(name = "mime_type") val mimeType: String? = null,
    @Json(name = "media_details") val mediaDetails: MediaDetailsDto? = null
)

@JsonClass(generateAdapter = true)
data class EmbeddedFeaturedMediaDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "source_url") val sourceUrl: String? = null,
    @Json(name = "alt_text") val altText: String? = null
)

@JsonClass(generateAdapter = true)
data class EmbeddedTermDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "taxonomy") val taxonomy: String? = null
)

@JsonClass(generateAdapter = true)
data class EmbeddedDto(
    @Json(name = "wp:featuredmedia") val featuredMedia: List<EmbeddedFeaturedMediaDto>? = null,
    @Json(name = "wp:term") val terms: List<List<EmbeddedTermDto>>? = null
)

@JsonClass(generateAdapter = true)
data class WordPressImageMetaDto(
    @Json(name = "gallery") val gallery: Any? = null
)

@JsonClass(generateAdapter = true)
data class WordPressImageCptDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "title") val title: RenderedTextDto? = null,
    @Json(name = "content") val content: RenderedTextDto? = null,
    @Json(name = "excerpt") val excerpt: RenderedTextDto? = null,
    @Json(name = "featured_media") val featuredMediaId: Long? = null,
    @Json(name = "meta") val meta: WordPressImageMetaDto? = null,
    @Json(name = "_embedded") val embedded: EmbeddedDto? = null
)

@JsonClass(generateAdapter = true)
data class WordPressVideoMetaDto(
    @Json(name = "aparat-code") val aparatCode: Any? = null
)

@JsonClass(generateAdapter = true)
data class WordPressVideoCptDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "title") val title: RenderedTextDto? = null,
    @Json(name = "content") val content: RenderedTextDto? = null,
    @Json(name = "excerpt") val excerpt: RenderedTextDto? = null,
    @Json(name = "featured_media") val featuredMediaId: Long? = null,
    @Json(name = "meta") val meta: WordPressVideoMetaDto? = null,
    @Json(name = "_embedded") val embedded: EmbeddedDto? = null
)

@JsonClass(generateAdapter = true)
data class WordPressPostDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "title") val title: RenderedTextDto? = null,
    @Json(name = "content") val content: RenderedTextDto? = null,
    @Json(name = "excerpt") val excerpt: RenderedTextDto? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "featured_media") val featuredMediaId: Long? = null,
    @Json(name = "_embedded") val embedded: EmbeddedDto? = null
)
