package com.ytdownloader.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytdownloader.app.domain.model.VideoInfo
import com.ytdownloader.app.domain.usecase.GetVideoInfoUseCase
import com.ytdownloader.app.domain.usecase.StartDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    object Idle : HomeUiState()
    object Loading : HomeUiState()
    data class VideoLoaded(val videoInfo: VideoInfo) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data class DownloadStarted(val taskId: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getVideoInfoUseCase: GetVideoInfoUseCase,
    private val startDownloadUseCase: StartDownloadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    fun onUrlChanged(url: String) {
        _urlInput.value = url
        if (_uiState.value is HomeUiState.Error || _uiState.value is HomeUiState.VideoLoaded) {
            _uiState.value = HomeUiState.Idle
        }
    }

    fun fetchVideoInfo(url: String = _urlInput.value) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            getVideoInfoUseCase(url)
                .onSuccess { _uiState.value = HomeUiState.VideoLoaded(it) }
                .onFailure { _uiState.value = HomeUiState.Error(it.message ?: "Bilinmeyen hata") }
        }
    }

    fun startDownload(videoInfo: VideoInfo, resolution: Int, audioOnly: Boolean = false) {
        viewModelScope.launch {
            startDownloadUseCase(videoInfo, resolution, audioOnly)
                .onSuccess { id ->
                    _uiState.value = HomeUiState.DownloadStarted(id)
                }
                .onFailure {
                    _uiState.value = HomeUiState.Error(it.message ?: "İndirme başlatılamadı")
                }
        }
    }

    fun reset() {
        _uiState.value = HomeUiState.Idle
        _urlInput.value = ""
    }
}
