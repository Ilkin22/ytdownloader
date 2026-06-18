package com.ytdownloader.app.domain.usecase

import com.ytdownloader.app.domain.model.VideoInfo
import com.ytdownloader.app.domain.repository.DownloadRepository
import javax.inject.Inject

/**
 * Verilen URL için video meta verisi ve mevcut formatları getirir.
 */
class GetVideoInfoUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(url: String): Result<VideoInfo> {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("URL boş olamaz"))
        return repository.getVideoInfo(trimmed)
    }
}
