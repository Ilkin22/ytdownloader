package com.ytdownloader.app.presentation.quality

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ytdownloader.app.domain.model.StreamFormat
import com.ytdownloader.app.domain.model.VideoInfo
import com.ytdownloader.app.presentation.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityBottomSheet(
    videoInfo: VideoInfo,
    onDismiss: () -> Unit,
    onDownload: (resolution: Int, audioOnly: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartialExpansion = true)
    var audioOnly by remember { mutableStateOf(false) }
    var selectedResolution by remember { mutableStateOf<Int?>(null) }

    // Mevcut video kaliteleri (tekilleştirilmiş, sıralı)
    val videoQualities = remember(videoInfo) {
        videoInfo.formats
            .filter { it.hasVideo }
            .map { it.resolution }
            .distinct()
            .sortedDescending()
    }

    val hasAudioStream = remember(videoInfo) {
        videoInfo.formats.any { it.hasAudio && !it.hasVideo }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(40.dp, 4.dp)
                    .background(GlassBorder, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            // Video önizleme
            VideoPreviewHeader(videoInfo = videoInfo)

            Spacer(Modifier.height(24.dp))

            // Sadece ses seçeneği
            if (hasAudioStream) {
                AudioOnlyToggle(
                    checked = audioOnly,
                    onToggle = {
                        audioOnly = it
                        if (it) selectedResolution = null
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

            // Kalite seçimi (ses modu değilse)
            if (!audioOnly) {
                Text(
                    text = "Kalite Seç",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(videoQualities) { resolution ->
                        QualityItem(
                            resolution = resolution,
                            isSelected = selectedResolution == resolution,
                            onClick = { selectedResolution = resolution }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // İndir butonu
            val canDownload = audioOnly || selectedResolution != null
            Button(
                onClick = {
                    if (audioOnly) {
                        onDownload(-1, true)
                    } else {
                        selectedResolution?.let { onDownload(it, false) }
                    }
                },
                enabled = canDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White,
                    disabledContainerColor = GlassBorder
                )
            ) {
                Icon(
                    if (audioOnly) Icons.Default.AudioFile else Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (audioOnly) "MP3 İndir" else "Video İndir",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VideoPreviewHeader(videoInfo: VideoInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Küçük resim
        AsyncImage(
            model = videoInfo.thumbnailUrl,
            contentDescription = "Video önizleme",
            modifier = Modifier
                .size(120.dp, 68.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )

        // Başlık ve bilgi
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = videoInfo.title,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = videoInfo.uploaderName,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            val duration = formatDuration(videoInfo.durationSeconds)
            Text(
                text = "⏱ $duration",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun AudioOnlyToggle(checked: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) Primary.copy(alpha = 0.2f) else GlassBackground
        ),
        border = BorderStroke(
            1.dp,
            if (checked) Primary.copy(alpha = 0.7f) else GlassBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.AudioFile,
                contentDescription = null,
                tint = if (checked) Primary else OnSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Yalnızca Ses (MP3)",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (checked) Primary else OnSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "En yüksek kalitede ses çıkar",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Primary
                )
            )
        }
    }
}

@Composable
private fun QualityItem(
    resolution: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = resolutionLabel(resolution)
    val badge = resolutionBadge(resolution)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.2f) else GlassBackground
        ),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) Primary else GlassBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Seçim göstergesi
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = Primary)
            )

            Spacer(Modifier.width(8.dp))

            // Kalite etiketi
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) Primary else OnSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(Modifier.weight(1f))

            // HD / 4K rozeti
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badge.second.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = badge.first,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badge.second,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun resolutionLabel(res: Int) = when {
    res >= 2160 -> "4K (2160p)"
    res >= 1440 -> "2K (1440p)"
    res >= 1080 -> "Full HD (1080p)"
    res >= 720  -> "HD (720p)"
    res >= 480  -> "480p"
    res >= 360  -> "360p"
    res >= 240  -> "240p"
    else        -> "144p"
}

private fun resolutionBadge(res: Int): Pair<String, Color>? = when {
    res >= 2160 -> "4K" to Color(0xFFFFD700)
    res >= 1440 -> "2K" to Color(0xFF9C27B0)
    res >= 1080 -> "FHD" to Info
    res >= 720  -> "HD" to Success
    else        -> null
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}
