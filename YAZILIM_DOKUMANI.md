# YouTube Video Downloader — Yazılım Dokümanı

**Platform:** Android
**Tür:** Video indirme uygulaması (144p → 4K)
**Doküman sürümü:** 1.0
**Tarih:** 2026-06-18

---

## İçindekiler

1. [Genel Bakış](#1-genel-bakış)
2. [Yasal Uyarı ve Uyumluluk](#2-yasal-uyarı-ve-uyumluluk)
3. [Hedef Kitle ve Kullanım Senaryoları](#3-hedef-kitle-ve-kullanım-senaryoları)
4. [Fonksiyonel Gereksinimler](#4-fonksiyonel-gereksinimler)
5. [Fonksiyonel Olmayan Gereksinimler](#5-fonksiyonel-olmayan-gereksinimler)
6. [Sistem Mimarisi](#6-sistem-mimarisi)
7. [Teknoloji Yığını](#7-teknoloji-yığını)
8. [Modül Tasarımı](#8-modül-tasarımı)
9. [Video Kalite ve Format Yönetimi](#9-video-kalite-ve-format-yönetimi)
10. [Veri Modeli ve Yerel Depolama](#10-veri-modeli-ve-yerel-depolama)
11. [Kullanıcı Arayüzü Akışı](#11-kullanıcı-arayüzü-akışı)
12. [İzinler ve Güvenlik](#12-izinler-ve-güvenlik)
13. [Hata Yönetimi](#13-hata-yönetimi)
14. [Performans ve Optimizasyon](#14-performans-ve-optimizasyon)
15. [Test Stratejisi](#15-test-stratejisi)
16. [Derleme, Paketleme ve Dağıtım](#16-derleme-paketleme-ve-dağıtım)
17. [Yol Haritası](#17-yol-haritası)

---

## 1. Genel Bakış

Bu uygulama, kullanıcıların bir video bağlantısını yapıştırarak veya uygulama içi tarayıcıdan paylaşarak videoları cihazlarına indirmesini sağlayan bir Android uygulamasıdır. İndirme kalitesi **144p, 240p, 360p, 480p, 720p, 1080p (Full HD), 1440p (2K) ve 2160p (4K)** seçenekleri arasında değişir. Ayrıca yalnızca ses (MP3/M4A) çıkarma desteği bulunur.

### Temel Özellikler

- URL yapıştırma veya "Paylaş" menüsü ile içe aktarma
- Kalite ve format seçimi (video + ses birleştirme)
- Çoklu eşzamanlı indirme kuyruğu
- Duraklat / devam ettir / yeniden dene
- Arka planda indirme (foreground service + bildirim)
- İndirilenler kütüphanesi ve dahili oynatıcı
- Karanlık / aydınlık tema

> **Not:** Uygulama, geçerli ToS ve telif kurallarına uygun içeriklerin (kullanıcının kendi içeriği, telifsiz/Creative Commons, izin verilen kaynaklar) indirilmesi amacıyla tasarlanmalıdır. Bkz. [Bölüm 2](#2-yasal-uyarı-ve-uyumluluk).

---

## 2. Yasal Uyarı ve Uyumluluk

> ⚠️ **Önemli:** YouTube'un Hizmet Şartları, açık izin olmadan içerik indirmeyi genellikle yasaklar. Google Play Store geliştirici politikaları da üçüncü taraf platform içeriğini izinsiz indiren uygulamaları reddedebilir.

Bu doküman bir **mühendislik referansıdır**, yasal bir tavsiye değildir. Üretim/dağıtım öncesinde aşağıdakiler değerlendirilmelidir:

| Konu | Sorumluluk |
|------|-----------|
| Telif hakkı | İndirilen içeriğin telif durumunu kullanıcı doğrulamalı |
| Platform ToS | YouTube ToS ihlali riski; izinli/kendi içerik tavsiye edilir |
| Mağaza politikası | Google Play reddi olası → APK harici (sideload) dağıtım |
| Veri gizliliği | KVKK/GDPR uyumu, yerel veri saklama |

**Önerilen kullanım:** Kullanıcının kendi yüklediği videolar, Creative Commons lisanslı içerik, telifsiz materyaller veya platform sahibinden açık izin alınmış içerikler.

---

## 3. Hedef Kitle ve Kullanım Senaryoları

**Hedef kullanıcı:** Çevrimdışı izleme için video kaydetmek isteyen, kendi içeriğini yedeklemek isteyen veya eğitim/CC içeriği arşivleyen Android kullanıcıları.

**Kullanım senaryoları:**

1. **US-01:** Kullanıcı bir bağlantı yapıştırır → kaliteyi seçer → indirir.
2. **US-02:** Kullanıcı başka uygulamadan "Paylaş → İndir" yapar.
3. **US-03:** Kullanıcı yalnızca sesi (MP3) indirir.
4. **US-04:** Kullanıcı 4K indirme başlatır, uygulamayı kapatır, indirme arka planda sürer.
5. **US-05:** Bağlantı koptu → uygulama indirmeyi otomatik yeniden dener.

---

## 4. Fonksiyonel Gereksinimler

| ID | Gereksinim | Öncelik |
|----|-----------|---------|
| FR-01 | Video URL'sini doğrulama ve meta veri (başlık, süre, küçük resim) getirme | Yüksek |
| FR-02 | Mevcut kaliteleri listeleme (144p–2160p) | Yüksek |
| FR-03 | Seçilen kalitede video akışını indirme | Yüksek |
| FR-04 | DASH akışlarında ayrı video+ses dosyalarını birleştirme (mux) | Yüksek |
| FR-05 | Yalnızca ses çıkarma (MP3/M4A) | Orta |
| FR-06 | İndirme kuyruğu + eşzamanlılık kontrolü | Yüksek |
| FR-07 | Duraklat / devam / iptal / yeniden dene | Yüksek |
| FR-08 | Arka plan indirme (foreground service) ve ilerleme bildirimi | Yüksek |
| FR-09 | İndirilenler kütüphanesi (liste, sil, paylaş, oynat) | Orta |
| FR-10 | Tema, varsayılan kalite, indirme klasörü ayarları | Düşük |

---

## 5. Fonksiyonel Olmayan Gereksinimler

- **Performans:** 4K indirme sırasında UI 60 FPS akıcı kalmalı; indirme I/O ayrı thread'de.
- **Güvenilirlik:** Ağ kesintisinde resume (HTTP Range) desteği; %99 yeniden başlatma başarısı.
- **Uyumluluk:** Android 8.0 (API 26) → Android 15 (API 35).
- **Depolama:** Scoped Storage (Android 10+) uyumlu, MediaStore API kullanımı.
- **Erişilebilirlik:** TalkBack, dinamik yazı tipi boyutu.
- **Yerelleştirme:** TR + EN, genişletilebilir string kaynakları.
- **Boyut:** APK < 50 MB (FFmpeg dahil split ABI ile).

---

## 6. Sistem Mimarisi

Önerilen mimari: **MVVM + Clean Architecture (3 katman)**.

```
┌─────────────────────────────────────────────┐
│              Presentation (UI)               │
│   Jetpack Compose · ViewModel · StateFlow    │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│                Domain                        │
│   UseCases · Repository Interface · Models   │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│                  Data                        │
│  Extractor · Downloader · Muxer · Room DB    │
│  (OkHttp · WorkManager · FFmpeg · MediaStore)│
└─────────────────────────────────────────────┘
```

**Veri akışı:**

```
URL → [Extractor] → stream formats
    → [Quality Selector / UseCase]
    → [Download Engine] → video.mp4 + audio.m4a
    → [Muxer (FFmpeg)] → final.mp4
    → [MediaStore] → Galeri/İndirilenler
    → [Room] → kütüphane kaydı
```

---

## 7. Teknoloji Yığını

| Katman | Teknoloji |
|--------|-----------|
| Dil | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Mimari | MVVM, Hilt (DI) |
| Async | Coroutines + Flow |
| Ağ | OkHttp / Retrofit |
| İndirme zamanlama | WorkManager + Foreground Service |
| Akış ayıklama | youtubedl-android (yt-dlp sarmalayıcı) veya NewPipeExtractor |
| Video birleştirme | FFmpeg-Kit (mobile-ffmpeg) |
| Yerel DB | Room |
| Depolama | MediaStore / Storage Access Framework |
| Oynatıcı | Media3 (ExoPlayer) |

> **Mimari karar:** Akış ayıklamada iki yaklaşım vardır:
> - **NewPipeExtractor** — saf Java/Kotlin, FFmpeg gerektirir, hafif.
> - **youtubedl-android (yt-dlp)** — Python tabanlı, çok güçlü, APK boyutunu büyütür.
> 144p–4K tüm formatları kapsamak için DASH desteği şarttır; her iki yaklaşım da ayrı video+ses akışını birleştirme (mux) gerektirir.

---

## 8. Modül Tasarımı

### 8.1 Extractor Modülü
- Girdi: video URL
- Çıktı: `VideoInfo(title, duration, thumbnail, List<StreamFormat>)`
- `StreamFormat(itag, resolution, mimeType, hasAudio, hasVideo, url, sizeBytes)`

### 8.2 Download Engine
- HTTP Range tabanlı parçalı indirme (segment indirme)
- Resume desteği (kısmi dosya + offset)
- İlerleme callback'i: `downloadedBytes / totalBytes`
- Eşzamanlılık: max 3 paralel indirme (yapılandırılabilir)

### 8.3 Muxer Modülü
- 1080p üzeri (DASH) akışlarda video ve ses ayrı gelir.
- FFmpeg komutu (kopyalama, yeniden kodlama yok — hızlı):
  ```
  -i video.mp4 -i audio.m4a -c copy -map 0:v:0 -map 1:a:0 output.mp4
  ```
- Ses çıkarma:
  ```
  -i input.m4a -vn -acodec libmp3lame -q:a 2 output.mp3
  ```

### 8.4 Storage Manager
- Android 10+ → MediaStore (`Environment.DIRECTORY_MOVIES`)
- Android 9- → doğrudan dosya yolu + `WRITE_EXTERNAL_STORAGE`

### 8.5 Library / DB Modülü
- Room ile indirilen dosya kayıtları, durum takibi.

---

## 9. Video Kalite ve Format Yönetimi

| Etiket | Çözünürlük | Tip | Ses durumu | Birleştirme |
|--------|-----------|-----|-----------|-------------|
| 144p | 256×144 | progressive/DASH | bazen ayrı | gerekebilir |
| 240p | 426×240 | progressive | birlikte | hayır |
| 360p | 640×360 | progressive | birlikte | hayır |
| 480p | 854×480 | DASH | ayrı | **evet** |
| 720p | 1280×720 | progressive/DASH | değişken | bazen |
| 1080p | 1920×1080 | DASH | ayrı | **evet** |
| 1440p (2K) | 2560×1440 | DASH | ayrı | **evet** |
| 2160p (4K) | 3840×2160 | DASH | ayrı | **evet** |

**Önemli kural:** 1080p ve üzeri çözünürlükler genellikle yalnızca DASH (video-only) akışı olarak sunulur. Bu nedenle 4K indirme için **video akışı + en iyi ses akışı ayrı indirilip FFmpeg ile birleştirilmelidir**.

**Kalite seçim algoritması (pseudo):**
```kotlin
fun pickStreams(formats: List<StreamFormat>, target: Resolution): Pair<StreamFormat, StreamFormat?> {
    val video = formats.filter { it.hasVideo }
        .filter { it.resolution <= target }
        .maxByOrNull { it.resolution }!!
    val audio = if (!video.hasAudio)
        formats.filter { it.hasAudio && !it.hasVideo }.maxByOrNull { it.bitrate }
    else null
    return video to audio
}
```

---

## 10. Veri Modeli ve Yerel Depolama

```kotlin
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,        // UUID
    val title: String,
    val sourceUrl: String,
    val filePath: String,
    val resolution: String,            // "2160p"
    val format: String,                // "mp4" | "mp3"
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,        // QUEUED, RUNNING, PAUSED, COMPLETED, FAILED
    val createdAt: Long,
    val thumbnailUrl: String?
)

enum class DownloadStatus { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED }
```

---

## 11. Kullanıcı Arayüzü Akışı

**Ekranlar:**

1. **Ana Ekran** — URL giriş alanı, "Yapıştır" butonu, kuyruk önizleme.
2. **Kalite Seçim Bottom Sheet** — küçük resim, başlık, kalite listesi, "Sadece ses" seçeneği, tahmini boyut.
3. **İndirme Kuyruğu** — ilerleme çubukları, duraklat/iptal.
4. **Kütüphane** — indirilenler grid'i, oynat/paylaş/sil.
5. **Ayarlar** — varsayılan kalite, klasör, tema, eşzamanlı indirme sayısı.

**Akış:**
```
Ana Ekran → URL gir → (Extractor çalışır) → Kalite Sheet
   → Kalite seç → İndirme başlar → Kuyruk ekranı
   → Tamamlandı → Bildirim + Kütüphaneye eklenir
```

---

## 12. İzinler ve Güvenlik

**AndroidManifest izinleri:**
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<!-- Android 9 ve altı için -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28"/>
```

**Güvenlik notları:**
- HTTPS zorunlu (cleartext trafiği kapat).
- Kullanıcı verisi yalnızca cihazda; sunucuya gönderilmez.
- ProGuard/R8 ile kod küçültme ve gizleme.
- Üçüncü taraf kütüphaneleri (FFmpeg, extractor) düzenli güncelle (CVE takibi).

---

## 13. Hata Yönetimi

| Hata | Sebep | Davranış |
|------|-------|----------|
| `ExtractionFailedException` | URL geçersiz / extractor güncel değil | Kullanıcıya mesaj + yeniden dene |
| `NetworkException` | Bağlantı koptu | Otomatik retry (exponential backoff), resume |
| `StorageFullException` | Disk dolu | İndirmeyi durdur, uyarı göster |
| `MuxingFailedException` | FFmpeg hatası | Ham dosyaları koru, log topla |
| `PermissionDeniedException` | İzin yok | İzin diyaloğu yönlendirme |

Retry politikası: 3 deneme, 2s → 4s → 8s backoff.

---

## 14. Performans ve Optimizasyon

- **Parçalı indirme:** Büyük 4K dosyaları (>1GB) için segment paralelliği.
- **Buffer:** 8–64 KB okuma buffer'ı, doğrudan disk akışı (bellekte tutma).
- **Mux hızlandırma:** `-c copy` ile yeniden kodlamadan birleştirme.
- **Bildirim throttle:** İlerleme bildirimini saniyede 1 güncelle (pil tasarrufu).
- **APK boyutu:** Split ABI (arm64-v8a, armeabi-v7a) ile FFmpeg yükünü azalt.

---

## 15. Test Stratejisi

| Seviye | Kapsam | Araç |
|--------|--------|------|
| Birim | UseCase, kalite seçim algoritması, parser | JUnit, MockK |
| Entegrasyon | Extractor + Downloader + Muxer zinciri | Robolectric |
| UI | Ekran akışları, Compose | Compose UI Test, Espresso |
| Manuel | Gerçek cihazda 144p–4K, arka plan, kesinti | — |

**Kritik test senaryoları:**
- 4K DASH indir → mux → oynat (ses senkron mu?)
- Ağ kesilip geri geldiğinde resume çalışıyor mu?
- Uygulama kill edilince foreground service sürüyor mu?
- Düşük depolamada davranış.

---

## 16. Derleme, Paketleme ve Dağıtım

**Derleme:**
```bash
./gradlew assembleRelease        # APK
./gradlew bundleRelease          # AAB (Play için)
```

**İmzalama:** `keystore` ile release imzalama, `signingConfigs` tanımı.

**Dağıtım seçenekleri:**
1. **Sideload APK** — doğrudan APK dağıtımı (Play politikası riski nedeniyle muhtemel yol).
2. **F-Droid** — açık kaynaksa.
3. **Play Store** — yüksek ret riski (Bölüm 2).

**Sürümleme:** Semantic versioning (`MAJOR.MINOR.PATCH`), `versionCode` artışı.

---

## 17. Yol Haritası

| Aşama | İçerik |
|-------|--------|
| v1.0 | URL indirme, 144p–4K, kuyruk, kütüphane |
| v1.1 | Yalnızca ses (MP3), altyazı indirme |
| v1.2 | Playlist toplu indirme |
| v1.3 | Dahili oynatıcı geliştirmeleri, dosya yöneticisi |
| v2.0 | Bulut yedekleme, çoklu kaynak desteği |

---

## Ek: Klasör Yapısı (Önerilen)

```
app/
├── data/
│   ├── extractor/      # akış ayıklama
│   ├── downloader/     # indirme motoru
│   ├── muxer/          # FFmpeg birleştirme
│   ├── db/             # Room
│   └── repository/
├── domain/
│   ├── model/
│   ├── usecase/
│   └── repository/     # interface
├── presentation/
│   ├── home/
│   ├── quality/
│   ├── queue/
│   ├── library/
│   └── settings/
├── service/            # ForegroundService
└── di/                 # Hilt modülleri
```

---

*Bu doküman bir mühendislik referansıdır. Üretim öncesi yasal uyumluluk ([Bölüm 2](#2-yasal-uyarı-ve-uyumluluk)) mutlaka değerlendirilmelidir.*
