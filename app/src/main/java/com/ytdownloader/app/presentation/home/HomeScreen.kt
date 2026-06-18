package com.ytdownloader.app.presentation.home

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ytdownloader.app.domain.model.VideoInfo
import com.ytdownloader.app.presentation.quality.QualityBottomSheet
import com.ytdownloader.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    initialUrl: String? = null,
    onNavigateToQueue: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var showQualitySheet by remember { mutableStateOf(false) }
    var currentVideoInfo by remember { mutableStateOf<VideoInfo?>(null) }

    // Dışarıdan gelen paylaşım URL'si
    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            viewModel.onUrlChanged(initialUrl)
            viewModel.fetchVideoInfo(initialUrl)
        }
    }

    // İndirme başlatıldığında kuyruğa git
    LaunchedEffect(uiState) {
        if (uiState is HomeUiState.DownloadStarted) {
            onNavigateToQueue()
            viewModel.reset()
        }
        if (uiState is HomeUiState.VideoLoaded) {
            currentVideoInfo = (uiState as HomeUiState.VideoLoaded).videoInfo
            showQualitySheet = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Secondary, SecondaryVariant, Color(0xFF0F0F1A))
                )
            )
    ) {
        // Arka plan dekoratif daireler
        BackgroundDecoration()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HomeTopBar(
                    onQueueClick = onNavigateToQueue,
                    onLibraryClick = onNavigateToLibrary,
                    onSettingsClick = onNavigateToSettings
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))

                // Logo / başlık
                AppLogo()

                Spacer(Modifier.height(40.dp))

                // URL giriş kartı
                UrlInputCard(
                    url = urlInput,
                    onUrlChange = viewModel::onUrlChanged,
                    onSearch = {
                        focusManager.clearFocus()
                        viewModel.fetchVideoInfo()
                    },
                    onPaste = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (text.isNotBlank()) {
                            viewModel.onUrlChanged(text)
                            viewModel.fetchVideoInfo(text)
                        }
                    },
                    isLoading = uiState is HomeUiState.Loading
                )

                Spacer(Modifier.height(24.dp))

                // Hata mesajı
                AnimatedVisibility(
                    visible = uiState is HomeUiState.Error,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    ErrorCard((uiState as? HomeUiState.Error)?.message ?: "")
                }

                Spacer(Modifier.height(24.dp))

                // Kılavuz ipuçları
                if (uiState is HomeUiState.Idle) {
                    TipsCard()
                }
            }
        }

        // Kalite seçim bottom sheet
        if (showQualitySheet && currentVideoInfo != null) {
            QualityBottomSheet(
                videoInfo = currentVideoInfo!!,
                onDismiss = {
                    showQualitySheet = false
                    viewModel.reset()
                },
                onDownload = { resolution, audioOnly ->
                    showQualitySheet = false
                    viewModel.startDownload(currentVideoInfo!!, resolution, audioOnly)
                }
            )
        }
    }
}

@Composable
private fun BackgroundDecoration() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Sol üst daire
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset((-80).dp, (-80).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Primary.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        // Sağ alt daire
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(80.dp, 80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Accent.copy(alpha = 0.12f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onQueueClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        actions = {
            IconButton(onClick = onQueueClick) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "İndirme Kuyruğu",
                    tint = OnSurface
                )
            }
            IconButton(onClick = onLibraryClick) {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = "Kütüphane",
                    tint = OnSurface
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Ayarlar",
                    tint = OnSurface
                )
            }
        }
    )
}

@Composable
private fun AppLogo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // YouTube play butonu ikonu
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Primary, PrimaryDark)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "YT Downloader",
            style = MaterialTheme.typography.displayMedium,
            color = OnSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "144p'den 4K'ya kadar indirin",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrlInputCard(
    url: String,
    onUrlChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPaste: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassBackground
        ),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Video URL'si",
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceVariant
            )

            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "https://youtube.com/watch?v=...",
                        color = OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null, tint = Primary)
                },
                trailingIcon = {
                    if (url.isNotBlank()) {
                        IconButton(onClick = { onUrlChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = OnSurfaceVariant)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    cursorColor = Primary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Yapıştır butonu
                OutlinedButton(
                    onClick = onPaste,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GlassBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OnSurface
                    )
                ) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Yapıştır")
                }

                // Analiz et butonu
                Button(
                    onClick = onSearch,
                    enabled = url.isNotBlank() && !isLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Analiz Et", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Error.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, Error.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = Error)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TipsCard() {
    val tips = listOf(
        Icons.Default.ContentPaste to "URL'yi kopyalayıp Yapıştır'a basın",
        Icons.Default.Share to "Diğer uygulamalardan 'Paylaş → İndir' yapın",
        Icons.Default.HighQuality to "144p'den 4K'ya kalite seçin",
        Icons.Default.AudioFile to "Yalnızca MP3 ses indirin"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Nasıl Kullanılır?",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            tips.forEach { (icon, text) ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}
