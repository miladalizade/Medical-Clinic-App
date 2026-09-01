package com.example.data.model

data class ConsultationRequest(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val subject: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "در انتظار بررسی"
)
