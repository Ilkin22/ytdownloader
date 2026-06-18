package com.ytdownloader.app.domain.repository

import com.ytdownloader.app.domain.model.DownloadItem
import com.ytdownloader.app.domain.model.DownloadStatus
import com.ytdownloader.app.domain.model.VideoInfo
import kotlinx.coroutines.flow.Flow

/**
 * İndirme işlemlerinin tüm giriş noktasını tanımlayan repository arayüzü.
 * Implementasyon data katmanında yer alır.
 */
interface DownloadRepository {

    /** URL'den video meta verisini ve stream formatlarını getirir */
    suspend fun getVideoInfo(url: String): Result<VideoInfo>

    /**
     * Yeni bir indirme görevi başlatır.
     * @return Oluşturulan görevin UUID'si
     */
    suspend fun startDownload(
        videoInfo: VideoInfo,
        targetResolution: Int,   // -1 = yalnızca ses
        audioOnly: Boolean = false
    ): Result<String>

    /** Devam eden indirmeyi duraklatır */
    suspend fun pauseDownload(id: String)

    /** Duraklatılmış indirmeyi devam ettirir */
    suspend fun resumeDownload(id: String)

    /** İndirmeyi iptal eder ve geçici dosyaları siler */
    suspend fun cancelDownload(id: String)

    /** Başarısız indirmeyi yeniden dener */
    suspend fun retryDownload(id: String)

    /** Kütüphaneden kaydı ve dosyayı siler */
    suspend fun deleteDownload(id: String)

    /** Tüm indirme görevlerini reaktif olarak gözlemler */
    fun observeAllDownloads(): Flow<List<DownloadItem>>

    /** Belirli bir duruma göre filtreler */
    fun observeDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>>

    /** ID'ye göre tek bir görevi gözlemler */
    fun observeDownload(id: String): Flow<DownloadItem?>
}
