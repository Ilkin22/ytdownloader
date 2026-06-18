package com.ytdownloader.app.presentation.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytdownloader.app.domain.model.DownloadItem
import com.ytdownloader.app.domain.model.DownloadStatus
import com.ytdownloader.app.domain.usecase.GetDownloadsUseCase
import com.ytdownloader.app.domain.usecase.PauseResumeDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val pauseResumeUseCase: PauseResumeDownloadUseCase
) : ViewModel() {

    /** Aktif indirme görevleri (tamamlananlar hariç) */
    val activeDownloads = getDownloadsUseCase.all()
        .map { list ->
            list.filter {
                it.status != DownloadStatus.COMPLETED &&
                it.status != DownloadStatus.CANCELLED
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun pause(id: String) = viewModelScope.launch { pauseResumeUseCase.pause(id) }
    fun resume(id: String) = viewModelScope.launch { pauseResumeUseCase.resume(id) }
    fun cancel(id: String) = viewModelScope.launch { pauseResumeUseCase.cancel(id) }
    fun retry(id: String) = viewModelScope.launch { pauseResumeUseCase.retry(id) }
}
