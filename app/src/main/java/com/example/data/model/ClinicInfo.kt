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
    val clinicName: String = "کلینیک تخصصی و فوق تخصصی سلامت",
    val doctorName: String = "دکتر علی رضایی",
    val doctorTitle: String = "متخصص و جراح ارتوپدی و آسیب‌های ورزشی",
    val medicalLicense: String = "نظام پزشکی: ۱۲۳۴۵۶",
    val biography: String = "دکتر علی رضایی با بیش از ۱۵ سال سابقه طبابت و جراحی‌های پیشرفته در مراکز معتبر درمانی کشور، ارائه دهنده برترین خدمات تشخیصی، درمانی و توانبخشی با بهره‌گیری از به‌روزترین تجهیزات پزشکی است.",
    val specialties: List<Specialty> = listOf(
        Specialty("جراحی و درمان آرتروز", "تعویض مفصل زانو و لگن با روش‌های کم‌تهاجمی"),
        Specialty("آسیب‌های ورزشی", "ترمیم رباط صلیبی و منیسک و فیزیوتراپی تخصصی"),
        Specialty("ستون فقرات و دیسک کمر", "درمان‌های غیرجراحی و لیزری دیسک"),
        Specialty("طب فیزیکی و توانبخشی", "برنامه‌های اختصاصی ورزش‌درمانی و بازیابی توان حرکتی")
    ),
    val phoneNumber: String = "021-88776655",
    val emergencyPhone: String = "09120000000",
    val address: String = "تهران، خیابان ولیعصر، بالاتر از میدان ونک، کوچه نگار، پلاک ۱۲، طبقه ۳",
    val mapLatitude: Double = 35.7601,
    val mapLongitude: Double = 51.4116,
    val workingHours: List<WorkingHour> = listOf(
        WorkingHour("شنبه تا چهارشنبه", "۱۵:۰۰ الی ۲۱:۰۰"),
        WorkingHour("پنج‌شنبه‌ها", "۰۹:۰۰ الی ۱۴:۰۰"),
        WorkingHour("جمعه‌ها و ایام تعطیل", "فقط موارد اورژانسی با هماهنگی قبلی")
    ),
    val instagramUrl: String = "https://instagram.com",
    val telegramUrl: String = "https://t.me",
    val whatsappUrl: String = "https://wa.me/989120000000",
    val websiteUrl: String = "https://example.com"
)
