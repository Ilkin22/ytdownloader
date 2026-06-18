package com.ytdownloader.app.data.downloader

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DownloadEngine"
private const val BUFFER_SIZE = 32 * 1024        // 32 KB
private const val MAX_RETRIES = 3

/**
 * HTTP Range tabanlı parçalı indirme motoru.
 * Resume desteği: mevcut kısmi dosya boyutunu Range başlangıcı olarak kullanır.
 */
@Singleton
class DownloadEngine @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    /**
     * Verilen URL'yi [destFile]'a indirir.
     * @param onProgress  (downloadedBytes, totalBytes) callback — saniyede ~1 kez tetiklenir
     * @param isCancelled İptal sinyali için lambda
     */
    suspend fun download(
        url: String,
        destFile: File,
        onProgress: suspend (Long, Long) -> Unit,
        isCancelled: () -> Boolean
    ) = withContext(Dispatchers.IO) {
        var attempt = 0
        var success = false

        while (attempt < MAX_RETRIES && !isCancelled() && !success) {
            try {
                val resumeOffset = if (destFile.exists()) destFile.length() else 0L

                val request = Request.Builder()
                    .url(url)
                    .apply {
                        if (resumeOffset > 0) {
                            header("Range", "bytes=$resumeOffset-")
                        }
                    }
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful && response.code != 206) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw Exception("Boş yanıt gövdesi")
                val contentLength = body.contentLength()
                val totalBytes = if (resumeOffset > 0 && contentLength > 0)
                    resumeOffset + contentLength else contentLength

                var downloadedBytes = resumeOffset
                var lastProgressNotify = System.currentTimeMillis()

                val outputStream = FileOutputStream(destFile, resumeOffset > 0)
                body.byteStream().use { input ->
                    outputStream.use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (isCancelled() || !isActive) {
                                Log.d(TAG, "İndirme iptal edildi")
                                return@withContext
                            }
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            // İlerlemeyi saniyede en fazla 1 kez bildir
                            val now = System.currentTimeMillis()
                            if (now - lastProgressNotify >= 1000L) {
                                onProgress(downloadedBytes, totalBytes)
                                lastProgressNotify = now
                            }
                        }
                    }
                }

                // Son ilerlemeyi kesinlikle bildir
                onProgress(downloadedBytes, totalBytes)
                success = true
                Log.d(TAG, "İndirme tamamlandı: ${destFile.name} (${downloadedBytes} bytes)")

            } catch (e: Exception) {
                attempt++
                Log.w(TAG, "İndirme başarısız (deneme $attempt/$MAX_RETRIES): ${e.message}")
                if (attempt < MAX_RETRIES && !isCancelled()) {
                    val backoffMs = (1L shl attempt) * 2000L  // 2s, 4s, 8s
                    kotlinx.coroutines.delay(backoffMs)
                } else if (attempt >= MAX_RETRIES) {
                    throw e
                }
            }
        }
    }
}
