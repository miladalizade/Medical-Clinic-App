package com.example

import com.example.data.model.GalleryImage
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryImageTest {

    @Test
    fun `imageUrl only resolves correctly`() {
        val image = GalleryImage(
            id = "img_1",
            imageUrl = "https://example.com/photo.jpg"
        )
        assertEquals("https://example.com/photo.jpg", image.resolvedUrl)
        assertEquals("img_1", image.resolvedMediaId)
    }

    @Test
    fun `url only resolves correctly`() {
        val image = GalleryImage(
            id = "img_2",
            url = "https://example.com/legacy.jpg"
        )
        assertEquals("https://example.com/legacy.jpg", image.resolvedUrl)
        assertEquals("img_2", image.resolvedMediaId)
    }

    @Test
    fun `both provided gives precedence to imageUrl`() {
        val image = GalleryImage(
            id = "img_3",
            imageUrl = "https://example.com/primary.jpg",
            url = "https://example.com/secondary.jpg"
        )
        assertEquals("https://example.com/primary.jpg", image.resolvedUrl)
    }

    @Test
    fun `both blank resolves to empty string`() {
        val image = GalleryImage(
            id = "img_4",
            imageUrl = "",
            url = ""
        )
        assertEquals("", image.resolvedUrl)
    }

    @Test
    fun `whitespace values trimmed and falls back safely`() {
        val image = GalleryImage(
            id = "img_5",
            imageUrl = "   ",
            url = "  https://example.com/trimmed.jpg  "
        )
        assertEquals("https://example.com/trimmed.jpg", image.resolvedUrl)
    }

    @Test
    fun `mediaId fallback to id when mediaId is blank`() {
        val imageWithId = GalleryImage(
            id = "12345",
            mediaId = "  "
        )
        assertEquals("12345", imageWithId.resolvedMediaId)

        val imageWithCustomMediaId = GalleryImage(
            id = "item_1",
            mediaId = "media_999"
        )
        assertEquals("media_999", imageWithCustomMediaId.resolvedMediaId)
    }
}
