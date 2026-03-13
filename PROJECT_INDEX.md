# Project Index: TPDevTools (Teknasyon AI)

Generated: 2026-03-13

---

## Project Structure

```
src/main/kotlin/com/github/teknasyon/plugin/
├── actions/
│   ├── AskClaudeAction.kt                  ← Editor right-click → Claude'a gönder
│   ├── CheckSkillBestPracticesAction.kt     ← SKILL.md validasyonu
│   ├── CreateReviewPRAction.kt              ← Review branch + PR oluşturur
│   ├── FixPRCommentsAction.kt               ← PR yorumlarını Claude ile düzeltir
│   ├── GenerateCommitMessageAction.kt       ← Claude ile commit mesajı üretir
│   ├── SkillBestPracticesNotificationProvider.kt ← SKILL.md editor banner
│   └── dialog/
│       ├── CreatePRDialog.kt                ← PR oluşturma dialogu
│       ├── CreateSkillDialog.kt             ← Yeni skill oluşturma dialogu
│       └── FixPRCommentsDialog.kt           ← PR yorum düzeltme dialogu
├── common/
│   ├── AppIcons.kt                          ← SVG ikon yükleme
│   ├── CliUtils.kt                          ← CLI çözümleme + process yürütme
│   ├── Constants.kt                         ← Timeout, delay, sabit değerler
│   ├── NoRippleTheme.kt                     ← Ripple efekt baskılama
│   ├── SkikoHelper.kt                       ← Skiko render ayarları
│   └── VcsProvider.kt                       ← VCS sağlayıcı enum/helper
├── components/                              ← Ortak Compose bileşenler (TP prefix)
│   ├── TPActionCard.kt, TPButton.kt, TPCheckbox.kt
│   ├── TPDialogWrapper.kt, TPSwitch.kt
│   ├── TPText.kt, TPTextField.kt
├── data/
│   ├── SkillRepository.kt                   ← Interface
│   └── SkillRepositoryImpl.kt               ← Skill tarama implementasyonu
├── domain/
│   ├── model/
│   │   ├── Skill.kt                         ← Markdown → komut modeli
│   │   └── SkillFolder.kt                   ← Ağaç yapısında klasör modeli
│   └── usecase/
│       └── ScanSkillsUseCase.kt             ← Root path'te .md dosyalarını tarar
├── service/
│   ├── BitbucketCloudPlatformService.kt     ← Bitbucket Cloud API
│   ├── BitbucketCredentialService.kt        ← Bitbucket kimlik yönetimi
│   ├── FileScanner.kt                       ← Dosya sistemi tarayıcı (5dk cache)
│   ├── GitHubApiClient.kt                   ← GitHub REST API client
│   ├── GitHubCredentialService.kt           ← GitHub kimlik yönetimi
│   ├── GitHubPlatformService.kt             ← GitHub API işlemleri
│   ├── JiraService.kt                       ← Jira entegrasyonu
│   ├── PluginConfigurable.kt               ← IDE Settings > Tools sayfası
│   ├── PluginSettingsService.kt             ← Proje ayarları (rootPath, agentsRootPath)
│   ├── VcsCacheService.kt                   ← VCS veri cache'i
│   └── VcsPlatformService.kt               ← VCS platform soyutlaması (GitHub/Bitbucket)
├── theme/
│   ├── TPColor.kt                           ← Renk paleti
│   └── TPTheme.kt                           ← MaterialTheme wrapper
└── toolwindow/
    ├── ClaudeInstallGuide.kt                ← Claude CLI kurulum rehberi
    ├── ClaudeSessionService.kt              ← Çoklu terminal oturum yönetimi
    ├── ClaudeTerminalContent.kt             ← Ana Claude UI (terminal + buttons + input)
    ├── ClaudeToolWindowFactory.kt           ← "Claude" tool window fabrikası
    ├── ImagePreviewDialog.kt                ← Görsel önizleme dialogu
    ├── ModelPickerDialog.kt                 ← Claude model seçim dialogu
    ├── RemoteControlDialog.kt               ← Uzaktan kontrol / action card dialogu
    ├── SessionTabBar.kt                     ← Oturum sekme çubuğu
    ├── TerminalInputBar.kt                  ← Terminal giriş çubuğu
    └── UnifiedCommandPalette.kt             ← Komut/skill/agent seçici

src/main/resources/
├── META-INF/plugin.xml                      ← Plugin manifest
└── icons/ (68 SVG)                          ← Material Design ikonları

.github/workflows/
├── build.yml                                ← CI: build + test + Qodana + Verifier
├── release.yml                              ← CD: publish plugin ZIP
└── run-ui-tests.yml                         ← Manual: robot server UI tests
```

**Toplam**: 48 Kotlin dosyası

---

## Entry Points

| Dosya | Amaç |
|-------|------|
| `ClaudeToolWindowFactory.kt` | "Claude" tool window — Gömülü Claude CLI terminali |
| `plugin.xml` | Plugin manifest: 1 tool window, 5 action, 1 configurable |

---

## Core Modules

### 1. Claude Terminal (`toolwindow/` — 10 dosya)
- **ClaudeSessionService**: Çoklu terminal oturumu yönetimi (create, switch, close)
- **ClaudeTerminalContent**: Ana UI — SessionTabBar, SwingPanel terminal, ActionButtons, TerminalInputBar
- **UnifiedCommandPalette**: Skills/Agents/Commands seçici dialog
- **ClaudeInstallGuide**: Claude CLI kurulu değilse rehber ekranı
- **SessionTabBar**: Oturum sekme çubuğu bileşeni
- **TerminalInputBar**: Terminal giriş çubuğu (görsel ekleme, @ dosya, Enter gönderme)
- **ModelPickerDialog**: Claude model seçim dialogu
- **RemoteControlDialog**: Uzaktan kontrol action card'ları
- **ImagePreviewDialog**: Görsel önizleme

### 2. IDE Actions (`actions/` — 8 dosya)
- **GenerateCommitMessageAction**: Staged diff → Claude `-p` → commit mesajı (VCS menu)
- **CreateReviewPRAction**: Auto base branch → review branch → PR oluşturma
- **FixPRCommentsAction**: Çözülmemiş PR yorumlarını Claude ile düzeltme
- **AskClaudeAction**: Editor sağ-tık → seçili kodu Claude terminal'a gönderir
- **SkillBestPracticesNotificationProvider**: SKILL.md dosyaları için editor banner
- **dialog/**: CreatePRDialog, CreateSkillDialog, FixPRCommentsDialog

### 3. VCS Platform Abstraction (`service/`)
- **VcsPlatformService**: GitHub ve Bitbucket için ortak arayüz
- **GitHubPlatformService**: GitHub PR, yorum, branch işlemleri
- **GitHubApiClient**: GitHub REST API client
- **GitHubCredentialService**: GitHub kimlik bilgisi yönetimi
- **BitbucketCloudPlatformService**: Bitbucket Cloud API
- **BitbucketCredentialService**: Bitbucket kimlik bilgisi yönetimi
- **VcsCacheService**: VCS verilerini önbelleğe alma

### 4. Domain Layer (`domain/`)
- **Skill**: `name`, `commandName`, `description`, `filePath`, `isFavorite`
- **SkillFolder**: Ağaç yapısı — `skills + subFolders`, recursive traversal
- **ScanSkillsUseCase**: Root path'te `.md` dosyalarını tarar

### 5. Data Layer (`data/`)
- **SkillRepositoryImpl**: `FileScanner` delegasyonu, Result wrapping

### 6. Services (`service/`)
- **FileScanner**: VirtualFile ile `.md` tarama, 5dk in-memory cache
  - `strictFilter=true` → sadece `SKILL.md` (Skills sekmesi)
  - `strictFilter=false` → tüm `.md` (Agents sekmesi)
- **PluginSettingsService** (PROJECT-scoped): `rootPath`, `agentsRootPath`
- **JiraService**: Branch adından ticket ID çıkarımı

### 7. UI Component Library (`components/` — 7 dosya)
TP prefix: `TPButton`, `TPCheckbox`, `TPTextField`, `TPText`, `TPSwitch`, `TPActionCard`, `TPDialogWrapper`

### 8. Theme (`theme/`)
- `TPTheme`: MaterialTheme wrapper, `TPTheme.colors.*` erişimi
- `TPColor`: Renk paleti (black, gray, blue, purple, white, red, vb.)

---

## Configuration

| Dosya | Amaç |
|-------|------|
| `gradle.properties` | `pluginGroup=com.github.teknasyon.plugin`, `platformType=IC`, `platformVersion=2024.3.1`, `pluginVersion=0.0.6` |
| `build.gradle.kts` | Compose Desktop, IntelliJ Platform, cross-OS Skiko natives |
| `gradle/libs.versions.toml` | Kotlin 2.3.10, IntelliJ Platform 2.11.0, Compose 1.10.1 |
| `plugin.xml` | 1 toolWindow (Claude), 5 action, 1 configurable, terminal bağımlılığı |

**Build aralığı**: `241` – `253.*` | **JVM**: Java 21

---

## Key Dependencies

| Bağımlılık | Versiyon | Amaç |
|-----------|---------|------|
| IntelliJ Platform Gradle Plugin | 2.11.0 | Plugin build sistemi |
| Kotlin JVM | 2.3.10 | Dil desteği |
| Jetpack Compose Desktop | 1.10.1 | UI framework (Skiko SOFTWARE render) |
| FreeMarker | 2.3.34 | Şablon motoru |
| kotlinx-serialization-json | 1.10.0 | JSON serileştirme |

---

## CI/CD

| Workflow | Amaç |
|----------|------|
| `build.yml` | Push/PR → build + test + Qodana + Plugin Verifier |
| `release.yml` | GitHub release → ZIP deploy + updatePlugins.xml güncelleme |
| `run-ui-tests.yml` | Manuel → Robot server UI testleri (port 8082) |

---

## Architecture Summary

```
plugin.xml
  └── ClaudeToolWindowFactory (right panel) ── Claude Terminal
        └── ClaudeSessionService (multi-session)
              └── JBTerminalWidget + CardLayout

Actions (VCS menu / Editor popup):
  ├── GenerateCommitMessageAction ── claude -p "diff" → commit msg
  ├── CreateReviewPRAction ── git + gh CLI → review PR
  ├── FixPRCommentsAction ── PR comments → Claude fix
  ├── AskClaudeAction ── selected code → Claude terminal
  └── CheckSkillBestPracticesAction ── SKILL.md validation

VCS Abstraction:
  VcsPlatformService (interface)
  ├── GitHubPlatformService + GitHubApiClient + GitHubCredentialService
  └── BitbucketCloudPlatformService + BitbucketCredentialService

Data Flow:
  UI → UseCase → Repository → FileScanner/Settings → State → UI
```

**UI**: Jetpack Compose Desktop gömülü (ComposePanel → JPanel)
**Terminal**: JBTerminalWidget (LocalTerminalDirectRunner) ile `claude` CLI
**Render**: `skiko.renderApi=SOFTWARE`

---

## Quick Start

```bash
./gradlew runIde          # Sandbox IDE'de çalıştır
./gradlew check           # Testleri çalıştır
./gradlew buildPlugin     # Dağıtım ZIP oluştur
./gradlew publishPlugin   # Marketplace'e yayınla
```
