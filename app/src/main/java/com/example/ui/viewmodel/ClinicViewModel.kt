package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Article
import com.example.data.model.ClinicInfo
import com.example.data.model.ClinicVideo
import com.example.data.model.ConsultationRequest
import com.example.data.model.GalleryAlbum
import com.example.data.repository.ClinicRepository
import com.example.data.repository.ClinicRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface ConsultationUiState {
    data object Idle : ConsultationUiState
    data object Submitting : ConsultationUiState
    data class Success(val trackingCode: String) : ConsultationUiState
    data class Error(val message: String) : ConsultationUiState
}

class ClinicViewModel(
    private val repository: ClinicRepository = ClinicRepositoryImpl()
) : ViewModel() {

    private val _articlesState = MutableStateFlow<UiState<List<Article>>>(UiState.Loading)
    val articlesState: StateFlow<UiState<List<Article>>> = _articlesState.asStateFlow()

    private val _galleryState = MutableStateFlow<UiState<List<GalleryAlbum>>>(UiState.Loading)
    val galleryState: StateFlow<UiState<List<GalleryAlbum>>> = _galleryState.asStateFlow()

    private val _videosState = MutableStateFlow<UiState<List<ClinicVideo>>>(UiState.Loading)
    val videosState: StateFlow<UiState<List<ClinicVideo>>> = _videosState.asStateFlow()

    private val _clinicInfo = MutableStateFlow(ClinicInfo())
    val clinicInfo: StateFlow<ClinicInfo> = _clinicInfo.asStateFlow()

    private val _consultationState = MutableStateFlow<ConsultationUiState>(ConsultationUiState.Idle)
    val consultationState: StateFlow<ConsultationUiState> = _consultationState.asStateFlow()

    // Active selected items for details
    private val _selectedArticle = MutableStateFlow<Article?>(null)
    val selectedArticle: StateFlow<Article?> = _selectedArticle.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<GalleryAlbum?>(null)
    val selectedAlbum: StateFlow<GalleryAlbum?> = _selectedAlbum.asStateFlow()

    private val _selectedVideo = MutableStateFlow<ClinicVideo?>(null)
    val selectedVideo: StateFlow<ClinicVideo?> = _selectedVideo.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadArticles()
        loadGallery()
        loadVideos()
        loadClinicInfo()
    }

    fun loadArticles() {
        viewModelScope.launch {
            _articlesState.value = UiState.Loading
            repository.getArticles()
                .onSuccess { articles ->
                    _articlesState.value = UiState.Success(articles)
                }
                .onFailure { error ->
                    _articlesState.value = UiState.Error(error.message ?: "خطا در دریافت مقالات")
                }
        }
    }

    fun loadGallery() {
        viewModelScope.launch {
            _galleryState.value = UiState.Loading
            repository.getGalleryAlbums()
                .onSuccess { albums ->
                    _galleryState.value = UiState.Success(albums)
                }
                .onFailure { error ->
                    _galleryState.value = UiState.Error(error.message ?: "خطا در دریافت تصاویر")
                }
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            _videosState.value = UiState.Loading
            repository.getVideos()
                .onSuccess { videos ->
                    _videosState.value = UiState.Success(videos)
                }
                .onFailure { error ->
                    _videosState.value = UiState.Error(error.message ?: "خطا در دریافت ویدیوها")
                }
        }
    }

    private fun loadClinicInfo() {
        viewModelScope.launch {
            _clinicInfo.value = repository.getClinicInfo()
        }
    }

    fun selectArticle(article: Article) {
        _selectedArticle.value = article
    }

    fun selectAlbum(album: GalleryAlbum) {
        _selectedAlbum.value = album
    }

    fun selectVideo(video: ClinicVideo) {
        _selectedVideo.value = video
    }

    fun submitConsultation(name: String, phone: String, subject: String, message: String) {
        if (name.isBlank() || phone.isBlank() || message.isBlank()) {
            _consultationState.value = ConsultationUiState.Error("لطفاً تمامی فیلدهای الزامی را تکمیل نمایید.")
            return
        }

        viewModelScope.launch {
            _consultationState.value = ConsultationUiState.Submitting
            val request = ConsultationRequest(
                name = name.trim(),
                phoneNumber = phone.trim(),
                subject = subject.ifBlank { "مشاوره عمومی" },
                message = message.trim()
            )
            repository.sendConsultationRequest(request)
                .onSuccess { trackingCode ->
                    _consultationState.value = ConsultationUiState.Success(trackingCode)
                }
                .onFailure { error ->
                    _consultationState.value = ConsultationUiState.Error(error.message ?: "خطا در ارسال پیام، لطفاً مجدداً تلاش فرمایید.")
                }
        }
    }

    fun resetConsultationState() {
        _consultationState.value = ConsultationUiState.Idle
    }
}
