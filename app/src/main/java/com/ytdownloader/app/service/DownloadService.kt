package com.ytdownloader.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ytdownloader.app.R
import com.ytdownloader.app.domain.model.DownloadItem
import com.ytdownloader.app.domain.model.DownloadStatus
import com.ytdownloader.app.domain.usecase.GetDownloadsUseCase
import com.ytdownloader.app.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.ytdownloader.app.STOP_SERVICE"
    }

    @Inject
    lateinit var getDownloadsUseCase: GetDownloadsUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("İndirme başlatılıyor…", 0))
        observeActiveDownloads()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeActiveDownloads() {
        observeJob?.cancel()
        observeJob = serviceScope.launch {
            getDownloadsUseCase.all().collectLatest { downloads ->
                val active = downloads.filter {
                    it.status in listOf(
                        DownloadStatus.RUNNING,
                        DownloadStatus.QUEUED,
                        DownloadStatus.MUXING
                    )
                }

                if (active.isEmpty()) {
                    stopSelf()
                } else {
                    updateNotification(active)
                }
            }
        }
    }

    private fun updateNotification(downloads: List<DownloadItem>) {
        val running = downloads.firstOrNull { it.status == DownloadStatus.RUNNING }
        val label = running?.let {
            "${it.title.take(30)}… — %${it.progressPercent}"
        } ?: "${downloads.size} indirme sırada"

        val maxProgress = downloads.sumOf { it.totalBytes }
        val currentProgress = downloads.sumOf { it.downloadedBytes }
        val percent = if (maxProgress > 0)
            ((currentProgress.toDouble() / maxProgress) * 100).toInt() else 0

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(label, percent))
    }

    private fun buildNotification(contentText: String, progressPercent: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DownloadService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YT Downloader")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, progressPercent, progressPercent == 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Durdur", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "İndirme Bildirimleri",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Arka planda devam eden indirme bildirimleri"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
