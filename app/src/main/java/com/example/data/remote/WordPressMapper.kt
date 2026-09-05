package com.example.data.remote

import androidx.core.text.HtmlCompat
import com.example.data.model.Article
import com.example.data.model.GalleryImage
import com.example.data.remote.model.WordPressMediaDto
import com.example.data.remote.model.WordPressPostDto

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
}
