package com.ytdownloader.app.domain.usecase

import com.ytdownloader.app.domain.model.DownloadItem
import com.ytdownloader.app.domain.model.DownloadStatus
import com.ytdownloader.app.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Tüm veya belirli duruma göre indirmeleri gözlemler */
class GetDownloadsUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    /** Tüm indirmeler */
    fun all(): Flow<List<DownloadItem>> = repository.observeAllDownloads()

    /** Yalnızca tamamlananlar (kütüphane ekranı) */
    fun completed(): Flow<List<DownloadItem>> =
        repository.observeDownloadsByStatus(DownloadStatus.COMPLETED)

    /** Aktif kuyruk (QUEUED + RUNNING + PAUSED + MUXING) */
    fun active(): Flow<List<DownloadItem>> =
        repository.observeAllDownloads()

    /** Tek görev */
    fun single(id: String): Flow<DownloadItem?> = repository.observeDownload(id)

    /** İndirmeyi sil */
    suspend fun delete(id: String) = repository.deleteDownload(id)
}
