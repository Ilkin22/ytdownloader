package com.ytdownloader.app.domain.model

/**
 * Kullanıcı arayüzünde ve domain katmanında kullanılan indirme görevi modeli.
 */
data class DownloadItem(
    val id: String,
    val title: String,
    val sourceUrl: String,
    val filePath: String,
    val resolution: String,       // "2160p", "audio" vb.
    val format: String,           // "mp4" | "mp3"
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val createdAt: Long,
    val thumbnailUrl: String?
) {
    val progressFraction: Float
        get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()
}
