package com.ytdownloader.app.domain.usecase

import com.ytdownloader.app.domain.model.VideoInfo
import com.ytdownloader.app.domain.repository.DownloadRepository
import javax.inject.Inject

/**
 * Seçilen kalitede bir indirme görevi başlatır.
 * @return Görev UUID'si
 */
class StartDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(
        videoInfo: VideoInfo,
        targetResolution: Int,
        audioOnly: Boolean = false
    ): Result<String> = repository.startDownload(videoInfo, targetResolution, audioOnly)
}
