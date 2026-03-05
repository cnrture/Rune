# Project Index: TPDevTools (Teknasyon AI)

Generated: 2026-03-05

---

## Project Structure

```
Teknasyon-IntelliJ-Plugin/
├── src/main/kotlin/com/github/teknasyon/plugin/
│   ├── actions/
│   │   ├── AskClaudeAction.kt                  ← Editor right-click → Claude'a gonder
│   │   ├── CreateReviewPRAction.kt              ← Review branch + PR olusturur
│   │   ├── FixPRCommentsAction.kt               ← PR yorumlarini Claude ile duzeltir
│   │   ├── GenerateCommitMessageAction.kt       ← Claude ile commit mesaji uretir
│   │   ├── SkillBestPracticesNotificationProvider.kt ← SKILL.md editor banner
│   │   └── dialog/
│   │       ├── CreatePRDialog.kt                ← PR olusturma dialogu
│   │       ├── CreateSkillDialog.kt             ← Yeni skill olusturma dialogu
│   │       └── FixPRCommentsDialog.kt           ← PR yorum duzeltme dialogu
│   ├── common/
│   │   ├── CliUtils.kt                          ← CLI yardimci fonksiyonlari
│   │   ├── Constants.kt                         ← Sabit degerler
│   │   ├── NoRippleTheme.kt                     ← Ripple efekt baskilama
│   │   ├── SkikoHelper.kt                       ← Skiko render ayarlari
│   │   └── VcsProvider.kt                       ← VCS saglayici enum/helper
│   ├── components/                              ← Ortak Compose bilesenler (TP prefix)
│   │   ├── TPActionCard.kt, TPButton.kt, TPCheckbox.kt
│   │   ├── TPDialogWrapper.kt, TPRadioButton.kt
│   │   ├── TPSwitch.kt, TPTabRow.kt
│   │   ├── TPText.kt, TPTextField.kt
│   ├── data/
│   │   ├── SkillRepository.kt                   ← Interface
│   │   └── SkillRepositoryImpl.kt               ← Skill tarama implementasyonu
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Skill.kt                         ← Markdown → komut modeli
│   │   │   └── SkillFolder.kt                   ← Agac yapisinda klasor modeli
│   │   └── usecase/
│   │       └── ScanSkillsUseCase.kt             ← Root path'te .md dosyalarini tarar
│   ├── service/
│   │   ├── BitbucketCloudPlatformService.kt     ← Bitbucket Cloud API
│   │   ├── BitbucketCredentialService.kt        ← Bitbucket kimlik yonetimi
│   │   ├── FileScanner.kt                       ← Dosya sistemi tarayici (5dk cache)
│   │   ├── GitHubPlatformService.kt             ← GitHub API islemleri
│   │   ├── JiraService.kt                       ← Jira entegrasyonu
│   │   ├── PluginConfigurable.kt                ← IDE Settings > Tools sayfasi
│   │   ├── PluginSettingsService.kt             ← Proje ayarlari (rootPath, agentsRootPath)
│   │   ├── VcsCacheService.kt                   ← VCS veri cache'i
│   │   └── VcsPlatformService.kt                ← VCS platform soyutlamasi (GitHub/Bitbucket)
│   ├── theme/
│   │   ├── TPTheme.kt                           ← MaterialTheme wrapper
│   │   └── TPColor.kt                           ← Renk paleti
│   └── toolwindow/
│       ├── ClaudeInstallGuide.kt                ← Claude CLI kurulum rehberi
│       ├── ClaudeSessionService.kt              ← Coklu terminal oturum yonetimi
│       ├── ClaudeTerminalContent.kt             ← Ana Claude UI (terminal + buttons + input)
│       ├── ClaudeToolWindowFactory.kt           ← "Claude" tool window fabrikasi
│       ├── ImagePreviewDialog.kt                ← Gorsel onizleme dialogu
│       ├── RemoteControlDialog.kt               ← Uzaktan kontrol dialogu
│       ├── SessionTabBar.kt                     ← Oturum sekme cubugu
│       ├── TerminalInputBar.kt                  ← Terminal giris cubugu
│       └── UnifiedCommandPalette.kt             ← Komut/skill/agent secici
├── src/main/resources/
│   ├── META-INF/plugin.xml
│   └── icons/pluginIcon.svg
├── .github/workflows/ (build.yml, release.yml, run-ui-tests.yml)
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
└── CHANGELOG.md
```

**Toplam**: 47 Kotlin dosyasi

---

## Entry Points

| Dosya | Amac |
|-------|------|
| `ClaudeToolWindowFactory.kt` | "Claude" tool window — Gomulu Claude CLI terminali |
| `plugin.xml` | Plugin manifest: 1 tool window, 5 action, 1 configurable |

---

## Core Modules

### 1. Claude Terminal (`toolwindow/`)
- **ClaudeSessionService**: Coklu terminal oturumu yonetimi (create, switch, close)
- **ClaudeTerminalContent**: Ana UI — SessionTabBar, SwingPanel terminal, ActionButtons, TerminalInputBar
- **UnifiedCommandPalette**: Skills/Agents/Commands secici dialog
- **ClaudeInstallGuide**: Claude CLI kurulu degilse rehber ekrani
- **SessionTabBar**: Oturum sekme cubugu bileseni
- **TerminalInputBar**: Terminal giris cubugu (gorsel ekleme, @ dosya, Enter gonderme)

### 2. IDE Actions (`actions/`)
- **GenerateCommitMessageAction**: Staged diff → Claude `-p` → commit mesaji (VCS menu)
- **CreateReviewPRAction**: Auto base branch → review branch → PR olusturma
- **FixPRCommentsAction**: Cozulmemis PR yorumlarini Claude ile duzeltme
- **AskClaudeAction**: Editor sag-tik → secili kodu Claude terminal'a gonderir
- **SkillBestPracticesNotificationProvider**: SKILL.md dosyalari icin editor banner

### 3. VCS Platform Abstraction (`service/`)
- **VcsPlatformService**: GitHub ve Bitbucket icin ortak arayuz
- **GitHubPlatformService**: GitHub API (PR, yorum, branch islemleri)
- **BitbucketCloudPlatformService**: Bitbucket Cloud API
- **BitbucketCredentialService**: Bitbucket kimlik bilgisi yonetimi
- **VcsCacheService**: VCS verilerini onbellege alma

### 4. Domain Layer (`domain/`)
- **Skill**: `name`, `commandName`, `description`, `filePath`, `isFavorite`
- **SkillFolder**: Agac yapisi — `skills + subFolders`, recursive traversal
- **ScanSkillsUseCase**: Root path'te `.md` dosyalarini tarar

### 5. Data Layer (`data/`)
- **SkillRepositoryImpl**: `FileScanner` delegasyonu, Result wrapping

### 6. Services (`service/`)
- **FileScanner**: VirtualFile ile `.md` tarama, 5dk in-memory cache
  - `strictFilter=true` → sadece `SKILL.md` (Skills sekmesi)
  - `strictFilter=false` → tum `.md` (Agents sekmesi)
- **PluginSettingsService** (PROJECT-scoped): `rootPath`, `agentsRootPath`
- **JiraService**: Branch adindan ticket ID cikarimi

### 7. UI Component Library (`components/`)
- TP prefix: `TPButton`, `TPCheckbox`, `TPTextField`, `TPText`, `TPRadioButton`
- `TPTabRow`, `TPActionCard`, `TPSwitch`, `TPDialogWrapper`

### 8. Theme (`theme/`)
- `TPTheme`: MaterialTheme wrapper, `TPTheme.colors.*` erisimi
- `TPColor`: Renk paleti (black, gray, blue, purple, white, red, vb.)

---

## Configuration

| Dosya | Amac |
|-------|------|
| `gradle.properties` | `pluginGroup=com.github.teknasyon.plugin`, `platformType=IC`, `platformVersion=2024.3.1`, `pluginVersion=0.0.6` |
| `build.gradle.kts` | Compose Desktop, IntelliJ Platform, cross-OS Skiko natives |
| `gradle/libs.versions.toml` | Kotlin 2.3.10, IntelliJ Platform 2.11.0, Compose 1.10.1 |
| `plugin.xml` | 1 toolWindow (Claude), 5 action, 1 configurable, terminal bagimliligi |

**Build araligi**: `241` – `253.*` | **JVM**: Java 21

---

## Key Dependencies

| Bagimlilik | Versiyon | Amac |
|-----------|---------|------|
| IntelliJ Platform Gradle Plugin | 2.11.0 | Plugin build sistemi |
| Kotlin JVM | 2.3.10 | Dil destegi |
| Jetpack Compose Desktop | 1.10.1 | UI framework (Skiko SOFTWARE render) |
| FreeMarker | 2.3.34 | Sablon motoru |
| kotlinx-serialization-json | 1.10.0 | JSON serilestirme |
| Compose Material Icons Extended | — | Ikon kutuphanesi |

---

## CI/CD

| Workflow | Amac |
|----------|------|
| `build.yml` | Push/PR → build + test + Qodana + Plugin Verifier |
| `release.yml` | GitHub release → ZIP deploy + updatePlugins.xml guncelleme |
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
  ├── GitHubPlatformService
  └── BitbucketCloudPlatformService

Data Flow:
  UI → UseCase → Repository → FileScanner/Settings → State → UI
```

**UI**: Jetpack Compose Desktop gomulu (ComposePanel → JPanel)
**Terminal**: JBTerminalWidget (LocalTerminalDirectRunner) ile `claude` CLI
**Render**: `skiko.renderApi=SOFTWARE`

---

## Quick Start

```bash
./gradlew runIde          # Sandbox IDE'de calistir
./gradlew check           # Testleri calistir
./gradlew buildPlugin     # Dagitim ZIP olustur
./gradlew publishPlugin   # Marketplace'e yayinla
```
