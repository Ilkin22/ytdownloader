package com.ytdownloader.app.domain.model

/**
 * Video hakkında tüm meta verileri ve mevcut stream formatlarını taşır.
 */
data class VideoInfo(
    val id: String,
    val title: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val uploaderName: String,
    val viewCount: Long,
    val formats: List<StreamFormat>
) {
    /** Hedef çözünürlüğe en uygun video + opsiyonel ses stream çiftini döndürür. */
    fun pickStreams(targetResolution: Int): Pair<StreamFormat, StreamFormat?> {
        val videoFormats = formats.filter { it.hasVideo }
            .filter { it.resolution <= targetResolution }
            .sortedByDescending { it.resolution }

        val video = videoFormats.firstOrNull()
            ?: formats.filter { it.hasVideo }.maxByOrNull { it.resolution }!!

        val audio = if (!video.hasAudio) {
            formats.filter { it.hasAudio && !it.hasVideo }
                .maxByOrNull { it.bitrate }
        } else null

        return video to audio
    }

    /** Yalnızca ses için en iyi stream */
    fun bestAudioStream(): StreamFormat? =
        formats.filter { it.hasAudio && !it.hasVideo }
            .maxByOrNull { it.bitrate }
}
