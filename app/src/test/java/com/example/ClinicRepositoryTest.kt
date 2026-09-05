package com.example

import com.example.data.model.ConsultationRequest
import com.example.data.remote.WordPressApiService
import com.example.data.remote.WordPressMapper
import com.example.data.remote.model.EmbeddedDto
import com.example.data.remote.model.EmbeddedFeaturedMediaDto
import com.example.data.remote.model.RenderedTextDto
import com.example.data.remote.model.WordPressImageCptDto
import com.example.data.remote.model.WordPressImageMetaDto
import com.example.data.remote.model.WordPressMediaDto
import com.example.data.remote.model.WordPressPostDto
import com.example.data.remote.model.WordPressVideoCptDto
import com.example.data.remote.model.WordPressVideoMetaDto
import com.example.data.repository.ClinicRepositoryImpl
import com.example.data.repository.DefaultData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClinicRepositoryTest {

    private class FakeWordPressApiService(
        var postsResult: Result<List<WordPressPostDto>> = Result.success(emptyList()),
        var singlePostResult: (Long) -> Result<WordPressPostDto> = { Result.failure(NoSuchElementException()) },
        var mediaResult: (String?) -> Result<List<WordPressMediaDto>> = { Result.success(emptyList()) },
        var imageAlbumsResult: Result<List<WordPressImageCptDto>> = Result.success(emptyList()),
        var singleImageAlbumResult: (Long) -> Result<WordPressImageCptDto> = { Result.failure(NoSuchElementException()) },
        var videosResult: Result<List<WordPressVideoCptDto>> = Result.success(emptyList()),
        var singleVideoResult: (Long) -> Result<WordPressVideoCptDto> = { Result.failure(NoSuchElementException()) }
    ) : WordPressApiService {

        override suspend fun getPosts(embed: Boolean, perPage: Int, page: Int): List<WordPressPostDto> {
            return postsResult.getOrThrow()
        }

        override suspend fun getPostById(id: Long, embed: Boolean): WordPressPostDto {
            return singlePostResult(id).getOrThrow()
        }

        override suspend fun getImageAlbums(embed: Boolean, perPage: Int, page: Int): List<WordPressImageCptDto> {
            return imageAlbumsResult.getOrThrow()
        }

        override suspend fun getImageAlbumById(id: Long, embed: Boolean): WordPressImageCptDto {
            return singleImageAlbumResult(id).getOrThrow()
        }

        override suspend fun getVideos(embed: Boolean, perPage: Int, page: Int): List<WordPressVideoCptDto> {
            return videosResult.getOrThrow()
        }

        override suspend fun getVideoById(id: Long, embed: Boolean): WordPressVideoCptDto {
            return singleVideoResult(id).getOrThrow()
        }

        override suspend fun getMedia(perPage: Int, page: Int, include: String?): List<WordPressMediaDto> {
            return mediaResult(include).getOrThrow()
        }
    }

    @Test
    fun `unknown article ID does not return another article`() = runTest {
        val fakeApi = FakeWordPressApiService()
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        val result = repository.getArticleById("non_existent_id_999999")
        assertTrue("Expected failure for non-existent article ID", result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun `known article ID from DefaultData returns matching article`() = runTest {
        val fakeApi = FakeWordPressApiService()
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        val targetArticle = DefaultData.articles.first()
        val result = repository.getArticleById(targetArticle.id)

        assertTrue(result.isSuccess)
        assertEquals(targetArticle.id, result.getOrNull()?.id)
        assertEquals(targetArticle.title, result.getOrNull()?.title)
    }

    @Test
    fun `direct getArticleById fetches specific WordPress post`() = runTest {
        val fakeApi = FakeWordPressApiService(
            singlePostResult = { id ->
                if (id == 99L) {
                    Result.success(
                        WordPressPostDto(
                            id = 99L,
                            title = RenderedTextDto(rendered = "مقاله تستی ۹۹"),
                            content = RenderedTextDto(rendered = "<p>متن تستی</p>")
                        )
                    )
                } else {
                    Result.failure(NoSuchElementException())
                }
            }
        )
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        val result = repository.getArticleById("wp_99")
        assertTrue(result.isSuccess)
        val article = result.getOrNull()
        assertNotNull(article)
        assertEquals("wp_99", article!!.id)
        assertEquals("مقاله تستی ۹۹", article.title)
    }

    @Test
    fun `unknown gallery ID does not return another album`() = runTest {
        val fakeApi = FakeWordPressApiService()
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        val result = repository.getGalleryAlbumById("non_existent_album_888")
        assertTrue("Expected failure for non-existent album ID", result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun `failed consultation submission returns failure when Firestore is null`() = runTest {
        val fakeApi = FakeWordPressApiService()
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        val request = ConsultationRequest(
            name = "علی رضایی",
            phoneNumber = "09123456789",
            subject = "مشاوره درد کمر",
            message = "درد دیسک کمر"
        )
        val result = repository.sendConsultationRequest(request)

        assertTrue("Expected failure when consultation database is not configured", result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `CancellationException in getArticles is not swallowed`() = runTest {
        val fakeApi = FakeWordPressApiService(
            postsResult = Result.failure(CancellationException("Coroutine was cancelled"))
        )
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        try {
            repository.getArticles()
            fail("Expected CancellationException to be thrown")
        } catch (e: CancellationException) {
            assertEquals("Coroutine was cancelled", e.message)
        }
    }

    @Test
    fun `CancellationException in getArticleById is not swallowed`() = runTest {
        val fakeApi = FakeWordPressApiService(
            singlePostResult = { throw CancellationException("Cancelled during single post fetch") }
        )
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        try {
            repository.getArticleById("wp_77")
            fail("Expected CancellationException to be thrown")
        } catch (e: CancellationException) {
            assertEquals("Cancelled during single post fetch", e.message)
        }
    }

    @Test
    fun `general IOException falls back to DefaultData gracefully`() = runTest {
        val fakeApi = FakeWordPressApiService(
            postsResult = Result.failure(IOException("No network connection"))
        )
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        val result = repository.getArticles()
        assertTrue(result.isSuccess)
        val articles = result.getOrNull()
        assertNotNull(articles)
        assertFalse(articles!!.isEmpty())
        assertEquals(DefaultData.articles.size, articles.size)
    }

    @Test
    fun `getGalleryAlbums returns WordPress albums and resolves meta gallery IDs`() = runTest {
        val fakeApi = FakeWordPressApiService(
            imageAlbumsResult = Result.success(
                listOf(
                    WordPressImageCptDto(
                        id = 8413L,
                        title = RenderedTextDto(rendered = "تست دیسکوژل"),
                        meta = WordPressImageMetaDto(gallery = listOf("8283", "8285")),
                        embedded = EmbeddedDto(
                            featuredMedia = listOf(
                                EmbeddedFeaturedMediaDto(
                                    sourceUrl = "https://drheidarian.ir/uploads/feat.webp",
                                    altText = "تصویر شاخص"
                                )
                            )
                        )
                    )
                )
            ),
            mediaResult = { include ->
                if (include?.contains("8283") == true) {
                    Result.success(
                        listOf(
                            WordPressMediaDto(
                                id = 8283L,
                                sourceUrl = "https://drheidarian.ir/uploads/8283.webp",
                                mimeType = "image/webp",
                                mediaType = "image",
                                title = RenderedTextDto(rendered = "دیسکوژل ۱")
                            ),
                            WordPressMediaDto(
                                id = 8285L,
                                sourceUrl = "https://drheidarian.ir/uploads/8285.webp",
                                mimeType = "image/webp",
                                mediaType = "image",
                                title = RenderedTextDto(rendered = "دیسکوژل ۲")
                            )
                        )
                    )
                } else {
                    Result.success(emptyList())
                }
            }
        )
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        val result = repository.getGalleryAlbums()
        assertTrue(result.isSuccess)
        val albums = result.getOrNull()
        assertNotNull(albums)
        assertEquals(1, albums!!.size)
        val album = albums.first()
        assertEquals("wp_album_8413", album.id)
        assertEquals("تست دیسکوژل", album.title)
        assertTrue(album.images.size >= 2)
        // Ensure DefaultData albums were NOT merged
        assertFalse(albums.any { it.id == "default_album_1" })
        assertFalse(albums.any { it.id == "wp_live_media" })
    }

    @Test
    fun `getGalleryAlbums filters out empty albums with no images`() = runTest {
        val fakeApi = FakeWordPressApiService(
            imageAlbumsResult = Result.success(
                listOf(
                    WordPressImageCptDto(
                        id = 9991L,
                        title = RenderedTextDto(rendered = "آلبوم خالی بدون تصویر"),
                        meta = WordPressImageMetaDto(gallery = emptyList<String>()),
                        embedded = null
                    )
                )
            ),
            mediaResult = { Result.success(emptyList()) }
        )
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        val result = repository.getGalleryAlbums()
        assertTrue(result.isSuccess)
        val albums = result.getOrNull()
        assertNotNull(albums)
        // Must be empty because album had 0 valid images
        assertTrue("Expected empty list because album had no images", albums!!.isEmpty())
        // Must NOT merge DefaultData
        assertFalse(albums.any { it.id == "default_album_1" })
    }

    @Test
    fun `getGalleryAlbums falls back to DefaultData on network failure`() = runTest {
        val fakeApi = FakeWordPressApiService(
            imageAlbumsResult = Result.failure(IOException("Server error"))
        )
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        val result = repository.getGalleryAlbums()
        assertTrue(result.isSuccess)
        val albums = result.getOrNull()
        assertNotNull(albums)
        assertEquals(DefaultData.galleryAlbums.size, albums!!.size)
    }

    @Test
    fun `getVideos returns only videos with valid aparat-code and filters out empty codes`() = runTest {
        val fakeApi = FakeWordPressApiService(
            videosResult = Result.success(
                listOf(
                    WordPressVideoCptDto(
                        id = 8417L,
                        title = RenderedTextDto(rendered = "ویدیوی بدون آپارات"),
                        meta = WordPressVideoMetaDto(aparatCode = "")
                    ),
                    WordPressVideoCptDto(
                        id = 8418L,
                        title = RenderedTextDto(rendered = "ویدیوی معتبر آپارات"),
                        meta = WordPressVideoMetaDto(aparatCode = "mZ3Kq"),
                        embedded = EmbeddedDto(
                            featuredMedia = listOf(
                                EmbeddedFeaturedMediaDto(
                                    sourceUrl = "https://drheidarian.ir/uploads/thumb.webp"
                                )
                            )
                        )
                    )
                )
            )
        )
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        val result = repository.getVideos()
        assertTrue(result.isSuccess)
        val videos = result.getOrNull()
        assertNotNull(videos)
        // Only the valid video should be returned
        assertEquals(1, videos!!.size)
        val video = videos.first()
        assertEquals("wp_video_8418", video.id)
        assertEquals("ویدیوی معتبر آپارات", video.title)
        assertTrue(video.aparatEmbedCode.contains("mZ3Kq"))
        // Ensure DefaultData videos were NOT merged
        assertFalse(videos.any { it.id == "video_1" })
    }

    @Test
    fun `getVideos does NOT merge DefaultData when WordPress succeeds with empty list`() = runTest {
        val fakeApi = FakeWordPressApiService(
            videosResult = Result.success(
                listOf(
                    WordPressVideoCptDto(
                        id = 8417L,
                        title = RenderedTextDto(rendered = "ویدیوی خالی"),
                        meta = WordPressVideoMetaDto(aparatCode = "")
                    )
                )
            )
        )
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        val result = repository.getVideos()
        assertTrue(result.isSuccess)
        val videos = result.getOrNull()
        assertNotNull(videos)
        // All had empty aparat code, so result is empty list
        assertTrue(videos!!.isEmpty())
        // DefaultData must NOT be merged
        assertFalse(videos.any { it.id == "video_1" })
    }

    @Test
    fun `getVideos falls back to DefaultData on network failure`() = runTest {
        val fakeApi = FakeWordPressApiService(
            videosResult = Result.failure(IOException("Timeout"))
        )
        val repository = ClinicRepositoryImpl(firestore = null, wordpressApi = fakeApi)

        val result = repository.getVideos()
        assertTrue(result.isSuccess)
        val videos = result.getOrNull()
        assertNotNull(videos)
        assertEquals(DefaultData.videos.size, videos!!.size)
    }

    @Test
    fun `WordPressMapper normalizeAparatCode correctly handles all formats`() {
        // Direct hash
        val normalHash = WordPressMapper.normalizeAparatCode("mZ3Kq")
        assertEquals("https://www.aparat.com/video/video/embed/videohash/mZ3Kq/vt/frame", normalHash)

        // Aparat watch URL
        val fromWatchUrl = WordPressMapper.normalizeAparatCode("https://www.aparat.com/v/mZ3Kq")
        assertEquals("https://www.aparat.com/video/video/embed/videohash/mZ3Kq/vt/frame", fromWatchUrl)

        // Iframe code
        val fromIframe = WordPressMapper.normalizeAparatCode("""<iframe src="https://www.aparat.com/video/video/embed/videohash/mZ3Kq/vt/frame" allowFullScreen="true"></iframe>""")
        assertEquals("https://www.aparat.com/video/video/embed/videohash/mZ3Kq/vt/frame", fromIframe)

        // Empty and blank
        assertEquals(null, WordPressMapper.normalizeAparatCode(""))
        assertEquals(null, WordPressMapper.normalizeAparatCode("   "))
        assertEquals(null, WordPressMapper.normalizeAparatCode(null))
    }
}
