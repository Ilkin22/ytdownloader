package com.ytdownloader.app.data.extractor

import com.ytdownloader.app.domain.model.StreamFormat
import com.ytdownloader.app.domain.model.VideoInfo
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.stream.AudioStream
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NewPipeExtractor tabanlı akış ayıklama implementasyonu.
 * YouTube URL'sinden VideoInfo ve StreamFormat listesini üretir.
 */
@Singleton
class NewPipeExtractorImpl @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    init {
        // NewPipe başlatma — uygulama ömrünce bir kez
        try {
            NewPipe.init(OkHttpDownloaderAdapter(okHttpClient))
        } catch (_: Exception) {
            // Zaten başlatılmışsa yoksay
        }
    }

    /**
     * Verilen URL'den video meta verisi ve stream listesini getirir.
     * Ağ çağrısı; IO dispatcher'da çalıştırılmalı.
     */
    suspend fun extract(url: String): VideoInfo {
        val service = NewPipe.getServiceByUrl(url)
        val extractor: StreamExtractor = service.getStreamExtractor(url)
        extractor.fetchPage()

        val videoStreams: List<VideoStream> = buildList {
            addAll(extractor.videoStreams ?: emptyList())
            addAll(extractor.videoOnlyStreams ?: emptyList())
        }

        val audioStreams: List<AudioStream> = extractor.audioStreams ?: emptyList()

        val formats = buildList<StreamFormat> {
            // Video + ses birlikte (progressive) ve video-only (DASH) akışlar
            videoStreams.forEachIndexed { index, vs ->
                val res = parseResolution(vs.resolution)
                add(
                    StreamFormat(
                        itag = index,
                        resolution = res,
                        label = vs.resolution,
                        mimeType = vs.format?.mimeType ?: "video/mp4",
                        hasVideo = true,
                        hasAudio = !vs.isVideoOnly,
                        url = vs.content ?: "",
                        sizeBytes = -1L,
                        bitrate = 0
                    )
                )
            }
            // Yalnızca ses akışları
            audioStreams.forEachIndexed { index, aus ->
                add(
                    StreamFormat(
                        itag = 1000 + index,
                        resolution = 0,
                        label = "audio",
                        mimeType = aus.format?.mimeType ?: "audio/mp4",
                        hasVideo = false,
                        hasAudio = true,
                        url = aus.content ?: "",
                        sizeBytes = -1L,
                        bitrate = aus.averageBitrate
                    )
                )
            }
        }

        return VideoInfo(
            id = extractor.id,
            title = extractor.name,
            durationSeconds = extractor.length,
            thumbnailUrl = extractor.thumbnails.firstOrNull()?.url ?: "",
            uploaderName = extractor.uploaderName ?: "",
            viewCount = extractor.viewCount,
            formats = formats.filter { it.url.isNotBlank() }
        )
    }

    private fun parseResolution(label: String): Int {
        return label.replace("p", "").replace("HD", "")
            .trim().toIntOrNull() ?: 0
    }
}

/**
 * NewPipe'ın Downloader arayüzünü OkHttpClient ile köprüleyen adaptör.
 */
private class OkHttpDownloaderAdapter(private val client: OkHttpClient) : Downloader() {
    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder().url(request.url())
        request.headers().forEach { (key, values) ->
            values.forEach { value -> builder.addHeader(key, value) }
        }
        request.dataToSend()?.let { body ->
            builder.post(okhttp3.RequestBody.create(null, body))
        }
        val response = client.newCall(builder.build()).execute()
        val responseBody = response.body?.string() ?: ""
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBody,
            response.request.url.toString()
        )
    }
}
