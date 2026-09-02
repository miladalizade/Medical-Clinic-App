package com.example.data.model

data class WorkingHour(
    val dayRange: String,
    val hours: String
)

data class Specialty(
    val title: String,
    val description: String,
    val iconName: String = "Medical"
)

data class ClinicInfo(
    val clinicName: String = "کلینیک فوق تخصصی درد ماهان",
    val doctorName: String = "دکتر مجید حیدریان",
    val doctorTitle: String = "فوق تخصص درد و ستون فقرات | فلوشیپ اینترونشنال درد",
    val medicalLicense: String = "نظام پزشکی: ۹۰۰۱۱",
    val biography: String = "دکتر مجید حیدریان، فوق تخصص درد از دانشگاه علوم پزشکی ایران و مؤسس کلینیک درد ماهان است. ایشان دارای گواهینامه تخصصی جراحی دیسک با لیزر از کشور آلمان و مدرک کاربرد اوزون در درمان دیسک از دانشگاه شهید بهشتی بوده و عضو فعال انجمن بررسی و مطالعه درد آمریکا (APS) می‌باشد. کلینیک درد ماهان با بهره‌گیری از اتاق عمل مجهز مجهز به رادیولوژی فلوروسکوپی، روش‌های درمانی بسته و بدون جراحی باز را به بیماران عزیز ارائه می‌نماید.",
    val specialties: List<Specialty> = listOf(
        Specialty("جراحی بسته و لیزری دیسک", "درمان دیسک کمر و گردن بدون بیهوشی و برش جراحی"),
        Specialty("اوزون تراپی و تزریق دیسکوژل", "ترمیم و کاهش التهاب دیسک با اوزون و ژل‌های پیشرفته"),
        Specialty("رادیوفرکوئنسی اعصاب و مفاصل (RF)", "تسکین طولانی‌مدت دردهای ستون فقرات، گردن و آرتروز مفاصل"),
        Specialty("درمان دردهای زانو، شانه و لگن", "تزریق‌های تخصصی پی‌آر‌پی (PRP)، اسید هیالورونیک و بلاک عصب"),
        Specialty("کارگذاری پمپ و پورت درد", "کنترل دردهای مزمن و مقاوم به درمان‌های دارویی")
    ),
    val phoneNumber: String = "021-88866619",
    val secondaryPhoneNumber: String = "021-88866629",
    val emergencyPhone: String = "09309524654",
    val address: String = "تهران، خیابان مطهری، خیابان فجر، روبروی بیمارستان جم، کوچه مدائن، پلاک ۲۰، ساختمان پزشکان ماهان، طبقه ۵، واحد ۱۰",
    val mapLatitude: Double = 35.7262,
    val mapLongitude: Double = 51.4248,
    val workingHours: List<WorkingHour> = listOf(
        WorkingHour("شنبه تا چهارشنبه", "۱۴:۰۰ الی ۲۰:۰۰"),
        WorkingHour("پنج‌شنبه‌ها", "۰۹:۰۰ الی ۱۳:۰۰"),
        WorkingHour("جمعه‌ها و ایام تعطیل", "مشاوره تلفنی و هماهنگی اورژانسی")
    ),
    val instagramUrl: String = "https://instagram.com/dr.heidarian",
    val telegramUrl: String = "https://t.me/drheidarian",
    val whatsappUrl: String = "https://wa.me/989309524654",
    val websiteUrl: String = "https://drheidarian.ir"
)
