package com.example.data.remote

import androidx.core.text.HtmlCompat
import com.example.data.model.Article
import com.example.data.model.ClinicVideo
import com.example.data.model.GalleryAlbum
import com.example.data.model.GalleryImage
import com.example.data.remote.model.WordPressImageCptDto
import com.example.data.remote.model.WordPressMediaDto
import com.example.data.remote.model.WordPressPostDto
import com.example.data.remote.model.WordPressVideoCptDto

object WordPressMapper {

    fun cleanHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""

        val preprocessed = html
            .replace("&zwnj;", "\u200C")
            .replace("&#8204;", "\u200C")

        val text = try {
            HtmlCompat.fromHtml(preprocessed, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        } catch (_: Throwable) {
            fallbackCleanHtml(preprocessed)
        }

        return normalizeCleanText(text)
    }

    internal fun fallbackCleanHtml(html: String): String {
        if (html.isBlank()) return ""

        var s = html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<li>", RegexOption.IGNORE_CASE), "• ")
            .replace(Regex("<h[1-6][^>]*>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</h[1-6]>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</tr>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")

        // Decode standard and numeric entities
        s = s
            .replace("&nbsp;", " ")
            .replace("&zwnj;", "\u200C")
            .replace("&#8204;", "\u200C")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#8211;", "–")
            .replace("&#8212;", "—")
            .replace("&#8216;", "‘")
            .replace("&#8217;", "’")
            .replace("&#8220;", "“")
            .replace("&#8221;", "”")
            .replace("&hellip;", "…")

        return s
    }

    internal fun normalizeCleanText(text: String): String {
        if (text.isBlank()) return ""

        return text
            .replace('\u00A0', ' ')
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex(" *\n *"), "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    fun isImageMedia(media: WordPressMediaDto): Boolean {
        val id = media.id
        if (id == null || id <= 0) return false

        val url = media.sourceUrl?.trim().orEmpty()
        if (url.isBlank()) return false
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return false
        }

        val mediaType = media.mediaType?.trim()?.lowercase()
        val mimeType = media.mimeType?.trim()?.lowercase()

        val isExplicitlyNonImage = mediaType in listOf("application", "video", "audio", "text") ||
                mimeType?.startsWith("application/") == true ||
                mimeType?.startsWith("video/") == true ||
                mimeType?.startsWith("audio/") == true

        if (isExplicitlyNonImage) return false

        val isImageType = mediaType == "image"
        val isImageMime = mimeType?.startsWith("image/") == true

        return isImageType || isImageMime
    }

    fun mapWordPressMediaToGalleryImage(media: WordPressMediaDto): GalleryImage? {
        if (!isImageMedia(media)) return null

        val mediaId = media.id?.toString().orEmpty()
        val url = media.sourceUrl?.trim().orEmpty()

        val rawTitle = media.title?.rendered.orEmpty()
        val rawCaption = media.caption?.rendered.orEmpty()
        val rawAlt = media.altText.orEmpty()

        val title = cleanHtml(rawTitle).ifBlank {
            cleanHtml(rawAlt).ifBlank { "تصویر بالینی و درمانی" }
        }
        val caption = cleanHtml(rawCaption).ifBlank {
            cleanHtml(rawAlt)
        }

        return GalleryImage(
            id = "wp_media_$mediaId",
            mediaId = mediaId,
            title = title,
            imageUrl = url,
            url = url,
            caption = caption
        )
    }

    fun calculateReadTimeMinutes(cleanContent: String): Int {
        if (cleanContent.isBlank()) return 1
        val words = cleanContent.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size
        if (wordCount <= 0) return 1
        return maxOf(1, (wordCount + 100) / 200)
    }

    fun mapWordPressPostToArticle(post: WordPressPostDto, fallbackImageUrl: String = ""): Article? {
        val postId = post.id
        if (postId == null || postId <= 0) return null

        val rawTitle = post.title?.rendered.orEmpty()
        val rawContent = post.content?.rendered.orEmpty()
        val rawExcerpt = post.excerpt?.rendered.orEmpty()

        val cleanTitle = cleanHtml(rawTitle).ifBlank { "مقاله تخصصی ستون فقرات و درد" }
        val cleanContent = cleanHtml(rawContent)
        val cleanExcerpt = cleanHtml(rawExcerpt)

        val cleanSummary = if (cleanExcerpt.isNotBlank()) {
            cleanExcerpt
        } else if (cleanContent.isNotBlank()) {
            if (cleanContent.length > 180) {
                cleanContent.take(180).trim().plus("...")
            } else {
                cleanContent
            }
        } else {
            "بدون چکیده"
        }

        val featuredUrl = post.embedded?.featuredMedia
            ?.firstOrNull { !it.sourceUrl.isNullOrBlank() }
            ?.sourceUrl?.trim()

        val finalImageUrl = if (!featuredUrl.isNullOrBlank()) {
            featuredUrl
        } else {
            fallbackImageUrl.trim()
        }

        val category = post.embedded?.terms
            ?.flatten()
            ?.firstOrNull { it.taxonomy == "category" && !it.name.isNullOrBlank() }
            ?.name?.trim()
            ?: "جراحی بسته و درمان درد"

        val rawDate = post.date?.trim()
        val formattedDate = if (!rawDate.isNullOrBlank()) {
            val datePart = rawDate.split("T").firstOrNull()?.replace("-", "/")
            if (!datePart.isNullOrBlank()) datePart else "به‌روزرسانی جدید"
        } else {
            "به‌روزرسانی جدید"
        }

        return Article(
            id = "wp_$postId",
            title = cleanTitle,
            summary = cleanSummary,
            content = if (cleanContent.isNotBlank()) cleanContent else cleanSummary,
            imageUrl = finalImageUrl,
            category = category,
            readTimeMinutes = calculateReadTimeMinutes(cleanContent),
            date = formattedDate,
            author = "دکتر مجید حیدریان",
            tags = listOf("دکتر حیدریان", "کلینیک درد ماهان", "ستون فقرات")
        )
    }

    fun extractMediaIds(galleryValue: Any?): List<Long> {
        return when (galleryValue) {
            is List<*> -> galleryValue.mapNotNull { item ->
                when (item) {
                    is Number -> item.toLong()
                    is String -> item.trim().toLongOrNull()
                    else -> null
                }
            }.filter { it > 0 }
            is String -> galleryValue.split(",", " ", ";")
                .mapNotNull { it.trim().toLongOrNull() }
                .filter { it > 0 }
            is Number -> if (galleryValue.toLong() > 0) listOf(galleryValue.toLong()) else emptyList()
            else -> emptyList()
        }
    }

    fun normalizeAparatCode(rawInput: String?): String? {
        if (rawInput.isNullOrBlank()) return null
        val trimmed = rawInput.trim()

        // 1. If it contains an iframe: extract src attribute
        if (trimmed.contains("<iframe", ignoreCase = true)) {
            val srcMatch = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(trimmed)
            val src = srcMatch?.groupValues?.get(1)?.trim()
            if (!src.isNullOrBlank()) {
                return normalizeAparatCode(src)
            }
        }

        // 2. If it contains script or HTML tags, extract URL or video hash
        if (trimmed.contains("<script", ignoreCase = true) || trimmed.contains("<")) {
            val urlMatch = Regex("""https?://[^\s"'>]+""").find(trimmed)
            if (urlMatch != null) {
                return normalizeAparatCode(urlMatch.value)
            }
            val hashMatch = Regex("""\b([a-zA-Z0-9_-]{4,15})\b""").find(trimmed)
            if (hashMatch != null) {
                return "https://www.aparat.com/video/video/embed/videohash/${hashMatch.groupValues[1]}/vt/frame"
            }
            return null
        }

        // 3. If it's already an embed URL: https://www.aparat.com/video/video/embed/videohash/xxxx/vt/frame
        val embedRegex = Regex("""(?:https?:)?//(?:www\.)?aparat\.com/video/video/embed/videohash/([a-zA-Z0-9_-]+)""", RegexOption.IGNORE_CASE)
        val embedMatch = embedRegex.find(trimmed)
        if (embedMatch != null) {
            val hash = embedMatch.groupValues[1]
            return "https://www.aparat.com/video/video/embed/videohash/$hash/vt/frame"
        }

        // 4. If it's a standard Aparat watch URL: https://www.aparat.com/v/xxxx
        val watchRegex = Regex("""(?:https?:)?//(?:www\.)?aparat\.com/v/([a-zA-Z0-9_-]+)""", RegexOption.IGNORE_CASE)
        val watchMatch = watchRegex.find(trimmed)
        if (watchMatch != null) {
            val hash = watchMatch.groupValues[1]
            return "https://www.aparat.com/video/video/embed/videohash/$hash/vt/frame"
        }

        // 5. Alternate Aparat embed URL: https://www.aparat.com/embed/xxxx
        val altEmbedRegex = Regex("""(?:https?:)?//(?:www\.)?aparat\.com/embed/([a-zA-Z0-9_-]+)""", RegexOption.IGNORE_CASE)
        val altEmbedMatch = altEmbedRegex.find(trimmed)
        if (altEmbedMatch != null) {
            val hash = altEmbedMatch.groupValues[1]
            return "https://www.aparat.com/video/video/embed/videohash/$hash/vt/frame"
        }

        // 6. Direct video hash code (4 to 15 alphanumeric/dash/underscore chars)
        val cleanHashRegex = Regex("""^[a-zA-Z0-9_-]{4,15}$""")
        if (cleanHashRegex.matches(trimmed)) {
            return "https://www.aparat.com/video/video/embed/videohash/$trimmed/vt/frame"
        }

        // 7. Generic valid HTTP/HTTPS URL
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }

        return null
    }

    fun mapWordPressImageCptToGalleryAlbum(
        post: WordPressImageCptDto,
        resolvedMediaMap: Map<Long, GalleryImage> = emptyMap()
    ): GalleryAlbum? {
        val postId = post.id
        if (postId == null || postId <= 0) return null

        val rawTitle = post.title?.rendered.orEmpty()
        val rawContent = post.content?.rendered.orEmpty()
        val rawExcerpt = post.excerpt?.rendered.orEmpty()

        val cleanTitle = cleanHtml(rawTitle).ifBlank { "آلبوم تصاویر کلینیک" }
        val cleanContent = cleanHtml(rawContent)
        val cleanExcerpt = cleanHtml(rawExcerpt)
        val cleanDescription = cleanExcerpt.ifBlank { cleanContent }.ifBlank {
            "مجموعه تصاویر بالینی و درمانی کلینیک درد ماهان"
        }

        // Collect images from meta.gallery
        val mediaIds = extractMediaIds(post.meta?.gallery)
        val albumImages = mutableListOf<GalleryImage>()

        for (id in mediaIds) {
            val img = resolvedMediaMap[id]
            if (img != null && img.resolvedUrl.isNotBlank()) {
                albumImages.add(img)
            }
        }

        // Check featured media from _embedded
        val featuredUrl = post.embedded?.featuredMedia
            ?.firstOrNull { !it.sourceUrl.isNullOrBlank() }
            ?.sourceUrl?.trim().orEmpty()

        if (featuredUrl.isNotBlank()) {
            val alreadyPresent = albumImages.any { it.resolvedUrl == featuredUrl }
            if (!alreadyPresent) {
                val featMediaId = post.featuredMediaId?.toString().orEmpty()
                val featAlt = post.embedded?.featuredMedia?.firstOrNull()?.altText.orEmpty()
                val featTitle = cleanHtml(featAlt).ifBlank { cleanTitle }
                albumImages.add(
                    0,
                    GalleryImage(
                        id = "wp_media_${featMediaId.ifBlank { "cover_${postId}" }}",
                        mediaId = featMediaId,
                        title = featTitle,
                        imageUrl = featuredUrl,
                        url = featuredUrl,
                        caption = cleanDescription
                    )
                )
            }
        }

        // CRITICAL: An album must have at least one valid image
        if (albumImages.isEmpty()) {
            return null
        }

        val coverImageUrl = if (featuredUrl.isNotBlank()) {
            featuredUrl
        } else {
            albumImages.first().resolvedUrl
        }

        return GalleryAlbum(
            id = "wp_album_$postId",
            title = cleanTitle,
            description = cleanDescription,
            coverImageUrl = coverImageUrl,
            images = albumImages
        )
    }

    fun mapWordPressVideoToClinicVideo(post: WordPressVideoCptDto): ClinicVideo? {
        val postId = post.id
        if (postId == null || postId <= 0) return null

        val rawAparat = post.meta?.aparatCode?.toString()
        val normalizedAparat = normalizeAparatCode(rawAparat)
        // CRITICAL: Only include videos that have a valid aparat-code
        if (normalizedAparat.isNullOrBlank()) {
            return null
        }

        val rawTitle = post.title?.rendered.orEmpty()
        val rawContent = post.content?.rendered.orEmpty()
        val rawExcerpt = post.excerpt?.rendered.orEmpty()

        val cleanTitle = cleanHtml(rawTitle).ifBlank { "ویدیوی آموزشی کلینیک درد ماهان" }
        val cleanContent = cleanHtml(rawContent)
        val cleanExcerpt = cleanHtml(rawExcerpt)
        val cleanDescription = cleanExcerpt.ifBlank { cleanContent }.ifBlank {
            "توضیحات ویدیوی آموزشی دکتر مجید حیدریان"
        }

        val thumbnailUrl = post.embedded?.featuredMedia
            ?.firstOrNull { !it.sourceUrl.isNullOrBlank() }
            ?.sourceUrl?.trim().orEmpty()

        return ClinicVideo(
            id = "wp_video_$postId",
            title = cleanTitle,
            description = cleanDescription,
            thumbnailUrl = thumbnailUrl,
            aparatEmbedCode = normalizedAparat,
            category = "آموزشی",
            duration = "ویدیو آموزشی"
        )
    }
}
