# Project Index: TPDevTools (Teknasyon Android Studio Plugin)

Generated: 2026-02-21

---

## 📁 Proje Yapısı

```
Teknasyon-AndroidStudio-Plugin/
├── src/main/kotlin/com/github/teknasyon/plugin/
│   ├── TPToolWindowFactory.kt          ← Ana giriş noktası
│   ├── actions/                        ← IDE VCS aksiyonları
│   ├── common/                         ← Yardımcı araçlar
│   │   ├── file/                       ← Dosya işlemleri
│   │   ├── Constants.kt
│   │   ├── Extensions.kt
│   │   └── Utils.kt
│   ├── components/                     ← Ortak UI bileşenleri (TPxxx)
│   ├── data/                           ← Veri modelleri & repository
│   │   └── repository/
│   ├── domain/
│   │   ├── model/                      ← Domain modelleri
│   │   └── usecase/                    ← Use case'ler
│   ├── service/                        ← IDE servis katmanı
│   ├── theme/                          ← Tema sistemi (TPTheme, TPColor)
│   └── toolwindow/
│       ├── ai/                         ← SkillDock (Ana AI paneli)
│       ├── manager/
│       │   ├── ai/                     ← AI içerik ekranı
│       │   ├── featuregenerator/       ← Feature Generator
│       │   ├── jungle/                 ← Jungle içerik ekranı
│       │   ├── modulegenerator/        ← Module Generator
│       │   └── settings/               ← Ayarlar ekranı
│       └── template/                   ← Kod şablonları
├── src/main/resources/META-INF/plugin.xml
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
└── CHANGELOG.md
```

---

## 🚀 Giriş Noktaları

| Dosya | Amaç |
|-------|-------|
| `TPToolWindowFactory.kt` | Tool window fabrikası, 5 bölümlü ana UI'ı oluşturur |
| `plugin.xml` | Plugin tanımı, aksiyonlar, extension point'ler |
| `SkillDockPanel.kt` | SkillDock'un ana Composable UI'ı |
| `SkillDockViewModel.kt` | MVI mimarisi ViewModel'i |

---

## 📦 Çekirdek Modüller

### 1. Tool Window (`TPToolWindowFactory`)
- **Yol**: `src/main/kotlin/.../TPToolWindowFactory.kt`
- **Amaç**: IDE'nin sağ tarafına `TPDevTools` panel'ini bağlar
- **Bölümler**: AI Tools, Jungle, Module, Feature, Settings
- **Navigasyon**: `selectedSection` state'i ile bölümler arası geçiş

### 2. SkillDock – AI Araç Paneli
- **Yol**: `toolwindow/ai/`
- **Amaç**: Skill dosyalarını (.md) tarayıp Claude komutlarına dönüştürür
- **Sekmeler**: Skills, Agents, Commands
- **Ana dosyalar**:
  - `SkillDockPanel.kt` — UI composable
  - `SkillDockViewModel.kt` — İş mantığı & state yönetimi
  - `SkillDockState.kt` — `SkillDockState`, `TabState`, `SkillDockTab` enum
  - `SkillDockEvent.kt` — Tüm UI olayları
  - `SearchBar.kt` — Arama bileşeni
  - `SkillItemView.kt` — Skill liste elemanı
  - `CommandsTabView.kt` — Claude komutları sekmesi
  - `ReviewTrackerDialog.kt` / `ReviewResultPanel.kt` — PR review takibi

### 3. Domain Katmanı
- **Yol**: `domain/model/` ve `domain/usecase/`
- **Modeller**:
  - `Skill` — Skill dosyası (isim, commandName, filePath, isFavorite)
  - `SkillFolder` — Klasör içindeki skill'lerin grubu
  - `ReviewTask` / `ReviewChange` — PR review görevleri ve değişiklikler
- **Use Case'ler**:
  - `ScanSkillsUseCase` — Belirlenen root path'te .md dosyalarını tarar
  - `ExecuteSkillUseCase` — Seçilen skill'i terminalde çalıştırır
  - `ToggleFavoriteUseCase` — Skill'i favorilere ekler/çıkarır
  - `ProcessReviewCommentsUseCase` — PR yorumlarını işleyip görev listesi oluşturur

### 4. Servisler
- **Yol**: `service/`
- `SettingsService` — Global plugin ayarları (templates, export/import)
- `SkillDockSettingsService` — Proje bazlı ayarlar (rootPath, agentsPath, favorites) → `skilldock.xml`
- `SkillDockConfigurable` — IDE Preferences > Tools > SkillDock ayar sayfası
- `FileScanner` — Proje dosya sistemi tarayıcı
- `TerminalExecutor` / `TerminalExecutorImpl` — Terminalde komut çalıştırma

### 5. VCS Aksiyonları
- **Yol**: `actions/`
- `GenerateCommitMessageAction` — Staged değişikliklerden Claude ile commit mesajı oluşturur (VCS menüsü)
- `CreateReviewPRAction` — Review branch oluşturup PR açar

### 6. Module Generator
- **Yol**: `toolwindow/manager/modulegenerator/`
- Adımlar: Root seçimi → Modul tipi/ismi → Template seçimi → Plugin seçimi → Kütüphane seçimi → Dosya ağacı
- **Dialog**: `ModuleGeneratorDialog.kt`
- **Şablonlar**: `GradleTemplate`, `ManifestTemplate`, `GitIgnoreTemplate`, `ModuleReadMeTemplate`

### 7. Feature Generator
- **Yol**: `toolwindow/manager/featuregenerator/`
- Adımlar: Root seçimi → Konfigürasyon → Dosya ağacı
- **Dialog**: `FeatureGeneratorDialog.kt`

### 8. UI Bileşen Kütüphanesi (TP Prefix)
- **Yol**: `components/`
- `TPButton`, `TPCheckbox`, `TPTextField`, `TPText`
- `TPRadioButton`, `TPTabRow`, `TPActionCard`
- `TPFileTree`, `TPDropdownItem`, `TPDialogWrapper`

### 9. Tema Sistemi
- **Yol**: `theme/`
- `TPTheme.kt` — `MaterialTheme` wrapper + özel renkler
- `TPColor.kt` — Renk paleti (black, gray, blue, purple, white, lightGray)

---

## 🔧 Yapılandırma

| Dosya | Amaç |
|-------|-------|
| `gradle.properties` | Plugin ID, versiyon, platform tipi (AI), build aralığı |
| `build.gradle.kts` | Bağımlılıklar, IntelliJ Platform yapılandırması |
| `gradle/libs.versions.toml` | Versiyon kataloğu |
| `plugin.xml` | Plugin manifest: tool window, actions, configurables |

**Platform**: `AI` (Android Studio AI) — `platformVersion=2025.2.2.3`
**Build Aralığı**: `241` – `253.*`
**Plugin Versiyonu**: `0.0.1`

---

## 📚 Dokümantasyon

| Dosya | Konu |
|-------|-------|
| `README.md` | Kurulum talimatları (şablon README, güncellenmemiş) |
| `CHANGELOG.md` | Sürüm notları |

---

## 🧪 Test

| Yol | Tip |
|-----|-----|
| `.run/Run Tests.run.xml` | Gradle test çalıştırma konfigürasyonu |
| `.run/Run Plugin.run.xml` | Plugin'i sandbox IDE'de çalıştırma |
| `.run/Run Verifications.run.xml` | Plugin doğrulama |

Test framework: JUnit 4 (`junit:junit:4.13.2`), opentest4j

---

## 🔗 Temel Bağımlılıklar

| Bağımlılık | Versiyon | Amaç |
|-----------|---------|-------|
| `org.jetbrains.intellij.platform` | 2.11.0 | IntelliJ Platform Gradle Plugin |
| `org.jetbrains.kotlin.jvm` | 2.3.0 | Kotlin desteği |
| `org.jetbrains.compose` | 1.10.1 | Compose Desktop UI |
| `org.freemarker:freemarker` | 2.3.34 | Şablon motoru |
| `kotlinx-serialization-json` | 1.9.0 | JSON serileştirme |
| `compose.materialIconsExtended` | — | Material ikonları |

---

## 🏗️ Mimari Özeti

```
TPToolWindowFactory (Giriş)
    └─► MainContent (Composable)
         ├─► Sidebar (AI / Jungle / Module / Feature / Settings)
         └─► İçerik Alanı
              ├─► AiContent → SkillDockPanel
              │     ├─► SkillDockViewModel (MVI)
              │     │     ├─► ScanSkillsUseCase
              │     │     ├─► ExecuteSkillUseCase  ──► TerminalExecutor
              │     │     ├─► ToggleFavoriteUseCase ──► SkillDockSettingsService
              │     │     └─► ProcessReviewCommentsUseCase
              │     └─► UI: SearchBar, SkillGroupsList, CommandsTabView,
              │           ReviewTrackerDialog, ReviewResultPanel
              ├─► ModuleGeneratorContent
              ├─► FeatureGeneratorContent
              ├─► JungleContent
              └─► SettingsContent
```

**Veri akışı**: Event → ViewModel → UseCase → Repository/Service → State → UI
**UI Teknolojisi**: Jetpack Compose Desktop (Skiko tabanlı, SOFTWARE render)
**Durum yönetimi**: `MutableStateFlow` / `collectAsState()`

---

## 📝 Hızlı Başlangıç

1. **Geliştirme için çalıştır**: IntelliJ'de `Run Plugin` konfigürasyonunu çalıştır
2. **Test**: `Run Tests` konfigürasyonunu çalıştır
3. **Build**: `./gradlew buildPlugin`
4. **Yayınlama**: `./gradlew publishPlugin` (PUBLISH_TOKEN env değişkeni gerekli)

---

## 📌 Önemli Notlar

- `SkillDock`, `.md` uzantılı dosyaları `Skill` olarak tarar; `commandName` dosya adından türetilir (`/filename`)
- Composable'lar `ComposePanel` ile Swing'e gömülmüştür (`skiko.renderApi=SOFTWARE`)
- `SkillDockSettingsService` proje bazlı (`Service.Level.PROJECT`), `SettingsService` global
- PR review takibi Claude AI ile entegre çalışır (terminal üzerinden)
