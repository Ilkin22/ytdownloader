package com.ytdownloader.app.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytdownloader.app.domain.model.DownloadItem
import com.ytdownloader.app.domain.model.DownloadStatus
import com.ytdownloader.app.domain.usecase.GetDownloadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getDownloadsUseCase: GetDownloadsUseCase
) : ViewModel() {

    val completedDownloads = getDownloadsUseCase.all()
        .map { list -> list.filter { it.status == DownloadStatus.COMPLETED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(id: String) = viewModelScope.launch {
        getDownloadsUseCase.delete(id)
    }
}
