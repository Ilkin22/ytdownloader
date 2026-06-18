package com.ytdownloader.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Uygulama giriş noktası.
 * @HiltAndroidApp ile Hilt bağımlılık enjeksiyonu etkinleştirilir.
 */
@HiltAndroidApp
class YtDownloaderApp : Application()
