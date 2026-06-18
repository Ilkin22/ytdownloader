package com.ytdownloader.app.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ytdownloader.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    var defaultQuality by remember { mutableStateOf("1080p") }
    var darkTheme by remember { mutableStateOf(true) }
    var maxConcurrent by remember { mutableStateOf(2) }
    var showQualityMenu by remember { mutableStateOf(false) }

    val qualityOptions = listOf("144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p")

    Scaffold(
        containerColor = Secondary,
        topBar = {
            TopAppBar(
                title = {
                    Text("Ayarlar", color = OnSurface, fontWeight = FontWeight.Bold)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Secondary, SecondaryVariant)))
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── İndirme Ayarları ──────────────────────────────────────────────
            SettingsSection(title = "İndirme") {
                // Varsayılan kalite
                SettingsItem(
                    icon = Icons.Default.HighQuality,
                    title = "Varsayılan Kalite",
                    subtitle = defaultQuality
                ) {
                    Box {
                        DropdownMenu(
                            expanded = showQualityMenu,
                            onDismissRequest = { showQualityMenu = false },
                            containerColor = SurfaceCard
                        ) {
                            qualityOptions.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q, color = OnSurface) },
                                    onClick = {
                                        defaultQuality = q
                                        showQualityMenu = false
                                    }
                                )
                            }
                        }
                        TextButton(onClick = { showQualityMenu = true }) {
                            Text(defaultQuality, color = Primary)
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Primary
                            )
                        }
                    }
                }

                HorizontalDivider(color = GlassBorder.copy(alpha = 0.5f))

                // Eşzamanlı indirme sayısı
                SettingsItem(
                    icon = Icons.Default.Layers,
                    title = "Eşzamanlı İndirme",
                    subtitle = "Maksimum $maxConcurrent eşzamanlı indirme"
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (maxConcurrent > 1) maxConcurrent-- },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, null, tint = OnSurface)
                        }
                        Text(
                            "$maxConcurrent",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 24.dp)
                        )
                        IconButton(
                            onClick = { if (maxConcurrent < 5) maxConcurrent++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = OnSurface)
                        }
                    }
                }
            }

            // ── Görünüm Ayarları ──────────────────────────────────────────────
            SettingsSection(title = "Görünüm") {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Karanlık Tema",
                    subtitle = if (darkTheme) "Açık" else "Kapalı"
                ) {
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = { darkTheme = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Primary
                        )
                    )
                }
            }

            // ── Hakkında ──────────────────────────────────────────────────────
            SettingsSection(title = "Hakkında") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Sürüm",
                    subtitle = "1.0.0"
                ) {}

                HorizontalDivider(color = GlassBorder.copy(alpha = 0.5f))

                SettingsItem(
                    icon = Icons.Default.Gavel,
                    title = "Yasal Uyarı",
                    subtitle = "Yalnızca izinli içerikleri indirin"
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = OnSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassBackground),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = OnSurface)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
        trailing()
    }
}
