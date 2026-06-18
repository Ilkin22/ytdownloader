package com.ytdownloader.app.data.repository

import android.util.Log
import com.ytdownloader.app.data.db.DownloadDao
import com.ytdownloader.app.data.db.DownloadEntity
import com.ytdownloader.app.data.db.toEntity
import com.ytdownloader.app.data.downloader.DownloadEngine
import com.ytdownloader.app.data.extractor.NewPipeExtractorImpl
import com.ytdownloader.app.data.muxer.MuxerImpl
import com.ytdownloader.app.data.storage.StorageManager
import com.ytdownloader.app.domain.model.DownloadItem
import com.ytdownloader.app.domain.model.DownloadStatus
import com.ytdownloader.app.domain.model.VideoInfo
import com.ytdownloader.app.domain.repository.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DownloadRepositoryImpl"

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val extractor: NewPipeExtractorImpl,
    private val downloadEngine: DownloadEngine,
    private val muxer: MuxerImpl,
    private val storageManager: StorageManager,
    private val dao: DownloadDao
) : DownloadRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val cancelFlags = ConcurrentHashMap<String, Boolean>()

    // ─── Repository arayüzü implementasyonları ────────────────────────────────

    override suspend fun getVideoInfo(url: String): Result<VideoInfo> = runCatching {
        extractor.extract(url)
    }

    override suspend fun startDownload(
        videoInfo: VideoInfo,
        targetResolution: Int,
        audioOnly: Boolean
    ): Result<String> = runCatching {
        val id = UUID.randomUUID().toString()
        val (videoStream, audioStream) = if (audioOnly) {
            val audio = videoInfo.bestAudioStream()
                ?: throw IllegalStateException("Ses akışı bulunamadı")
            null to audio
        } else {
            videoInfo.pickStreams(targetResolution)
        }

        val format = if (audioOnly) "mp3" else "mp4"
        val resLabel = if (audioOnly) "audio" else "${targetResolution}p"

        val entity = DownloadEntity(
            id = id,
            title = videoInfo.title,
            sourceUrl = videoInfo.id,
            filePath = "",
            resolution = resLabel,
            format = format,
            totalBytes = 0L,
            downloadedBytes = 0L,
            status = DownloadStatus.QUEUED,
            createdAt = System.currentTimeMillis(),
            thumbnailUrl = videoInfo.thumbnailUrl
        )
        dao.insert(entity)

        // Arka planda indirmeyi başlat
        val job = scope.launch {
            runDownload(
                id = id,
                videoInfo = videoInfo,
                videoStreamUrl = videoStream?.url,
                audioStreamUrl = audioStream?.url,
                audioOnly = audioOnly,
                format = format,
                resLabel = resLabel
            )
        }
        activeJobs[id] = job
        cancelFlags[id] = false
        id
    }

    override suspend fun pauseDownload(id: String) {
        cancelFlags[id] = true
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        dao.updateStatus(id, DownloadStatus.PAUSED)
    }

    override suspend fun resumeDownload(id: String) {
        val entity = dao.getById(id) ?: return
        cancelFlags[id] = false
        val job = scope.launch {
            runDownload(
                id = id,
                videoInfo = null,   // URL'yi entity'den alacağız
                videoStreamUrl = null,
                audioStreamUrl = null,
                audioOnly = entity.format == "mp3",
                format = entity.format,
                resLabel = entity.resolution,
                resumeEntity = entity
            )
        }
        activeJobs[id] = job
        dao.updateStatus(id, DownloadStatus.QUEUED)
    }

    override suspend fun cancelDownload(id: String) {
        cancelFlags[id] = true
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        dao.updateStatus(id, DownloadStatus.CANCELLED)
        // Geçici dosyaları temizle
        cleanTempFiles(id)
    }

    override suspend fun retryDownload(id: String) {
        val entity = dao.getById(id) ?: return
        cancelFlags[id] = false
        dao.updateStatus(id, DownloadStatus.QUEUED)
        dao.updateProgress(id, 0L, 0L, DownloadStatus.QUEUED)
        cleanTempFiles(id)
        val job = scope.launch {
            runDownload(
                id = id,
                videoInfo = null,
                videoStreamUrl = null,
                audioStreamUrl = null,
                audioOnly = entity.format == "mp3",
                format = entity.format,
                resLabel = entity.resolution,
                resumeEntity = entity
            )
        }
        activeJobs[id] = job
    }

    override suspend fun deleteDownload(id: String) {
        val entity = dao.getById(id)
        entity?.filePath?.let { storageManager.deletePermanent(it) }
        dao.deleteById(id)
        cleanTempFiles(id)
    }

    override fun observeAllDownloads(): Flow<List<DownloadItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>> =
        dao.observeByStatus(status).map { list -> list.map { it.toDomain() } }

    override fun observeDownload(id: String): Flow<DownloadItem?> =
        dao.observeById(id).map { it?.toDomain() }

    // ─── İç indirme iş akışı ──────────────────────────────────────────────────

    private suspend fun runDownload(
        id: String,
        videoInfo: VideoInfo?,
        videoStreamUrl: String?,
        audioStreamUrl: String?,
        audioOnly: Boolean,
        format: String,
        resLabel: String,
        resumeEntity: DownloadEntity? = null
    ) {
        try {
            dao.updateStatus(id, DownloadStatus.RUNNING)

            val isCancelled = { cancelFlags[id] == true }

            if (audioOnly) {
                // Yalnızca ses indirme
                val audioUrl = audioStreamUrl ?: run {
                    // Resume durumunda URL'yi yeniden çekemiyoruz; hata ver
                    Log.e(TAG, "Resume için ses URL'si yok, ID: $id")
                    dao.updateStatus(id, DownloadStatus.FAILED)
                    return
                }
                val tempAudio = File(storageManager.tempDir, "${id}_audio.m4a")

                downloadEngine.download(
                    url = audioUrl,
                    destFile = tempAudio,
                    onProgress = { dl, total ->
                        scope.launch {
                            dao.updateProgress(id, dl, total, DownloadStatus.RUNNING)
                        }
                    },
                    isCancelled = isCancelled
                )

                if (isCancelled()) return

                // MP3'e dönüştür
                dao.updateStatus(id, DownloadStatus.MUXING)
                val tempMp3 = File(storageManager.tempDir, "${id}.mp3")
                val muxResult = muxer.extractAudioToMp3(tempAudio, tempMp3)
                tempAudio.delete()

                if (muxResult.isFailure) {
                    dao.updateStatus(id, DownloadStatus.FAILED)
                    return
                }

                val title = resumeEntity?.title ?: videoInfo?.title ?: id
                val (_, filePath) = storageManager.moveToMediaStore(
                    tempMp3, sanitizeFileName("$title.mp3"), "audio/mpeg", isAudio = true
                )
                val entity = dao.getById(id) ?: return
                dao.update(entity.copy(filePath = filePath, status = DownloadStatus.COMPLETED))

            } else {
                // Video indirme
                val vUrl = videoStreamUrl ?: run {
                    dao.updateStatus(id, DownloadStatus.FAILED)
                    return
                }
                val tempVideo = File(storageManager.tempDir, "${id}_video.mp4")
                downloadEngine.download(
                    url = vUrl,
                    destFile = tempVideo,
                    onProgress = { dl, total ->
                        scope.launch {
                            dao.updateProgress(id, dl, total, DownloadStatus.RUNNING)
                        }
                    },
                    isCancelled = isCancelled
                )

                if (isCancelled()) return

                var finalFile = tempVideo

                if (audioStreamUrl != null) {
                    // DASH: ses ayrı indirilip birleştirilecek
                    val tempAudio = File(storageManager.tempDir, "${id}_audio.m4a")
                    downloadEngine.download(
                        url = audioStreamUrl,
                        destFile = tempAudio,
                        onProgress = { _, _ -> },
                        isCancelled = isCancelled
                    )

                    if (isCancelled()) return

                    dao.updateStatus(id, DownloadStatus.MUXING)
                    val tempMuxed = File(storageManager.tempDir, "${id}_muxed.mp4")
                    val muxResult = muxer.mux(tempVideo, tempAudio, tempMuxed)
                    tempVideo.delete()
                    tempAudio.delete()

                    if (muxResult.isFailure) {
                        dao.updateStatus(id, DownloadStatus.FAILED)
                        return
                    }
                    finalFile = tempMuxed
                }

                val title = resumeEntity?.title ?: videoInfo?.title ?: id
                val (_, filePath) = storageManager.moveToMediaStore(
                    finalFile, sanitizeFileName("$title.mp4"), "video/mp4", isAudio = false
                )
                val entity = dao.getById(id) ?: return
                dao.update(entity.copy(filePath = filePath, status = DownloadStatus.COMPLETED))
            }

            Log.d(TAG, "İndirme tamamlandı: $id")

        } catch (e: Exception) {
            Log.e(TAG, "İndirme hatası ($id): ${e.message}")
            if (cancelFlags[id] != true) {
                dao.updateStatus(id, DownloadStatus.FAILED)
            }
        } finally {
            activeJobs.remove(id)
            cancelFlags.remove(id)
        }
    }

    private fun cleanTempFiles(id: String) {
        listOf("${id}_video.mp4", "${id}_audio.m4a", "${id}_muxed.mp4", "${id}.mp3").forEach {
            storageManager.deleteTempFile(File(storageManager.tempDir, it))
        }
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(200)
}
