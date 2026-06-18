package com.ytdownloader.app.data.muxer

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MuxerImpl"

/**
 * FFmpeg-Kit kullanarak video + ses birleştirme (mux) ve ses çıkarma işlemlerini yönetir.
 */
@Singleton
class MuxerImpl @Inject constructor() {

    /**
     * Video ve ses dosyalarını birleştirir (yeniden kodlama yok — hızlı kopyalama).
     * @param videoFile  Yalnızca video akışı içeren geçici dosya
     * @param audioFile  Yalnızca ses akışı içeren geçici dosya
     * @param outputFile Son çıktı dosyası (MP4)
     */
    suspend fun mux(
        videoFile: File,
        audioFile: File,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        val command = arrayOf(
            "-y",                           // Çıktıyı üzerine yaz
            "-i", videoFile.absolutePath,
            "-i", audioFile.absolutePath,
            "-c", "copy",                   // Yeniden kodlama yok
            "-map", "0:v:0",
            "-map", "1:a:0",
            outputFile.absolutePath
        ).joinToString(" ")

        Log.d(TAG, "FFmpeg mux başlatılıyor: $command")

        val session = FFmpegKit.execute(command)
        return@withContext if (ReturnCode.isSuccess(session.returnCode)) {
            Log.d(TAG, "Mux başarılı: ${outputFile.absolutePath}")
            Result.success(outputFile)
        } else {
            val error = session.failStackTrace ?: "Bilinmeyen FFmpeg hatası"
            Log.e(TAG, "Mux başarısız: $error")
            Result.failure(Exception("FFmpeg mux hatası: $error"))
        }
    }

    /**
     * Ses dosyasından MP3 çıkarır.
     * @param inputFile  M4A / WebM ses akışı
     * @param outputFile Hedef MP3 dosyası
     */
    suspend fun extractAudioToMp3(
        inputFile: File,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        val command = "-y -i ${inputFile.absolutePath} -vn -acodec libmp3lame -q:a 2 ${outputFile.absolutePath}"

        Log.d(TAG, "FFmpeg ses çıkarma başlatılıyor: $command")

        val session = FFmpegKit.execute(command)
        return@withContext if (ReturnCode.isSuccess(session.returnCode)) {
            Log.d(TAG, "Ses çıkarma başarılı: ${outputFile.absolutePath}")
            Result.success(outputFile)
        } else {
            val error = session.failStackTrace ?: "Bilinmeyen FFmpeg hatası"
            Log.e(TAG, "Ses çıkarma başarısız: $error")
            Result.failure(Exception("FFmpeg ses çıkarma hatası: $error"))
        }
    }
}
