package com.ytdownloader.app.domain.model

/** İndirme görevinin mevcut durumu */
enum class DownloadStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    MUXING,      // FFmpeg birleştirme aşaması
    COMPLETED,
    FAILED,
    CANCELLED
}
