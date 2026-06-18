package com.ytdownloader.app.presentation.queue

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ytdownloader.app.domain.model.DownloadItem
import com.ytdownloader.app.domain.model.DownloadStatus
import com.ytdownloader.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    viewModel: QueueViewModel = hiltViewModel()
) {
    val downloads by viewModel.activeDownloads.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Secondary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "İndirme Kuyruğu",
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = OnSurface)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToLibrary) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = "Kütüphane", tint = OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Secondary, SecondaryVariant))
                )
                .padding(padding)
        ) {
            if (downloads.isEmpty()) {
                EmptyQueueState(onGoBack = onNavigateBack)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "${downloads.size} indirme",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    items(downloads, key = { it.id }) { item ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            DownloadQueueCard(
                                item = item,
                                onPause = { viewModel.pause(item.id) },
                                onResume = { viewModel.resume(item.id) },
                                onCancel = { viewModel.cancel(item.id) },
                                onRetry = { viewModel.retry(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadQueueCard(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Küçük resim
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp, 46.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(item.status)
                        Text(
                            text = item.resolution,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Kontrol butonları
                Row {
                    when (item.status) {
                        DownloadStatus.RUNNING, DownloadStatus.MUXING -> {
                            IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Pause, contentDescription = "Duraklat", tint = Warning)
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Devam", tint = Success)
                            }
                        }
                        DownloadStatus.FAILED -> {
                            IconButton(onClick = onRetry, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Yeniden dene", tint = Info)
                            }
                        }
                        else -> {}
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "İptal", tint = OnSurfaceVariant)
                    }
                }
            }

            // İlerleme çubuğu
            if (item.status == DownloadStatus.RUNNING || item.status == DownloadStatus.MUXING) {
                Spacer(Modifier.height(12.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (item.status == DownloadStatus.MUXING) "Birleştiriliyor…"
                                   else formatBytes(item.downloadedBytes) + " / " + formatBytes(item.totalBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = "%${item.progressPercent}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { item.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Primary,
                        trackColor = GlassBorder
                    )
                }
            } else if (item.status == DownloadStatus.PAUSED && item.totalBytes > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Warning,
                    trackColor = GlassBorder
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: DownloadStatus) {
    val (label, color) = when (status) {
        DownloadStatus.QUEUED    -> "Sırada" to OnSurfaceVariant
        DownloadStatus.RUNNING   -> "İndiriliyor" to Info
        DownloadStatus.PAUSED    -> "Duraklatıldı" to Warning
        DownloadStatus.MUXING    -> "Birleştiriliyor" to Accent
        DownloadStatus.FAILED    -> "Başarısız" to Error
        DownloadStatus.CANCELLED -> "İptal" to OnSurfaceVariant
        DownloadStatus.COMPLETED -> "Tamamlandı" to Success
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyQueueState(onGoBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.DownloadDone,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Kuyruk boş",
            style = MaterialTheme.typography.headlineMedium,
            color = OnSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Henüz aktif indirme yok.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGoBack,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Yeni İndirme")
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "?"
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576    -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024         -> "%.1f KB".format(bytes / 1024.0)
        else                  -> "$bytes B"
    }
}
