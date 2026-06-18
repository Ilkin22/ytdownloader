package com.ytdownloader.app.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ytdownloader.app.presentation.theme.YtDownloaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Başka uygulamadan gelen paylaşım URL'sini al
        val sharedUrl = if (intent?.action == Intent.ACTION_SEND &&
            intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        setContent {
            YtDownloaderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph(initialSharedUrl = sharedUrl)
                }
            }
        }
    }
}
