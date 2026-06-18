package com.ytdownloader.app.domain.usecase

import com.ytdownloader.app.domain.repository.DownloadRepository
import javax.inject.Inject

/** İndirmeyi duraklatır veya devam ettirir */
class PauseResumeDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend fun pause(id: String) = repository.pauseDownload(id)
    suspend fun resume(id: String) = repository.resumeDownload(id)
    suspend fun cancel(id: String) = repository.cancelDownload(id)
    suspend fun retry(id: String) = repository.retryDownload(id)
}
