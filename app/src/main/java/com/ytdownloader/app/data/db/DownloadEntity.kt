package com.ytdownloader.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ytdownloader.app.domain.model.DownloadItem
import com.ytdownloader.app.domain.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceUrl: String,
    val filePath: String,
    val resolution: String,        // "2160p", "720p", "audio"
    val format: String,            // "mp4" | "mp3"
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val createdAt: Long,
    val thumbnailUrl: String?
) {
    fun toDomain() = DownloadItem(
        id = id,
        title = title,
        sourceUrl = sourceUrl,
        filePath = filePath,
        resolution = resolution,
        format = format,
        totalBytes = totalBytes,
        downloadedBytes = downloadedBytes,
        status = status,
        createdAt = createdAt,
        thumbnailUrl = thumbnailUrl
    )
}

fun DownloadItem.toEntity() = DownloadEntity(
    id = id,
    title = title,
    sourceUrl = sourceUrl,
    filePath = filePath,
    resolution = resolution,
    format = format,
    totalBytes = totalBytes,
    downloadedBytes = downloadedBytes,
    status = status,
    createdAt = createdAt,
    thumbnailUrl = thumbnailUrl
)
