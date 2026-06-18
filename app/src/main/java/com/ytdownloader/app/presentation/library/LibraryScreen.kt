package com.ytdownloader.app.presentation.library

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ytdownloader.app.domain.model.DownloadItem
import com.ytdownloader.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val downloads by viewModel.completedDownloads.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        containerColor = Secondary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kütüphane",
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Secondary, SecondaryVariant)))
                .padding(padding)
        ) {
            if (downloads.isEmpty()) {
                EmptyLibraryState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Text(
                            "${downloads.size} öğe",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    items(downloads, key = { it.id }) { item ->
                        LibraryCard(
                            item = item,
                            onPlay = {
                                val uri = Uri.parse(item.filePath)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(
                                        uri,
                                        if (item.format == "mp3") "audio/mp3" else "video/mp4"
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Oynat"))
                            },
                            onShare = {
                                val uri = Uri.parse(item.filePath)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (item.format == "mp3") "audio/mp3" else "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Paylaş"))
                            },
                            onDelete = { viewModel.delete(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(
    item: DownloadItem,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        border = BorderStroke(1.dp, GlassBorder),
        onClick = onPlay
    ) {
        Column {
            // Önizleme resim / ses ikonu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(SurfaceElevated)
            ) {
                if (item.thumbnailUrl != null && item.format != "mp3") {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        if (item.format == "mp3") Icons.Default.AudioFile else Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                    )
                }

                // Oynat ikonu overlay
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = "Oynat",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )

                // Format rozeti
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceElevated.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = item.resolution,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Alt bilgi
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.format.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Seçenekler",
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = SurfaceCard
                        ) {
                            DropdownMenuItem(
                                text = { Text("Oynat", color = OnSurface) },
                                leadingIcon = {
                                    Icon(Icons.Default.PlayArrow, null, tint = Success)
                                },
                                onClick = { showMenu = false; onPlay() }
                            )
                            DropdownMenuItem(
                                text = { Text("Paylaş", color = OnSurface) },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, null, tint = Info)
                                },
                                onClick = { showMenu = false; onShare() }
                            )
                            DropdownMenuItem(
                                text = { Text("Sil", color = Error) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, null, tint = Error)
                                },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.VideoLibrary,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Kütüphane boş",
            style = MaterialTheme.typography.headlineMedium,
            color = OnSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "İndirdiğiniz videolar burada görünecek.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant
        )
    }
}
