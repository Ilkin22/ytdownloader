package com.ytdownloader.app.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "StorageManager"

/**
 * Android 10+ (API 29) → MediaStore API
 * Android 9 ve altı → Doğrudan dosya yolu + WRITE_EXTERNAL_STORAGE
 */
@Singleton
class StorageManager @Inject constructor(
    private val context: Context
) {

    /**
     * Geçici dosya dizini (uygulama özel, izin gerektirmez)
     */
    val tempDir: File
        get() = File(context.cacheDir, "downloads_tmp").also { it.mkdirs() }

    /**
     * Tamamlanan dosyayı kalıcı depolamaya taşır ve URI döndürür.
     * @param tempFile   Geçici tamamlanmış dosya
     * @param fileName   Hedef dosya adı (uzantılı)
     * @param mimeType   Dosyanın MIME tipi
     * @param isAudio    Ses mi, video mu?
     * @return Erişim URI'si veya null (başarısız)
     */
    fun moveToMediaStore(
        tempFile: File,
        fileName: String,
        mimeType: String,
        isAudio: Boolean = false
    ): Pair<Uri?, String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            moveToMediaStoreModern(tempFile, fileName, mimeType, isAudio)
        } else {
            moveToLegacyStorage(tempFile, fileName, isAudio)
        }
    }

    private fun moveToMediaStoreModern(
        tempFile: File,
        fileName: String,
        mimeType: String,
        isAudio: Boolean
    ): Pair<Uri?, String> {
        val collection = if (isAudio) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val subDir = if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$subDir/YtDownloader/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(collection, values)
        if (uri == null) {
            Log.e(TAG, "MediaStore URI oluşturulamadı")
            return null to ""
        }

        return try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                tempFile.inputStream().copyTo(output)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            tempFile.delete()
            uri to (uri.toString())
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore yazma hatası: ${e.message}")
            context.contentResolver.delete(uri, null, null)
            null to ""
        }
    }

    private fun moveToLegacyStorage(
        tempFile: File,
        fileName: String,
        isAudio: Boolean
    ): Pair<Uri?, String> {
        val subDir = if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
        val dir = File(
            Environment.getExternalStoragePublicDirectory(subDir),
            "YtDownloader"
        ).also { it.mkdirs() }

        val dest = File(dir, fileName)
        return try {
            tempFile.copyTo(dest, overwrite = true)
            tempFile.delete()
            Uri.fromFile(dest) to dest.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Legacy depolama hatası: ${e.message}")
            null to ""
        }
    }

    /** Geçici dosyayı temizler */
    fun deleteTempFile(file: File) {
        if (file.exists()) file.delete()
    }

    /** MediaStore veya dosya sisteminden kalıcı dosyayı siler */
    fun deletePermanent(filePath: String) {
        try {
            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)
                context.contentResolver.delete(uri, null, null)
            } else {
                File(filePath).delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kalıcı dosya silme hatası: ${e.message}")
        }
    }
}
