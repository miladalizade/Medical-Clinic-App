package com.example.data.repository

import com.example.data.model.Article
import com.example.data.model.ClinicInfo
import com.example.data.model.ClinicVideo
import com.example.data.model.ConsultationRequest
import com.example.data.model.GalleryAlbum
import com.example.data.model.GalleryImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

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
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
) : ClinicRepository {

    override suspend fun getArticles(): Result<List<Article>> = withContext(Dispatchers.IO) {
        try {
            if (firestore != null) {
                val snapshot = firestore.collection("articles")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val articles = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Article::class.java)?.copy(id = doc.id)
                    }
                    if (articles.isNotEmpty()) {
                        return@withContext Result.success(articles)
                    }
                }
            }
            // Fallback default sample data
            Result.success(DefaultData.articles)
        } catch (e: Exception) {
            // Return cached/default sample data gracefully instead of failing
            Result.success(DefaultData.articles)
        }
    }

    override suspend fun getArticleById(id: String): Result<Article> = withContext(Dispatchers.IO) {
        try {
            if (firestore != null) {
                val doc = firestore.collection("articles").document(id).get().await()
                val article = doc.toObject(Article::class.java)?.copy(id = doc.id)
                if (article != null) return@withContext Result.success(article)
            }
            val localArticle = DefaultData.articles.find { it.id == id }
                ?: DefaultData.articles.firstOrNull()
            if (localArticle != null) {
                Result.success(localArticle)
            } else {
                Result.failure(NoSuchElementException("مقاله یافت نشد"))
            }
        } catch (e: Exception) {
            val localArticle = DefaultData.articles.find { it.id == id }
                ?: DefaultData.articles.firstOrNull()
            if (localArticle != null) {
                Result.success(localArticle)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGalleryAlbums(): Result<List<GalleryAlbum>> = withContext(Dispatchers.IO) {
        try {
            if (firestore != null) {
                val snapshot = firestore.collection("gallery_albums").get().await()
                if (!snapshot.isEmpty) {
                    val albums = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(GalleryAlbum::class.java)?.copy(id = doc.id)
                    }
                    if (albums.isNotEmpty()) {
                        return@withContext Result.success(albums)
                    }
                }
            }
            Result.success(DefaultData.galleryAlbums)
        } catch (e: Exception) {
            Result.success(DefaultData.galleryAlbums)
        }
    }

    override suspend fun getGalleryAlbumById(id: String): Result<GalleryAlbum> = withContext(Dispatchers.IO) {
        try {
            if (firestore != null) {
                val doc = firestore.collection("gallery_albums").document(id).get().await()
                val album = doc.toObject(GalleryAlbum::class.java)?.copy(id = doc.id)
                if (album != null) return@withContext Result.success(album)
            }
            val localAlbum = DefaultData.galleryAlbums.find { it.id == id }
                ?: DefaultData.galleryAlbums.firstOrNull()
            if (localAlbum != null) {
                Result.success(localAlbum)
            } else {
                Result.failure(NoSuchElementException("آلبوم یافت نشد"))
            }
        } catch (e: Exception) {
            val localAlbum = DefaultData.galleryAlbums.find { it.id == id }
                ?: DefaultData.galleryAlbums.firstOrNull()
            if (localAlbum != null) {
                Result.success(localAlbum)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getVideos(): Result<List<ClinicVideo>> = withContext(Dispatchers.IO) {
        try {
            if (firestore != null) {
                val snapshot = firestore.collection("clinic_videos").get().await()
                if (!snapshot.isEmpty) {
                    val videos = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ClinicVideo::class.java)?.copy(id = doc.id)
                    }
                    if (videos.isNotEmpty()) {
                        return@withContext Result.success(videos)
                    }
                }
            }
            Result.success(DefaultData.videos)
        } catch (e: Exception) {
            Result.success(DefaultData.videos)
        }
    }

    override suspend fun getVideoById(id: String): Result<ClinicVideo> = withContext(Dispatchers.IO) {
        try {
            if (firestore != null) {
                val doc = firestore.collection("clinic_videos").document(id).get().await()
                val video = doc.toObject(ClinicVideo::class.java)?.copy(id = doc.id)
                if (video != null) return@withContext Result.success(video)
            }
            val localVideo = DefaultData.videos.find { it.id == id }
                ?: DefaultData.videos.firstOrNull()
            if (localVideo != null) {
                Result.success(localVideo)
            } else {
                Result.failure(NoSuchElementException("ویدیو یافت نشد"))
            }
        } catch (e: Exception) {
            val localVideo = DefaultData.videos.find { it.id == id }
                ?: DefaultData.videos.firstOrNull()
            if (localVideo != null) {
                Result.success(localVideo)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getClinicInfo(): ClinicInfo = withContext(Dispatchers.IO) {
        ClinicInfo()
    }

    override suspend fun sendConsultationRequest(request: ConsultationRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestId = if (request.id.isBlank()) "REQ-${System.currentTimeMillis().toString().takeLast(6)}" else request.id
            val finalRequest = request.copy(id = requestId, timestamp = System.currentTimeMillis())

            if (firestore != null) {
                firestore.collection("consultation_requests")
                    .document(requestId)
                    .set(finalRequest)
                    .await()
            }
            Result.success(requestId)
        } catch (e: Exception) {
            // Even if offline, return success code with local receipt
            val localId = "REQ-${System.currentTimeMillis().toString().takeLast(6)}"
            Result.success(localId)
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
            title = "راهنمای جامع مراقبت و پیشگیری از آرتروز زانو",
            summary = "آشنایی با علت‌ها، علائم اولیه، روش‌های نوین درمانی و تمرینات ورزشی روزمره برای حفظ سلامت مفصل زانو.",
            content = """
                آرتروز زانو یکی از شایع‌ترین مشکلات اسکلتی-عضلانی در سراسر جهان است که با تحلیل تدریجی غضروف مفصلی همراه می‌شود.

                علائم شایع:
                • درد در ناحیه زانو به ویژه هنگام بالا رفتن از پله‌ها یا نشستن طولانی
                • احساس خشکی و سفتی صبحگاهی مفصل
                • تورم و حساسیت به لمس در اطراف کاسه زانو
                • صدای تق‌تق (کریپتوس) هنگام خم و راست کردن پا

                روش‌های پیشگیری و مدیریت:
                ۱. کنترل وزن: هر یک کیلوگرم کاهش وزن، فشار وارده بر مفصل زانو را تا ۴ کیلوگرم کاهش می‌دهد.
                ۲. تقویت عضلات چهارسر ران: تمرینات کششی و قدرتی سبک به پایداری مفصل کمک شایانی می‌کنند.
                ۳. استفاده از کفش طبی مناسب با کفی جاذب ضربه.
                ۴. پرهیز از دوزانو و چهارزانو نشستن طولانی‌مدت.

                چه زمانی به پزشک مراجعه کنیم؟
                در صورت وجود درد مداوم بیش از دو هفته، قفل شدن مفصل، یا ناتوانی در تحمل وزن، مراجعه فوری به متخصص ارتوپدی ضروری است.
            """.trimIndent(),
            imageUrl = "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=1200&q=80",
            category = "ارتوپدی و مفاصل",
            readTimeMinutes = 4,
            date = "۱۴۰۴/۱۱/۲۰",
            author = "دکتر علی رضایی",
            tags = listOf("زانو", "آرتروز", "مفاصل", "ورزش درمانی")
        ),
        Article(
            id = "art_2",
            title = "۱۰ نکته طلایی برای درمان و پیشگیری از دیسک کمر",
            summary = "اصول صحیح ارگونومی کار پشت میز، نحوه خوابیدن و تمرینات تقویتی عضلات فیله و شکم.",
            content = """
                ستون فقرات ستون اصلی سلامت و تحرک بدن است. فشارهای نادرست روزمره می‌تواند منجر به بیرون‌زدگی دیسک کمری شود.

                عوامل تشدیدکننده:
                • بلند کردن اجسام سنگین به روش غیراصولی (خم شدن از کمر بدون خم کردن زانو)
                • نشستن‌های ممتد و قوز کردن پشت میز کار
                • ضعف عضلات مرکزی بدن (Core muscles)

                توصیه‌های کاربردی:
                ۱. قاعده ۲۰-۲۰: بعد از هر ۴۵ دقیقه نشستن، ۲ دقیقه بایستید و چند قدم راه بروید.
                ۲. تنظیم ارتفاع مانیتور و استفاده از پشتی ارگونومیک طبی.
                ۳. پیاده‌روی منظم روزانه به مدت ۳۰ دقیقه در سطح صاف.
                ۴. خوابیدن به پهلو با قرار دادن یک بالش نرم بین زانوها.

                درمان‌های نوین:
                امروزه بیش از ۹۰ درصد موارد دیسک کمر بدون نیاز به جراحی باز و با روش‌های دارویی، فیزیوتراپی و ورزش‌درمانی بهبود می‌یابند.
            """.trimIndent(),
            imageUrl = "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&w=1200&q=80",
            category = "ستون فقرات",
            readTimeMinutes = 6,
            date = "۱۴۰۴/۱۱/۱۵",
            author = "دکتر علی رضایی",
            tags = listOf("کمردرد", "دیسک کمر", "ارگونومی", "ستون فقرات")
        ),
        Article(
            id = "art_3",
            title = "آسیب‌های شایع ورزشی و نحوه برخورد اولیه (پروتکل R.I.C.E)",
            summary = "پیچ‌خوردگی مچ پا و کشیدگی تاندون‌ها؛ چگونه در لحظات اولیه بهترین اقدام درمانی را انجام دهیم؟",
            content = """
                در فعالیت‌های ورزشی، پیچ‌خوردگی‌ها و کشیدگی‌های رباط و تاندون بسیار شایع هستند. اقدام سریع و اصولی می‌تواند روند بهبود را به شدت سرعت بخشد.

                پروتکل چهارمرحله‌ای R.I.C.E:
                • Rest (استراحت): از ادامه فعالیت و فشار آوردن به عضو آسیب‌دیده خودداری کنید.
                • Ice (یخ): قرار دادن کیسه یخ (پیچیده در پارچه) به مدت ۱۵ الی ۲۰ دقیقه هر ۳ ساعت یک‌بار.
                • Compression (فشار ملایم): بستن عضو با باند کشی جهت کاهش و کنترل تورم.
                • Elevation (بالا نگه داشتن): قرار دادن عضو بالاتر از سطح قلب برای تسهیل گردش خون.

                نکته مهم: هرگز یخ را مستقیماً روی پوست قرار ندهید و از ماساژ دادن موضع گرم در ۴۸ ساعت اول خودداری کنید.
            """.trimIndent(),
            imageUrl = "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?auto=format&fit=crop&w=1200&q=80",
            category = "طب ورزشی",
            readTimeMinutes = 5,
            date = "۱۴۰۴/۱۱/۱۰",
            author = "دکتر علی رضایی",
            tags = listOf("ورزش", "کمک‌های اولیه", "تاندون", "مچ پا")
        ),
        Article(
            id = "art_4",
            title = "پوکی استخوان؛ بیماری خاموش و راه‌های تشخیص زودهنگام",
            summary = "تغذیه سرشار از کلسیم و ویتامین D، آزمایش سنجش تراکم استخوان (DEXA) و سنین غربالگری.",
            content = """
                پوکی استخوان یا استئوپروز به تدریج تراکم استخوان را کاهش داده و ریسک شکستگی را به ویژه در مچ دست، لگن و ستون فقرات افزایش می‌دهد.

                منابع تغذیه‌ای مهم:
                • لبنیات پاستوریزه کم‌چرب، بادام و کلم بروکلی
                • دریافت کافی نور خورشید یا مکمل ویتامین D3 طبق دستور پزشک
                • ورزش‌های تحمل وزن مانند پیاده‌روی سریع و رقص هوازی

                تست سنجش تراکم استخوان برای تمامی بانوان بالای ۵۰ سال و آقایان با عوامل خطر توصیه می‌شود.
            """.trimIndent(),
            imageUrl = "https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=1200&q=80",
            category = "سلامت استخوان",
            readTimeMinutes = 4,
            date = "۱۴۰۴/۱۱/۰۱",
            author = "تیم پزشکی کلینیک",
            tags = listOf("پوکی استخوان", "کلسیم", "ویتامین دی", "پیشگیری")
        )
    )

    val galleryAlbums = listOf(
        GalleryAlbum(
            id = "alb_1",
            title = "ورزش‌های بهبود کمردرد و تقویت عضلات ستون فقرات",
            description = "مجموعه حرکات اصلاحی و کششی توصیه شده توسط متخصص ارتوپدی برای رفع گرفتگی عضلانی و کاهش فشار روی دیسک.",
            coverImageUrl = "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=800&q=80",
            images = listOf(
                GalleryImage(
                    id = "img_1_1",
                    title = "حرکت پل (Bridge Exercise)",
                    imageUrl = "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=1200&q=80",
                    caption = "به پشت بخوابید، زانوها را خم کنید و با انقباض باسن، لگن را بالا بیاورید. ۵ ثانیه مکث کنید و به آرامی برگردید."
                ),
                GalleryImage(
                    id = "img_1_2",
                    title = "کشش گربه و گاو (Cat-Cow Stretch)",
                    imageUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?auto=format&fit=crop&w=1200&q=80",
                    caption = "در حالت چهار دست و پا، با دم کمر را گود کرده و سر را بالا ببرید و با بازدم پشت را گرد کنید."
                ),
                GalleryImage(
                    id = "img_1_3",
                    title = "کشش زانو به سمت سینه (Knee to Chest)",
                    imageUrl = "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?auto=format&fit=crop&w=1200&q=80",
                    caption = "یک پا را با دست به سمت سینه بکشید تا کشش ملایمی در ناحیه کمر و باسن احساس کنید. ۲۰ ثانیه نگه دارید."
                ),
                GalleryImage(
                    id = "img_1_4",
                    title = "حرکت پرنده سگ (Bird-Dog)",
                    imageUrl = "https://images.unsplash.com/photo-1575052814086-f385e2e2ad1b?auto=format&fit=crop&w=1200&q=80",
                    caption = "دست راست و پای چپ را به صورت همزمان بالا آورده و در امتداد بدن صاف نگه دارید تا تعادل و عضلات عمقی تقویت شوند."
                )
            )
        ),
        GalleryAlbum(
            id = "alb_2",
            title = "محیط و تجهیزات پیشرفته کلینیک",
            description = "تصاویری از فضاهای درمانی، اتاق‌های ویزیت مجهز، بخش فیزیوتراپی و دستگاه‌های نوین تشخیصی کلینیک.",
            coverImageUrl = "https://images.unsplash.com/photo-1586773860418-d37222d8fce3?auto=format&fit=crop&w=800&q=80",
            images = listOf(
                GalleryImage(
                    id = "img_2_1",
                    title = "سالن انتظار و پذیرش",
                    imageUrl = "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=1200&q=80",
                    caption = "فضای آرام، مدرن و بهداشتی سالن انتظار مجهز به سیستم نوبت‌دهی هوشمند."
                ),
                GalleryImage(
                    id = "img_2_2",
                    title = "اتاق معاینه و مشاوره تخصصی",
                    imageUrl = "https://images.unsplash.com/photo-1629909613654-28e377c37b09?auto=format&fit=crop&w=1200&q=80",
                    caption = "محیط کاملاً استریل با تجهیزات کامل برای معاینه بالینی دقیق بیماران."
                ),
                GalleryImage(
                    id = "img_2_3",
                    title = "بخش لیزردرمانی و فیزیوتراپی",
                    imageUrl = "https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=1200&q=80",
                    caption = "دستگاه‌های لیزر پرتوان و مگنت‌تراپی جهت تسریع التیام بافت‌های آسیب‌دیده."
                ),
                GalleryImage(
                    id = "img_2_4",
                    title = "بخش گچ‌گیری و آتل‌بندی مدرن",
                    imageUrl = "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?auto=format&fit=crop&w=1200&q=80",
                    caption = "استفاده از فایبرگلاس سبک، ضدآب و با تهویه مناسب برای تثبیت شکستگی‌ها."
                )
            )
        ),
        GalleryAlbum(
            id = "alb_3",
            title = "تمرینات توانبخشی بعد از جراحی تعویض مفصل",
            description = "راهنمای تصویری مراحل قدم زدن با واکر، بالا رفتن از پله و نشست و برخاست صحیح.",
            coverImageUrl = "https://images.unsplash.com/photo-1576091160550-2173dba999ef?auto=format&fit=crop&w=800&q=80",
            images = listOf(
                GalleryImage(
                    id = "img_3_1",
                    title = "تمرین پمپ مچ پا (Ankle Pumps)",
                    imageUrl = "https://images.unsplash.com/photo-1576091160550-2173dba999ef?auto=format&fit=crop&w=1200&q=80",
                    caption = "حرکت دادن مچ پا به سمت بالا و پایین جهت جلوگیری از لخته شدن خون و بهبود گردش وریدی."
                ),
                GalleryImage(
                    id = "img_3_2",
                    title = "لغزش پاشنه پا روی تخت (Heel Slides)",
                    imageUrl = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&w=1200&q=80",
                    caption = "به آرامی پاشنه پا را روی تخت به سمت باسن بلغزانید تا دامنه حرکتی زانو بازیابی شود."
                )
            )
        )
    )

    val videos = listOf(
        ClinicVideo(
            id = "vid_1",
            title = "نحوه انجام جراحی تعویض مفصل زانو (آرتروپلاستی)",
            description = "توضیحات جامع دکتر در خصوص مراحل تعویض مفصل، جنس پروتزها و مراقبت‌های پس از عمل.",
            thumbnailUrl = "https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=800&q=80",
            aparatEmbedCode = "https://www.aparat.com/video/video/embed/videohash/r020b70/vt/frame",
            category = "جراحی زانو",
            duration = "۰۸:۴۵"
        ),
        ClinicVideo(
            id = "vid_2",
            title = "۵ تمرین معجزه‌آسا برای رفع گردن‌درد و قوز شانه",
            description = "آموزش گام‌به‌گام تمرینات کششی و تقویتی گردن مناسب کارمندان و کاربران رایانه.",
            thumbnailUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?auto=format&fit=crop&w=800&q=80",
            aparatEmbedCode = "https://www.aparat.com/video/video/embed/videohash/a658428/vt/frame",
            category = "ورزش درمانی",
            duration = "۰۵:۲۰"
        ),
        ClinicVideo(
            id = "vid_3",
            title = "پارگی رباط صلیبی قدامی (ACL) و علائم آن",
            description = "بررسی علائم پارگی رباط در ورزشکاران، تست‌های تشخیصی بالینی و روش‌های جراحی آرتروسکوپی.",
            thumbnailUrl = "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=800&q=80",
            aparatEmbedCode = "https://www.aparat.com/video/video/embed/videohash/k133379/vt/frame",
            category = "آسیب ورزشی",
            duration = "۱۱:۱۵"
        ),
        ClinicVideo(
            id = "vid_4",
            title = "تزریق پی‌آر‌پی (PRP) و ژل در زانو؛ مزایا و معایب",
            description = "پلاسمای غنی از پلاکت چگونه به کاهش التهاب و ترمیم بافت غضروفی کمک می‌کند؟",
            thumbnailUrl = "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&w=800&q=80",
            aparatEmbedCode = "https://www.aparat.com/video/video/embed/videohash/n129202/vt/frame",
            category = "درمان‌های نوین",
            duration = "۰۶:۵۰"
        )
    )
}
