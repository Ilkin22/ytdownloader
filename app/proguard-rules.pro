# Genel kurallar
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
# ─── YtDownloader ProGuard Kuralları ─────────────────────────────────────────

# Kotlin / Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.** { *; }

# Room — entity sınıfları korunmalı
-keep class com.ytdownloader.app.data.db.** { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Hilt — DI bileşenleri
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }
-keepclassmembers class * {
    @dagger.hilt.* <init>(...);
}

# NewPipeExtractor
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# FFmpeg-Kit
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Coil
-keep class coil.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# Genel Kotlin yansıma
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# R8 optimizasyonları için güvenli kural
-dontobfuscate

# Modeller (serileştirme güvenliği)
-keep class com.ytdownloader.app.domain.model.** { *; }
