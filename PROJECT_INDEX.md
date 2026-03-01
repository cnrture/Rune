# Project Index: Rune

Generated: 2026-03-01

---

## Project Structure

```
Rune/
├── src/main/kotlin/com/github/cnrture/rune/
│   ├── actions/
│   │   ├── AskClaudeAction.kt                  ← Editor sag-tik → seçili kodu Claude'a gönderir
│   │   ├── GenerateCommitMessageAction.kt       ← Claude CLI ile commit mesaji olusturur
│   │   ├── CreateReviewPRAction.kt              ← Review branch + PR olusturur
│   │   ├── FixPRCommentsAction.kt               ← PR review yorumlarini otomatik düzeltir
│   │   ├── SkillBestPracticesNotificationProvider.kt ← SKILL.md dosyalari icin editor banner
│   │   └── dialog/
│   │       ├── CreatePRDialog.kt                ← PR olusturma dialogu (GitHub entegrasyonu)
│   │       ├── CreateSkillDialog.kt             ← Skill dosyasi olusturma dialogu
│   │       └── FixPRCommentsDialog.kt           ← PR yorumlari düzeltme dialogu
│   ├── common/
│   │   ├── Constants.kt                         ← Global sabitler
│   │   └── NoRippleTheme.kt                     ← Compose ripple temasi
│   ├── components/                              ← Ortak Compose bilesenleri (R prefix)
│   │   ├── RActionCard.kt                       ← Eylem karti bileseni
│   │   ├── RCheckbox.kt                         ← Checkbox bileseni
│   │   ├── RDialogWrapper.kt                    ← Dialog wrapper bileseni
│   │   ├── RText.kt                             ← Metin bileseni
│   │   └── RTextField.kt                        ← Metin alani bileseni
│   ├── data/repository/
│   │   ├── SkillRepository.kt                   ← Repository interface
│   │   └── SkillRepositoryImpl.kt               ← FileScanner delegasyonu, Result wrapping
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Skill.kt                         ← Skill veri modeli (name, commandName, description, filePath, isFavorite)
│   │   │   └── SkillFolder.kt                   ← Agac yapisinda klasör modeli (skills + subFolders)
│   │   └── usecase/
│   │       └── ScanSkillsUseCase.kt             ← Root path kontrolü → SkillRepository.scanSkills()
│   ├── service/
│   │   ├── FileScanner.kt                       ← Dosya sistemi tarayicisi (5dk in-memory cache)
│   │   ├── GitHubCacheService.kt                ← GitHub API veri cache servisi
│   │   └── JiraService.kt                       ← Jira entegrasyon servisi
│   ├── settings/
│   │   ├── PluginSettingsService.kt             ← Proje ayarlari (runeplugin.xml) — rootPath, agentsRootPath
│   │   └── PluginConfigurable.kt               ← IDE Settings > Tools > Rune sayfasi
│   ├── theme/
│   │   ├── RColor.kt                            ← Renk paleti tanimlari
│   │   └── RTheme.kt                            ← MaterialTheme wrapper (RTheme.colors.*)
│   └── toolwindow/
│       ├── ClaudeToolWindowFactory.kt           ← "Claude" tool window fabrikasi (sag panel)
│       ├── ClaudeSessionService.kt              ← Coklu terminal oturum yönetimi (StateFlow)
│       └── ClaudeTerminalContent.kt             ← Ana Claude UI: session tabs, terminal, input bar, dialogs
├── src/main/resources/
│   ├── META-INF/plugin.xml                      ← Plugin manifest
│   └── icons/pluginIcon.svg                     ← Plugin ikonu
├── .github/workflows/
│   ├── build.yml                                ← CI: build + test + Qodana + Plugin Verifier
│   ├── release.yml                              ← CD: JetBrains Marketplace yayinlama
│   └── run-ui-tests.yml                         ← Robot server UI testleri
├── build.gradle.kts                             ← Ana build yapilandirmasi
├── settings.gradle.kts                          ← Gradle ayarlari
├── gradle.properties                            ← Plugin/platform özellikleri
└── gradle/libs.versions.toml                    ← Versiyon katalogu
```

**Toplam**: 30 Kotlin kaynak dosyasi | ~4,700 LOC

---

## Entry Points

| Dosya | Amac |
|-------|------|
| `plugin.xml` | Plugin manifest: 1 tool window, 5 action, 1 configurable, terminal bagimliligi |
| `ClaudeToolWindowFactory.kt` | "Claude" tool window — gömülü Claude CLI terminali (sag panel) |

---

## Core Modules

### 1. Claude Terminal (`toolwindow/`)
- **ClaudeToolWindowFactory**: ComposePanel icinde tool window olusturur
- **ClaudeSessionService**: Coklu terminal oturumu yönetimi (StateFlow tabanli reaktif state)
- **ClaudeTerminalContent**: Ana UI — session tabs, SwingPanel terminal, action butonlari (Model, Skills, Agents, Commands), input bar
- **Özellikler**: Coklu oturum (sekme bazli), Skills/Agents/Commands picker, görsel ekleme, dosya enjeksiyonu, Claude CLI kurulum kontrolü

### 2. IDE Actions (`actions/`)
- **GenerateCommitMessageAction**: Staged diff → `claude -p` → commit mesaji (VCS menüsü, 30sn timeout)
- **CreateReviewPRAction**: Auto base branch algilama → review branch → `gh pr create`
- **FixPRCommentsAction**: PR review yorumlarini otomatik düzeltme
- **AskClaudeAction**: Editör sag-tik → secili kodu Claude terminal'e gönderir
- **SkillBestPracticesNotificationProvider**: SKILL.md dosyalari icin editor notification banner

### 3. Dialogs (`actions/dialog/`)
- **CreatePRDialog**: GitHub entegrasyonlu PR olusturma (487 satir)
- **CreateSkillDialog**: Skill dosyasi olusturma — state-based dogrulama (nameErrors/nameWarnings/nameHints)
- **FixPRCommentsDialog**: PR yorumlari düzeltme dialogu (690 satir)

### 4. Domain Layer (`domain/`)
- **Skill**: `name`, `commandName` (/dosya-adi), `description`, `filePath`, `isFavorite`
- **SkillFolder**: Agac yapisi — `skills + subFolders`, `getAllSkills()` recursive traversal
- **ScanSkillsUseCase**: Root path bos kontrolü → `SkillRepository.scanSkills()`

### 5. Data Layer (`data/repository/`)
- **SkillRepository**: Interface — `scanSkills(rootPath, strictFilter): Result<List<SkillFolder>>`
- **SkillRepositoryImpl**: FileScanner delegasyonu, Result wrapping

### 6. Services (`service/`)
- **FileScanner**: VirtualFile sistemi ile `.md` tarama, 5dk in-memory cache
  - `strictFilter=true` → sadece `SKILL.md` uzantili (Skills sekmesi)
  - `strictFilter=false` → tüm `.md` dosyalari (Agents sekmesi)
- **GitHubCacheService**: GitHub API verilerini cache'ler
- **JiraService**: Branch adindan `[A-Z]+-\d+` ticket ID cikarir

### 7. Settings (`settings/`)
- **PluginSettingsService** (PROJECT-scoped): `runeplugin.xml` → `rootPath`, `agentsRootPath`
- **PluginConfigurable**: IDE Settings > Tools > Rune ayar sayfasi

### 8. UI Components (`components/`)
- R prefix: `RActionCard`, `RCheckbox`, `RDialogWrapper`, `RText`, `RTextField`

### 9. Theme (`theme/`)
- **RTheme**: MaterialTheme wrapper — `RTheme.colors.*` erisimi
- **RColor**: black, gray, blue, purple, white, lightGray, hintGray, red, primaryContainer

---

## Configuration

| Dosya | Amac |
|-------|------|
| `gradle.properties` | `pluginGroup=com.github.cnrture.rune`, `platformType=AI`, `platformVersion=2025.2.2.3` |
| `build.gradle.kts` | Compose Desktop, IntelliJ Platform, Serialization, FreeMarker |
| `gradle/libs.versions.toml` | Kotlin 2.3.0, IntelliJ Platform 2.11.0, Compose 1.10.1 |
| `plugin.xml` | 1 toolWindow, 5 action, 1 configurable, terminal bagimliligi |

**Build araligi**: `241` – `253.*` | **JVM**: Java 21 | **Gradle**: 8.13.2

---

## Key Dependencies

| Bagimlilik | Versiyon | Amac |
|-----------|---------|------|
| IntelliJ Platform Gradle Plugin | 2.11.0 | Plugin build sistemi |
| Kotlin JVM | 2.3.0 | Dil destegi |
| Jetpack Compose Desktop | 1.10.1 | UI framework (Skiko SOFTWARE render) |
| FreeMarker | 2.3.34 | Sablon motoru |
| kotlinx-serialization-json | 1.9.0 | JSON serilestirme |
| Compose Material Icons Extended | 1.10.1 | Ikon kütüphanesi |
| JUnit | 4.13.2 | Unit test framework |

---

## Service Access Pattern

DI framework yok — companion `getInstance()` ile erisim:
```kotlin
PluginSettingsService.getInstance(project)  // Project-scoped
ClaudeSessionService.getInstance(project)   // Project-scoped
```

---

## CI/CD

| Workflow | Tetikleyici | Amac |
|----------|------------|------|
| `build.yml` | push to main, PR | Build + test + Qodana + Plugin Verifier |
| `release.yml` | GitHub release | JetBrains Marketplace yayinlama |
| `run-ui-tests.yml` | Manuel | Robot server UI testleri (port 8082) |

---

## Architecture Summary

```
plugin.xml
  └── ClaudeToolWindowFactory (right panel) ── Claude Terminal
        └── ClaudeSessionService (multi-session, StateFlow)
              └── JBTerminalWidget + ComposePanel

Actions (VCS menu / Editor popup):
  ├── GenerateCommitMessageAction ── claude -p "diff" → commit msg
  ├── CreateReviewPRAction ── git + gh CLI → review PR
  ├── FixPRCommentsAction ── PR review comment fix
  └── AskClaudeAction ── selected code → Claude terminal

Data Flow:
  UI (Compose) → UseCase → Repository → FileScanner → State → UI
```

**UI**: Jetpack Compose Desktop (ComposePanel → JPanel)
**Terminal**: JBTerminalWidget (LocalTerminalDirectRunner) ile `claude` CLI
**Render**: `skiko.renderApi=SOFTWARE`

---

## Quick Start

```bash
./gradlew runIde              # Sandbox IDE'de calistir
./gradlew check               # Testleri calistir
./gradlew buildPlugin         # Dagitim ZIP olustur
./gradlew publishPlugin       # Marketplace'e yayinla
./gradlew runPluginVerifier   # Plugin uyumluluk dogrulamasi
```