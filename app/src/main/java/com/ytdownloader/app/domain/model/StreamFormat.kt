package com.ytdownloader.app.domain.model

/**
 * Video akış formatını temsil eder.
 * @param itag     YouTube itag numarası
 * @param resolution  Çözünürlük (ör. 1080, 720)
 * @param label    Kullanıcıya gösterilecek etiket (ör. "1080p")
 * @param mimeType Video/ses MIME tipi
 * @param hasVideo Video akışı içeriyor mu?
 * @param hasAudio Ses akışı içeriyor mu?
 * @param url      İndirme URL'si
 * @param sizeBytes Tahmini dosya boyutu (-1 bilinmiyorsa)
 * @param bitrate  Ses bitrate (bps); yalnızca ses akışları için
 */
data class StreamFormat(
    val itag: Int,
    val resolution: Int,        // px olarak dikey çözünürlük
    val label: String,          // "1080p", "720p", "audio"
    val mimeType: String,
    val hasVideo: Boolean,
    val hasAudio: Boolean,
    val url: String,
    val sizeBytes: Long = -1L,
    val bitrate: Int = 0
)
