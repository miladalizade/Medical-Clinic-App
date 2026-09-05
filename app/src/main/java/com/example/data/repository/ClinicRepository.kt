package com.example.data.repository

import android.util.Log
import com.example.data.model.Article
import com.example.data.model.ClinicInfo
import com.example.data.model.ClinicVideo
import com.example.data.model.ConsultationRequest
import com.example.data.model.GalleryAlbum
import com.example.data.model.GalleryImage
import com.example.data.remote.WordPressApiService
import com.example.data.remote.WordPressClient
import com.example.data.remote.WordPressMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface ClinicRepository {
    suspend fun getArticles(): Result<List<Article>>
    suspend fun getArticleById(id: String): Result<Article>
    suspend fun getGalleryAlbums(): Result<List<GalleryAlbum>>
    suspend fun getGalleryAlbumById(id: String): Result<GalleryAlbum>
    suspend fun getVideos(): Result<List<ClinicVideo>>
    suspend fun getVideoById(id: String): Result<ClinicVideo>
    suspend fun getClinicInfo(): ClinicInfo
    suspend fun sendConsultationRequest(request: ConsultationRequest): Result<String>
}

class ClinicRepositoryImpl(
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null },
    private val wordpressApi: WordPressApiService = WordPressClient.apiService
) : ClinicRepository {

    override suspend fun getArticles(): Result<List<Article>> = withContext(Dispatchers.IO) {
        // 1. Try to fetch live posts from drheidarian.ir WordPress REST API
        try {
            val wpPosts = wordpressApi.getPosts(embed = true, perPage = 20)
            if (wpPosts.isNotEmpty()) {
                val fallbackImg = DefaultData.articles.firstOrNull()?.imageUrl.orEmpty()
                val mappedArticles = wpPosts.mapNotNull { post ->
                    WordPressMapper.mapWordPressPostToArticle(post, fallbackImg)
                }
                if (mappedArticles.isNotEmpty()) {
                    return@withContext Result.success(mappedArticles)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ClinicRepository", "WordPress API getPosts failed, falling back", e)
        }

        // 2. Try Firestore if available (ordered by date with fallback)
        try {
            if (firestore != null) {
                val snapshot = try {
                    firestore.collection("articles")
                        .orderBy("date", Query.Direction.DESCENDING)
                        .get()
                        .await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("ClinicRepository", "Ordered Firestore query failed, falling back to unordered", e)
                    firestore.collection("articles").get().await()
                }

                if (!snapshot.isEmpty) {
                    val articles = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Article::class.java)?.copy(id = doc.id)
                    }
                    if (articles.isNotEmpty()) {
                        val sortedArticles = articles.sortedByDescending { it.date }
                        return@withContext Result.success(sortedArticles)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ClinicRepository", "Firestore getArticles failed", e)
        }

        // 3. Fallback to curated Dr. Heidarian clinic data
        Result.success(DefaultData.articles)
    }

    override suspend fun getArticleById(id: String): Result<Article> = withContext(Dispatchers.IO) {
        if (id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("شناسه مقاله نامعتبر است"))
        }

        // 1. If it's a WordPress article ID: "wp_123"
        if (id.startsWith("wp_")) {
            val numericId = id.removePrefix("wp_").toLongOrNull()
            if (numericId != null && numericId > 0) {
                try {
                    val postDto = wordpressApi.getPostById(numericId, embed = true)
                    val fallbackImg = DefaultData.articles.firstOrNull()?.imageUrl.orEmpty()
                    val mapped = WordPressMapper.mapWordPressPostToArticle(postDto, fallbackImg)
                    if (mapped != null) {
                        return@withContext Result.success(mapped)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("ClinicRepository", "Error fetching WordPress post $id by ID", e)
                }
            }
        }

        // 2. Try Firestore if available
        try {
            if (firestore != null) {
                val doc = firestore.collection("articles").document(id).get().await()
                if (doc.exists()) {
                    val article = doc.toObject(Article::class.java)?.copy(id = doc.id)
                    if (article != null) {
                        return@withContext Result.success(article)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ClinicRepository", "Error fetching article $id from Firestore", e)
        }

        // 3. Fallback to local DefaultData (ONLY matching ID)
        val localArticle = DefaultData.articles.find { it.id == id }
        if (localArticle != null) {
            return@withContext Result.success(localArticle)
        }

        Result.failure(NoSuchElementException("مقاله با شناسه $id یافت نشد"))
    }

    override suspend fun getGalleryAlbums(): Result<List<GalleryAlbum>> = withContext(Dispatchers.IO) {
        // 1. Try WordPress CPT "images"
        try {
            val cptPosts = wordpressApi.getImageAlbums(embed = true, perPage = 50)
            // Extract all media IDs to resolve attachment URLs in batch
            val allMediaIds = cptPosts
                .flatMap { WordPressMapper.extractMediaIds(it.meta?.gallery) }
                .distinct()

            val mediaMap: Map<Long, GalleryImage> = if (allMediaIds.isNotEmpty()) {
                try {
                    val mediaList = wordpressApi.getMedia(
                        perPage = 100,
                        include = allMediaIds.joinToString(",")
                    )
                    mediaList.mapNotNull { mediaDto ->
                        val id = mediaDto.id
                        val mapped = WordPressMapper.mapWordPressMediaToGalleryImage(mediaDto)
                        if (id != null && mapped != null) id to mapped else null
                    }.toMap()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("ClinicRepository", "Error resolving gallery attachments via media endpoint", e)
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            val validAlbums = cptPosts.mapNotNull { post ->
                WordPressMapper.mapWordPressImageCptToGalleryAlbum(post, mediaMap)
            }

            // CRITICAL: When WordPress succeeds, return ONLY WordPress albums!
            // Even if empty, do NOT merge or fallback to DefaultData.
            return@withContext Result.success(validAlbums)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ClinicRepository", "WordPress getImageAlbums request failed, falling back", e)
        }

        // 2. Fallback to Firestore if available
        try {
            if (firestore != null) {
                val snapshot = firestore.collection("gallery_albums").get().await()
                if (!snapshot.isEmpty) {
                    val firestoreAlbums = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(GalleryAlbum::class.java)?.copy(id = doc.id)
                    }.filter { it.images.isNotEmpty() }
                    if (firestoreAlbums.isNotEmpty()) {
                        return@withContext Result.success(firestoreAlbums)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ClinicRepository", "Firestore getGalleryAlbums fallback failed", e)
        }

        // 3. Fallback to local DefaultData (ONLY when WordPress failed)
        Result.success(DefaultData.galleryAlbums.filter { it.images.isNotEmpty() })
    }

    override suspend fun getGalleryAlbumById(id: String): Result<GalleryAlbum> = withContext(Dispatchers.IO) {
        if (id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("شناسه آلبوم نامعتبر است"))
        }

        // 1. Try WordPress if it is a WordPress ID ("wp_album_123" or "wp_123" or numeric)
        val numericId = id.removePrefix("wp_album_").removePrefix("wp_").toLongOrNull()
        if (numericId != null && numericId > 0) {
            try {
                val postDto = wordpressApi.getImageAlbumById(numericId, embed = true)
                val mediaIds = WordPressMapper.extractMediaIds(postDto.meta?.gallery)
                val mediaMap: Map<Long, GalleryImage> = if (mediaIds.isNotEmpty()) {
                    try {
                        val mediaList = wordpressApi.getMedia(
                            perPage = 100,
                            include = mediaIds.joinToString(",")
                        )
                        mediaList.mapNotNull { mediaDto ->
                            val mId = mediaDto.id
                            val mapped = WordPressMapper.mapWordPressMediaToGalleryImage(mediaDto)
                            if (mId != null && mapped != null) mId to mapped else null
                        }.toMap()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("ClinicRepository", "Error resolving attachments for album $id", e)
                        emptyMap()
                    }
                } else {
                    emptyMap()
                }

                val album = WordPressMapper.mapWordPressImageCptToGalleryAlbum(postDto, mediaMap)
                if (album != null) {
                    return@withContext Result.success(album)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("ClinicRepository", "Error fetching WordPress image album $id by ID", e)
            }
        }

        // 2. Try Firestore if available
        try {
            if (firestore != null) {
                val doc = firestore.collection("gallery_albums").document(id).get().await()
                if (doc.exists()) {
                    val album = doc.toObject(GalleryAlbum::class.java)?.copy(id = doc.id)
                    if (album != null) {
                        return@withContext Result.success(album)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ClinicRepository", "Error fetching album $id from Firestore", e)
        }

        // 3. Fallback to local DefaultData (ONLY matching ID)
        val localAlbum = DefaultData.galleryAlbums.find { it.id == id }
        if (localAlbum != null) {
            return@withContext Result.success(localAlbum)
        }

        Result.failure(NoSuchElementException("آلبوم با شناسه $id یافت نشد"))
    }

    override suspend fun getVideos(): Result<List<ClinicVideo>> = withContext(Dispatchers.IO) {
        // 1. Try WordPress CPT "videos"
        try {
            val cptVideos = wordpressApi.getVideos(embed = true, perPage = 50)
            // CRITICAL: Only include videos that have a valid, non-blank aparat-code
            val validVideos = cptVideos.mapNotNull { post ->
                WordPressMapper.mapWordPressVideoToClinicVideo(post)
            }

            // CRITICAL: When WordPress succeeds, return ONLY WordPress videos!
            // Even if empty (e.g. none have aparat-code yet), do NOT merge or fallback to DefaultData.
            return@withContext Result.success(validVideos)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ClinicRepository", "WordPress getVideos request failed, falling back", e)
        }

        // 2. Fallback to Firestore if available
        try {
            if (firestore != null) {
                val snapshot = firestore.collection("clinic_videos").get().await()
                if (!snapshot.isEmpty) {
                    val firestoreVideos = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ClinicVideo::class.java)?.copy(id = doc.id)
                    }
                    if (firestoreVideos.isNotEmpty()) {
                        return@withContext Result.success(firestoreVideos)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ClinicRepository", "Firestore getVideos fallback failed", e)
        }

        // 3. Fallback to local DefaultData (ONLY when WordPress failed)
        Result.success(DefaultData.videos)
    }

    override suspend fun getVideoById(id: String): Result<ClinicVideo> = withContext(Dispatchers.IO) {
        if (id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("شناسه ویدیو نامعتبر است"))
        }

        // 1. Try WordPress if it is a WordPress ID ("wp_video_123" or "wp_123" or numeric)
        val numericId = id.removePrefix("wp_video_").removePrefix("wp_").toLongOrNull()
        if (numericId != null && numericId > 0) {
            try {
                val videoDto = wordpressApi.getVideoById(numericId, embed = true)
                val video = WordPressMapper.mapWordPressVideoToClinicVideo(videoDto)
                if (video != null) {
                    return@withContext Result.success(video)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("ClinicRepository", "Error fetching WordPress video $id by ID", e)
            }
        }

        // 2. Try Firestore if available
        try {
            if (firestore != null) {
                val doc = firestore.collection("clinic_videos").document(id).get().await()
                if (doc.exists()) {
                    val video = doc.toObject(ClinicVideo::class.java)?.copy(id = doc.id)
                    if (video != null) {
                        return@withContext Result.success(video)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ClinicRepository", "Firestore getVideoById failed", e)
        }

        // 3. Fallback to local DefaultData (ONLY matching ID)
        val localVideo = DefaultData.videos.find { it.id == id }
        if (localVideo != null) {
            Result.success(localVideo)
        } else {
            Result.failure(NoSuchElementException("ویدیو با شناسه $id یافت نشد"))
        }
    }

    override suspend fun getClinicInfo(): ClinicInfo = withContext(Dispatchers.IO) {
        ClinicInfo()
    }

    override suspend fun sendConsultationRequest(request: ConsultationRequest): Result<String> = withContext(Dispatchers.IO) {
        if (firestore == null) {
            return@withContext Result.failure(
                IllegalStateException("سیستم ثبت مشاوره آنلاین در حال حاضر در دسترس نیست. لطفاً با شماره تلفن کلینیک تماس بگیرید.")
            )
        }

        try {
            val requestId = if (request.id.isBlank()) "DRH-${System.currentTimeMillis().toString().takeLast(6)}" else request.id
            val finalRequest = request.copy(id = requestId, timestamp = System.currentTimeMillis())

            firestore.collection("consultation_requests")
                .document(requestId)
                .set(finalRequest)
                .await()

            Result.success(requestId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ClinicRepository", "Failed to submit consultation request", e)
            Result.failure(e)
        }
    }
}

/**
 * Curated offline / initial sample dataset in Persian
 */
object DefaultData {
    val articles = listOf(
        Article(
            id = "art_1",
            title = "جراحی بسته و لیزری دیسک کمر و گردن؛ مزایا و روند درمان",
            summary = "آشنایی با شیوه درمان کم‌تهاجمی دیسک با لیزر بدون نیاز به بیهوشی عمومی و بدون برش جراحی در کلینیک درد ماهان.",
            content = """
                جراحی بسته دیسک با لیزر (PLDD) یکی از پیشرفته‌ترین و مطمئن‌ترین روش‌های درمان بیرون‌زدگی دیسک ستون فقرات است. در این روش با هدایت اشعه ایکس زنده (فلوروسکوپی) و بی‌حسی موضعی، یک سوزن بسیار نازک وارد فضای دیسک شده و انرژی لیزر با تبخیر بخش کوچکی از هسته دیسک، فشار را از روی ریشه عصب سیاتیک یا نخاع برمی‌دارد.

                مزایای جراحی لیزری دیسک:
                • بدون نیاز به بیهوشی عمومی و بستری طولانی (عمل سرپایی)
                • بدون ایجاد برش پوستی و بدون خونریزی
                • عدم دستکاری به استخوان مهره یا لیگامان‌ها و حفظ ساختار طبیعی ستون فقرات
                • بازگشت بسیار سریع بیمار به فعالیت‌های شغلی و روزمره (معمولاً ظرف ۳ تا ۵ روز)

                کاندیداهای مناسب:
                بیمارانی که دچار درد تیرکشنده به پاها (سیاتیک) یا دست‌ها هستند و درمان‌های اولیه دارویی یا فیزیوتراپی به بهبود آن‌ها کمکی نکرده است، پس از بررسی دقیق MRI و معاینه تخصصی توسط فوق تخصص درد، می‌توانند از این روش بهره‌مند شوند.
            """.trimIndent(),
            imageUrl = "https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=1200&q=80",
            category = "جراحی بسته دیسک",
            readTimeMinutes = 5,
            date = "۱۴۰۴/۱۱/۲۵",
            author = "دکتر مجید حیدریان",
            tags = listOf("لیزر دیسک", "دیسک کمر", "سیاتیک", "کلینیک درد")
        ),
        Article(
            id = "art_2",
            title = "اوزون تراپی و تزریق دیسکوژل؛ گزینه‌های نوین درمان ستون فقرات",
            summary = "بررسی نحوه عملکرد گاز اوزون طبی و ژل رادیواپک دیسکوژل در ترمیم نوکلئوس دیسک و کاهش سریع التهاب عصب.",
            content = """
                اوزون‌تراپی و دیسکوژل از روش‌های انقلابی طب درد هستند که می‌توانند بدون جراحی باز، بافت دیسک تخریب‌شده را تثبیت و التهاب موضعی را مهار کنند.

                تزریق گاز اوزون (Ozone Therapy):
                گاز اوزون با غلظت پزشکی مشخص خواص ضدالتهابی و تسکین‌دهنده بسیار قوی دارد. اوزون با اکسیداسیون پروتئوگلیکان‌های دیسک سبب جمع شدن فتق دیسک و بهبود اکسیژن‌رسانی و ترمیم ریشه‌های عصبی می‌شود.

                دیسکوژل (DiscoGel):
                دیسکوژل یک مایع ژله‌ای بر پایه الکل خالص و سلولز است که تحت هدایت فلوروسکوپ داخل مرکز دیسک تزریق می‌شود. این ماده پس از چند دقیقه به ساختاری الاستیک و اسفنجی تبدیل شده و سبب انسداد شکاف‌های فیبری دیسک و عقب‌نشینی برآمدگی دیسک می‌گردد.
            """.trimIndent(),
            imageUrl = "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&w=1200&q=80",
            category = "درمان‌های نوین درد",
            readTimeMinutes = 4,
            date = "۱۴۰۴/۱۱/۱۸",
            author = "دکتر مجید حیدریان",
            tags = listOf("اوزون تراپی", "دیسکوژل", "درمان بدون جراحی", "ستون فقرات")
        ),
        Article(
            id = "art_3",
            title = "رادیوفرکوئنسی (RF) عصب؛ پایان دردهای مفصلی و سردردهای مقاوم",
            summary = "چگونه با فرکانس‌های رادیویی امواج درد ناشی از آرتروز ستون فقرات، مفاصل فاست و دردهای مزمن را متوقف کنیم؟",
            content = """
                روش رادیوفرکوئنسی (RF Neurotomy / Pulsed RF) از امواج با فرکانس بالا برای قطع انتقال پیام‌های درد از مفاصل آسیب‌دیده به مغز استفاده می‌کند.

                کاربردهای اصلی رادیوفرکوئنسی:
                • دردهای ناشی از آرتروز مفاصل فاست گردن و کمر
                • دردهای مفصل خاجی-خاصره‌ای (ساکروایلیاک)
                • عصب سه‌قلو (نورالژی تری ژمینال) و سردردهای با منشأ گردنی
                • درد مزمن زانو ناشی از آرتروز شدید با فرسایش اعصاب ژنیکولار (Genicular Nerves)

                این روش با بی‌حسی موضعی انجام شده و اثرات تسکینی آن معمولاً بین ۶ ماه تا ۲ سال یا بیشتر پایدار باقی می‌ماند.
            """.trimIndent(),
            imageUrl = "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=1200&q=80",
            category = "رادیوفرکوئنسی و اینترونشن",
            readTimeMinutes = 6,
            date = "۱۴۰۴/۱۱/۱۲",
            author = "دکتر مجید حیدریان",
            tags = listOf("رادیوفرکوئنسی", "آرتروز", "بلاک عصب", "کنترل درد")
        ),
        Article(
            id = "art_4",
            title = "روش‌های نوین درمان آرتروز زانو با تزریق پی‌آر‌پی (PRP) و اسید هیالورونیک",
            summary = "ترمیم و بازسازی بافت غضروفی با پلاسمای غنی از پلاکت و ژل‌های غضروف‌ساز تحت هدایت سونوگرافی دقیق.",
            content = """
                آرتروز زانو با از بین رفتن تدریجی غضروف و درد هنگام راه رفتن همراه است. در کلینیک درد ماهان، با رویکردهای نوین بازساختی (Regenerative Medicine) می‌توان از پیشرفت تخریب مفصل جلوگیری کرد.

                پی‌آر‌پی (PRP):
                در این تکنیک، مقداری از خون خود بیمار گرفته شده و با سانتریفیوژ تخصصی، پلاکت‌ها با غلظت ۵ تا ۸ برابر جداسازی می‌شوند. فاکتورهای رشد موجود در پلاکت‌ها فاکتورهای ترمیمی قوی آزاد کرده و به کاهش چشمگیر درد و تورم زانو کمک می‌نمایند.

                تزریق هیالورونات (ژل غضروف‌ساز):
                تزریق هیالورونیک اسید با روان‌سازی حرکت مفصل و جذب ضربات، اصطکاک را کاهش داده و کیفیت زندگی بیمار را بهبود می‌بخشد.
            """.trimIndent(),
            imageUrl = "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?auto=format&fit=crop&w=1200&q=80",
            category = "درمان مفاصل و زانو",
            readTimeMinutes = 5,
            date = "۱۴۰۴/۱۱/۰۵",
            author = "دکتر مجید حیدریان",
            tags = listOf("PRP", "آرتروز زانو", "پی آر پی", "تزریق مفصل")
        )
    )

    val galleryAlbums = listOf(
        GalleryAlbum(
            id = "alb_1",
            title = "جراحی‌های بسته و لیزری دیسک ستون فقرات",
            description = "تصاویر و مستندات مراحل انجام جراحی کم‌تهاجمی لیزر دیسک و دیسکوژل در اتاق عمل کلینیک درد ماهان.",
            coverImageUrl = "https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=800&q=80",
            images = listOf(
                GalleryImage(
                    id = "media_101",
                    title = "انجام لیزر دیسک تحت فلوروسکوپی",
                    imageUrl = "https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=1200&q=80",
                    caption = "دسترسی دقیق به فضای دیسک با سوزن مخصوص و تابش هدایت‌شده فیبر نوری لیزر."
                ),
                GalleryImage(
                    id = "media_102",
                    title = "تزریق دیسکوژل در دیسک گردنی",
                    imageUrl = "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&w=1200&q=80",
                    caption = "ترمیم فتق دیسک گردن بدون برش با کنترل دیجیتالی تصویربرداری زنده."
                ),
                GalleryImage(
                    id = "media_103",
                    title = "بلاک ریشه عصب ترانس فورامینال",
                    imageUrl = "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?auto=format&fit=crop&w=1200&q=80",
                    caption = "تزریق ضدالتهاب به محل درگیری عصب سیاتیک جهت تسکین فوری درد حاد."
                ),
                GalleryImage(
                    id = "media_104",
                    title = "رادیوفرکوئنسی مفاصل فاست ستون فقرات",
                    imageUrl = "https://images.unsplash.com/photo-1516549655169-df83a0774514?auto=format&fit=crop&w=1200&q=80",
                    caption = "قطع امواج درد ناشی از آرتروز مهره‌ها با الکترودهای سوزنی RF."
                )
            )
        ),
        GalleryAlbum(
            id = "alb_2",
            title = "محیط و امکانات کلینیک فوق تخصصی درد ماهان",
            description = "فضای پذیرش، اتاق عمل اختصاصی مجهز به C-Arm، کلینیک طب فیزیکی و سالن‌های درمان.",
            coverImageUrl = "https://images.unsplash.com/photo-1586773860418-d37222d8fce3?auto=format&fit=crop&w=800&q=80",
            images = listOf(
                GalleryImage(
                    id = "media_201",
                    title = "اتاق عمل اینترونشنال درد با دستگاه C-Arm",
                    imageUrl = "https://images.unsplash.com/photo-1586773860418-d37222d8fce3?auto=format&fit=crop&w=1200&q=80",
                    caption = "تجهیزات رادیولوژی فلوروسکوپی پیشرفته برای هدایت میلی‌متری سوزن‌های درمانی."
                ),
                GalleryImage(
                    id = "media_202",
                    title = "فضای پذیرش و مشاوره تخصصی",
                    imageUrl = "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=1200&q=80",
                    caption = "فضای آرام، مدرن و احترام‌برانگیز برای بیماران و همراهان گرامی."
                ),
                GalleryImage(
                    id = "media_203",
                    title = "دستگاه ژنراتور رادیوفرکوئنسی (RF)",
                    imageUrl = "https://images.unsplash.com/photo-1629909613654-28e377c37b09?auto=format&fit=crop&w=1200&q=80",
                    caption = "دستگاه‌های پیشرفته ساخت آلمان برای نورولیز حرارتی و پالس عصب."
                ),
                GalleryImage(
                    id = "media_204",
                    title = "اتاق ریکاوری بیماران پس از اقدام",
                    imageUrl = "https://images.unsplash.com/photo-1512678080530-7760d81faba6?auto=format&fit=crop&w=1200&q=80",
                    caption = "مراقبت پرستاری اختصاصی پس از عمل‌های سرپایی تا زمان ترخیص."
                )
            )
        ),
        GalleryAlbum(
            id = "alb_3",
            title = "حرکات اصلاحی و فیزیوتراپی تخصصی درد",
            description = "برنامه‌های ورزشی و تمرینات تقویتی ستون فقرات و زانو پس از مداخلات درمانی.",
            coverImageUrl = "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=800&q=80",
            images = listOf(
                GalleryImage(
                    id = "media_301",
                    title = "تقویت عضلات ثبات‌دهنده مهره‌ها (Core)",
                    imageUrl = "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=1200&q=80",
                    caption = "تمرینات متوازن برای پیشگیری از عود مجدد دیسک و کاهش فشار مهره‌ای."
                ),
                GalleryImage(
                    id = "media_302",
                    title = "کشش ملایم زنجیره خلفی و سیاتیک",
                    imageUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?auto=format&fit=crop&w=1200&q=80",
                    caption = "آزادسازی فاسیای عضلانی و کاهش گرفتگی عضلات پیریفورمیس و باسن."
                )
            )
        )
    )

    val videos = listOf(
        ClinicVideo(
            id = "vid_1",
            title = "روش جراحی بسته دیسک کمر با لیزر (توضیحات دکتر مجید حیدریان)",
            description = "دکتر مجید حیدریان نحوه انجام لیزر دیسک و تفاوت آن با جراحی باز را به صورت کامل تشریح می‌کنند.",
            thumbnailUrl = "https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=800&q=80",
            aparatEmbedCode = "https://www.aparat.com/video/video/embed/videohash/r020b70/vt/frame",
            category = "جراحی بسته دیسک",
            duration = "۰۸:۴۵"
        ),
        ClinicVideo(
            id = "vid_2",
            title = "علل دردهای لگن و درمان‌های کم‌تهاجمی مفاصل ساکروایلیاک",
            description = "تشخیص افتراقی درد لگن از دیسک کمر و روش‌های بلاک و رادیوفرکوئنسی اعصاب مفصل ساکروایلیاک.",
            thumbnailUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?auto=format&fit=crop&w=800&q=80",
            aparatEmbedCode = "https://www.aparat.com/video/video/embed/videohash/a658428/vt/frame",
            category = "درمان درد لگن",
            duration = "۰۶:۳۰"
        ),
        ClinicVideo(
            id = "vid_3",
            title = "درمان‌های نوین آرتروز زانو بدون جراحی تعویض مفصل",
            description = "کاربردهای پی‌آر‌پی (PRP)، ژل و رادیوفرکوئنسی اعصاب حسی زانو در بیماران مبتلا به ساییدگی مفصل.",
            thumbnailUrl = "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=800&q=80",
            aparatEmbedCode = "https://www.aparat.com/video/video/embed/videohash/k133379/vt/frame",
            category = "آرتروز زانو",
            duration = "۱۰:۱۵"
        ),
        ClinicVideo(
            id = "vid_4",
            title = "درد گردن و شانه؛ چه زمانی نیاز به مداخله اینترونشنال دارد؟",
            description = "بررسی علائم فشار دیسک گردنی روی اعصاب دست و درمان با تزریق اپیدورال و دیسکوژل.",
            thumbnailUrl = "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&w=800&q=80",
            aparatEmbedCode = "https://www.aparat.com/video/video/embed/videohash/n129202/vt/frame",
            category = "دیسک گردن",
            duration = "۰۷:۲۰"
        )
    )
}
