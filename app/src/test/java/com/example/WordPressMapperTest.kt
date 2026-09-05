package com.example

import com.example.data.remote.WordPressMapper
import com.example.data.remote.model.EmbeddedDto
import com.example.data.remote.model.EmbeddedFeaturedMediaDto
import com.example.data.remote.model.EmbeddedTermDto
import com.example.data.remote.model.RenderedTextDto
import com.example.data.remote.model.WordPressMediaDto
import com.example.data.remote.model.WordPressPostDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WordPressMapperTest {

    @Test
    fun `map normal post with full details`() {
        val post = WordPressPostDto(
            id = 456L,
            date = "2026-08-15T10:30:00",
            title = RenderedTextDto(rendered = "درمان بسته دیسک کمر"),
            content = RenderedTextDto(rendered = "<p>توضیحات کامل درباره عمل لیزر دیسک.</p>"),
            excerpt = RenderedTextDto(rendered = "<p>خلاصه روش لیزری.</p>"),
            embedded = EmbeddedDto(
                featuredMedia = listOf(
                    EmbeddedFeaturedMediaDto(id = 10L, sourceUrl = "https://drheidarian.ir/uploads/laser.jpg")
                ),
                terms = listOf(
                    listOf(
                        EmbeddedTermDto(id = 1L, name = "دیسک کمر", taxonomy = "category")
                    )
                )
            )
        )

        val article = WordPressMapper.mapWordPressPostToArticle(post, "https://default.com/img.jpg")
        assertNotNull(article)
        assertEquals("wp_456", article!!.id)
        assertEquals("درمان بسته دیسک کمر", article.title)
        assertEquals("خلاصه روش لیزری.", article.summary)
        assertEquals("توضیحات کامل درباره عمل لیزر دیسک.", article.content)
        assertEquals("https://drheidarian.ir/uploads/laser.jpg", article.imageUrl)
        assertEquals("دیسک کمر", article.category)
        assertEquals("2026/08/15", article.date)
        assertTrue(article.readTimeMinutes >= 1)
    }

    @Test
    fun `map post with missing title uses default title`() {
        val post = WordPressPostDto(
            id = 789L,
            title = null,
            content = RenderedTextDto(rendered = "محتوای مقاله")
        )

        val article = WordPressMapper.mapWordPressPostToArticle(post)
        assertNotNull(article)
        assertEquals("مقاله تخصصی ستون فقرات و درد", article!!.title)
    }

    @Test
    fun `map post with empty excerpt derives from content without trailing dots if short`() {
        val post = WordPressPostDto(
            id = 101L,
            content = RenderedTextDto(rendered = "<p>متن کوتاه مقاله بدون خلاصه مجزا</p>"),
            excerpt = null
        )

        val article = WordPressMapper.mapWordPressPostToArticle(post)
        assertNotNull(article)
        assertEquals("متن کوتاه مقاله بدون خلاصه مجزا", article!!.summary)
        assertFalse(article.summary.endsWith("..."))
    }

    @Test
    fun `map post with both empty excerpt and content does not produce dots`() {
        val post = WordPressPostDto(
            id = 102L,
            content = null,
            excerpt = null
        )

        val article = WordPressMapper.mapWordPressPostToArticle(post)
        assertNotNull(article)
        assertEquals("بدون چکیده", article!!.summary)
        assertFalse(article.summary.contains("..."))
    }

    @Test
    fun `map post with no featured media falls back safely`() {
        val post = WordPressPostDto(
            id = 103L,
            title = RenderedTextDto(rendered = "عنوان"),
            embedded = null
        )

        val article = WordPressMapper.mapWordPressPostToArticle(post, fallbackImageUrl = "https://fallback.com/img.jpg")
        assertNotNull(article)
        assertEquals("https://fallback.com/img.jpg", article!!.imageUrl)
        assertEquals("جراحی بسته و درمان درد", article.category)
    }

    @Test
    fun `map post with null or zero ID returns null`() {
        val nullIdPost = WordPressPostDto(id = null)
        assertNull(WordPressMapper.mapWordPressPostToArticle(nullIdPost))

        val zeroIdPost = WordPressPostDto(id = 0L)
        assertNull(WordPressMapper.mapWordPressPostToArticle(zeroIdPost))

        val negativeIdPost = WordPressPostDto(id = -5L)
        assertNull(WordPressMapper.mapWordPressPostToArticle(negativeIdPost))
    }

    @Test
    fun `media filtering accepts jpeg and png`() {
        val jpegMedia = WordPressMediaDto(
            id = 201L,
            sourceUrl = "https://drheidarian.ir/img.jpg",
            mediaType = "image",
            mimeType = "image/jpeg"
        )
        assertTrue(WordPressMapper.isImageMedia(jpegMedia))
        assertNotNull(WordPressMapper.mapWordPressMediaToGalleryImage(jpegMedia))

        val pngMedia = WordPressMediaDto(
            id = 202L,
            sourceUrl = "https://drheidarian.ir/diagram.png",
            mediaType = "image",
            mimeType = "image/png"
        )
        assertTrue(WordPressMapper.isImageMedia(pngMedia))
        assertNotNull(WordPressMapper.mapWordPressMediaToGalleryImage(pngMedia))
    }

    @Test
    fun `media filtering rejects pdf video and audio`() {
        val pdfMedia = WordPressMediaDto(
            id = 203L,
            sourceUrl = "https://drheidarian.ir/manual.pdf",
            mediaType = "application",
            mimeType = "application/pdf"
        )
        assertFalse(WordPressMapper.isImageMedia(pdfMedia))
        assertNull(WordPressMapper.mapWordPressMediaToGalleryImage(pdfMedia))

        val videoMedia = WordPressMediaDto(
            id = 204L,
            sourceUrl = "https://drheidarian.ir/video.mp4",
            mediaType = "video",
            mimeType = "video/mp4"
        )
        assertFalse(WordPressMapper.isImageMedia(videoMedia))
        assertNull(WordPressMapper.mapWordPressMediaToGalleryImage(videoMedia))

        val audioMedia = WordPressMediaDto(
            id = 205L,
            sourceUrl = "https://drheidarian.ir/podcast.mp3",
            mediaType = "audio",
            mimeType = "audio/mpeg"
        )
        assertFalse(WordPressMapper.isImageMedia(audioMedia))
        assertNull(WordPressMapper.mapWordPressMediaToGalleryImage(audioMedia))
    }

    @Test
    fun `media filtering rejects blank or invalid URLs`() {
        val blankUrl = WordPressMediaDto(
            id = 206L,
            sourceUrl = "   ",
            mediaType = "image",
            mimeType = "image/jpeg"
        )
        assertFalse(WordPressMapper.isImageMedia(blankUrl))

        val nonHttpUrl = WordPressMediaDto(
            id = 207L,
            sourceUrl = "ftp://drheidarian.ir/file.jpg",
            mediaType = "image",
            mimeType = "image/jpeg"
        )
        assertFalse(WordPressMapper.isImageMedia(nonHttpUrl))
    }

    @Test
    fun `cleanHtml handles paragraphs line breaks headings and lists`() {
        val html = "<h2>عنوان بخش</h2><p>پاراگراف اول<br/>سطر دوم</p><ul><li>مورد ۱</li><li>مورد ۲</li></ul>"
        val cleaned = WordPressMapper.cleanHtml(html)

        assertTrue(cleaned.contains("عنوان بخش"))
        assertTrue(cleaned.contains("پاراگراف اول"))
        assertTrue(cleaned.contains("سطر دوم"))
        assertTrue(cleaned.contains("• مورد ۱") || cleaned.contains("مورد ۱"))
        assertFalse(cleaned.contains("<"))
        assertFalse(cleaned.contains(">"))
    }

    @Test
    fun `cleanHtml decodes HTML entities and preserves Persian ZWNJ`() {
        val html = "می&zwnj;خواهم &quot;درمان&quot; انجام دهم &amp; هزینه&#8211;ها &nbsp;را بدانم."
        val cleaned = WordPressMapper.cleanHtml(html)

        // ZWNJ \u200C must be preserved
        assertTrue(cleaned.contains("می\u200Cخواهم"))
        assertTrue(cleaned.contains("\"درمان\""))
        assertTrue(cleaned.contains("&"))
        assertTrue(cleaned.contains("–"))
        assertFalse(cleaned.contains("&quot;"))
        assertFalse(cleaned.contains("&amp;"))
        assertFalse(cleaned.contains("&zwnj;"))
    }

    @Test
    fun `cleanHtml normalizes excessive blank lines`() {
        val html = "<p>سطر اول</p><br><br><br><br><p>سطر دوم</p>"
        val cleaned = WordPressMapper.cleanHtml(html)
        assertFalse(cleaned.contains("\n\n\n"))
    }

    @Test
    fun `calculateReadTimeMinutes provides reasonable word-based estimates`() {
        assertEquals(1, WordPressMapper.calculateReadTimeMinutes(""))
        assertEquals(1, WordPressMapper.calculateReadTimeMinutes("چند کلمه کوتاه"))

        // 600 words -> ~3-4 minutes
        val longContent = (1..600).joinToString(" ") { "کلمه" }
        val readTime = WordPressMapper.calculateReadTimeMinutes(longContent)
        assertTrue(readTime in 3..4)
    }
}
