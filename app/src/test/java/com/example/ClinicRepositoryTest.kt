package com.example

import com.example.data.model.ConsultationRequest
import com.example.data.remote.WordPressApiService
import com.example.data.remote.model.RenderedTextDto
import com.example.data.remote.model.WordPressMediaDto
import com.example.data.remote.model.WordPressPostDto
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
        var mediaResult: Result<List<WordPressMediaDto>> = Result.success(emptyList())
    ) : WordPressApiService {

        override suspend fun getPosts(embed: Boolean, perPage: Int, page: Int): List<WordPressPostDto> {
            return postsResult.getOrThrow()
        }

        override suspend fun getPostById(id: Long, embed: Boolean): WordPressPostDto {
            return singlePostResult(id).getOrThrow()
        }

        override suspend fun getMedia(perPage: Int, page: Int): List<WordPressMediaDto> {
            return mediaResult.getOrThrow()
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
}
