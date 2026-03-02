# Project Index: TPDevTools (Teknasyon Android Studio Plugin)

Generated: 2026-02-27

---

## Project Structure

```
Teknasyon-AndroidStudio-Plugin/
├── src/main/kotlin/com/github/teknasyon/plugin/
│   ├── TPToolWindowFactory.kt             ← Ana tool window (Jungle/Module/Settings)
│   ├── actions/
│   │   ├── AskClaudeAction.kt             ← Editörden seçili kodu Claude'a gönderir
│   │   ├── GenerateCommitMessageAction.kt ← Claude ile commit mesajı oluşturur
│   │   ├── CreateReviewPRAction.kt        ← Review branch + PR oluşturur
│   │   └── dialog/
│   │       └── CreatePRDialog.kt          ← PR oluşturma dialogu
│   ├── common/
│   │   ├── file/
│   │   │   ├── File.kt, FileTree.kt, FileWriter.kt
│   │   │   ├── ImportAnalyzer.kt
│   │   │   └── LibraryDependencyFinder.kt
│   │   ├── Constants.kt
│   │   ├── Extensions.kt
│   │   ├── NoRippleTheme.kt
│   │   └── Utils.kt
│   ├── components/                         ← Ortak Compose bileşenleri (TP prefix)
│   │   ├── TPActionCard.kt, TPButton.kt, TPCheckbox.kt
│   │   ├── TPDialogWrapper.kt, TPDropdownItem.kt
│   │   ├── TPFileTree.kt, TPRadioButton.kt
│   │   ├── TPTabRow.kt, TPText.kt, TPTextField.kt
│   ├── data/
│   │   ├── FileTemplate.kt, LibraryInfo.kt
│   │   ├── ModuleTemplate.kt, PluginInfo.kt, PluginListItem.kt
│   │   ├── SettingsState.kt
│   │   └── repository/
│   │       ├── SkillRepository.kt          ← Interface
│   │       └── SkillRepositoryImpl.kt      ← Skill tarama implementasyonu
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Skill.kt                    ← Markdown dosyası → komut modeli
│   │   │   ├── SkillFolder.kt              ← Ağaç yapısında klasör modeli
│   │   │   ├── ReviewTask.kt, ReviewChange.kt
│   │   └── usecase/
│   │       └── ScanSkillsUseCase.kt        ← Root path'te .md dosyalarını tarar
│   ├── service/
│   │   ├── FileScanner.kt                  ← Dosya sistemi tarayıcı (5dk cache)
│   │   ├── SettingsService.kt              ← Global ayarlar (gtcDevToolsSettings.xml)
│   │   ├── PluginSettingsService.kt        ← Proje ayarları (rootPath, agentsRootPath)
│   │   ├── PluginConfigurable.kt           ← IDE Settings > Tools sayfası
│   │   ├── GitHubCacheService.kt           ← GitHub veri cache'i
│   │   └── JiraService.kt                  ← Jira entegrasyonu
│   ├── theme/
│   │   ├── TPTheme.kt                      ← MaterialTheme wrapper
│   │   └── TPColor.kt                      ← Renk paleti
│   └── toolwindow/
│       ├── claude/                          ← Claude Terminal entegrasyonu
│       │   ├── ClaudeToolWindowFactory.kt   ← "Claude" tool window fabrikası
│       │   ├── ClaudeSessionService.kt      ← Çoklu terminal oturum yönetimi
│       │   └── ClaudeTerminalContent.kt     ← Ana Claude UI (terminal + input + dialogs)
│       ├── manager/
│       │   ├── jungle/
│       │   │   └── JungleContent.kt
│       │   ├── modulegenerator/             ← Modül oluşturucu
│       │   │   ├── ModuleGeneratorContent.kt
│       │   │   ├── MoveExistingFilesToModuleContent.kt
│       │   │   ├── CreateNewModuleConfigurationPanel.kt
│       │   │   ├── action/ModuleGeneratorAction.kt
│       │   │   ├── dialog/ModuleGeneratorDialog.kt
│       │   │   └── components/ (6 dosya)
│       │   └── settings/
│       │       ├── SettingsContent.kt
│       │       ├── component/FileTemplateEditor.kt
│       │       └── dialog/ (3 dialog)
│       └── template/                        ← FreeMarker şablonları
│           ├── GradleTemplate.kt, ManifestTemplate.kt
│           ├── GitIgnoreTemplate.kt, ModuleReadMeTemplate.kt
│           └── TemplateWriter.kt
├── src/main/resources/
│   ├── META-INF/plugin.xml
│   └── icons/ (pluginIcon.svg, claudeIcon.svg)
├── .github/workflows/ (build.yml, release.yml, run-ui-tests.yml)
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
└── CHANGELOG.md
```

**Toplam**: 71 Kotlin dosyası

---

## Entry Points

| Dosya | Amaç |
|-------|-------|
| `TPToolWindowFactory.kt` | "Teknasyon Plugin" tool window — Jungle, Module, Settings bölümleri |
| `ClaudeToolWindowFactory.kt` | "Claude" tool window — Gömülü Claude CLI terminali |
| `plugin.xml` | Plugin manifest: 2 tool window, 4 action, 1 configurable |

---

## Core Modules

### 1. Claude Terminal (`toolwindow/claude/`)
- **Amaç**: Claude CLI'ı IDE içinde gömülü terminal olarak sunar
- **ClaudeSessionService**: Çoklu terminal oturumu yönetimi (SessionManager + CardLayout)
- **ClaudeTerminalContent**: Ana UI — SessionTabBar, SwingPanel terminal, ActionButtons, TerminalInputBar
- **Özellikler**:
  - Çoklu oturum (sekme bazlı)
  - Skills/Agents/Commands picker dialog'ları
  - Görsel ekleme, dosya enjeksiyonu (@), Enter ile gönderme
  - Claude CLI kurulum kontrolü + rehber ekranı
  - 27 adet Claude komutu hızlı erişim grid'i

### 2. IDE Actions (`actions/`)
- **GenerateCommitMessageAction**: Staged diff → Claude `-p` → commit mesajı (VCS menüsü)
- **CreateReviewPRAction**: Auto base branch algılama → review branch → `gh pr create`
- **AskClaudeAction**: Editör sağ-tık → seçili kodu Claude tool window'a gönderir
- Jira entegrasyonu: Branch adından `[A-Z]+-\d+` ticket ID çıkarır

### 3. Domain Layer (`domain/`)
- **Skill**: `name`, `commandName` (/dosya-adı), `description`, `filePath`, `isFavorite`
- **SkillFolder**: Ağaç yapısı — `skills + subFolders`, `getAllSkills()` recursive traversal
- **ScanSkillsUseCase**: Root path boş kontrolü → `SkillRepository.scanSkills()`

### 4. Data Layer (`data/`)
- **SkillRepositoryImpl**: `FileScanner` delegasyonu, Result wrapping
- **SettingsState**: `moduleTemplates`, `defaultModuleTemplateId`, `isActionsExpanded`
- **ModuleTemplate / FileTemplate**: Kotlinx Serialization ile persist edilen şablonlar

### 5. Services (`service/`)
- **FileScanner**: VirtualFile sistemi ile `.md` tarama, 5dk in-memory cache
  - `strictFilter=true` → sadece `SKILL.md` (Skills sekmesi)
  - `strictFilter=false` → tüm `.md` dosyaları (Agents sekmesi)
- **SettingsService** (APP-scoped): `gtcDevToolsSettings.xml` + `~/.gtcdevtools/settings.json` auto-backup
- **PluginSettingsService** (PROJECT-scoped): `teknasyonandroidstudioplugin.xml` → `rootPath`, `agentsRootPath`

### 6. Module Generator (`toolwindow/manager/modulegenerator/`)
- Wizard akışı: Root → Tip/İsim → Template → Plugin → Kütüphane → Dosya ağacı
- FreeMarker template'leri: Gradle, Manifest, GitIgnore, ReadMe
- Varsayılan şablon: "Candroid's Module" (Screen + ViewModel + Contract + PreviewProvider)

### 7. UI Component Library (`components/`)
- TP prefix: `TPButton`, `TPCheckbox`, `TPTextField`, `TPText`, `TPRadioButton`
- `TPTabRow`, `TPActionCard` (EXTRA_SMALL/SMALL/MEDIUM/LARGE), `TPFileTree`
- `TPDialogWrapper`, `TPDropdownItem`

### 8. Theme (`theme/`)
- `TPTheme`: MaterialTheme wrapper, `TPTheme.colors.*` erişimi
- `TPColor`: black, gray, blue, purple, white, lightGray, hintGray, red, primaryContainer

---

## Configuration

| Dosya | Amaç |
|-------|-------|
| `gradle.properties` | `pluginGroup=com.github.teknasyon.plugin`, `platformType=AI`, `platformVersion=2025.2.2.3` |
| `build.gradle.kts` | Compose Desktop, IntelliJ Platform, Serialization, FreeMarker |
| `gradle/libs.versions.toml` | Kotlin 2.3.0, IntelliJ Platform 2.11.0, Compose 1.10.1 |
| `plugin.xml` | 2 toolWindow, 4 action, 1 configurable, terminal bağımlılığı |

**Build aralığı**: `241` – `253.*` | **JVM**: Java 21

---

## Key Dependencies

| Bağımlılık | Versiyon | Amaç |
|-----------|---------|-------|
| IntelliJ Platform Gradle Plugin | 2.11.0 | Plugin build sistemi |
| Kotlin JVM | 2.3.0 | Dil desteği |
| Jetpack Compose Desktop | 1.10.1 | UI framework (Skiko SOFTWARE render) |
| FreeMarker | 2.3.34 | Modül şablon motoru |
| kotlinx-serialization-json | 1.9.0 | Ayarlar JSON serileştirme |
| Compose Material Icons Extended | — | İkon kütüphanesi |

---

## CI/CD

| Workflow | Amaç |
|----------|-------|
| `build.yml` | Build + test |
| `release.yml` | JetBrains Marketplace yayınlama |
| `run-ui-tests.yml` | Robot server ile UI testleri (port 8082) |

---

## Architecture Summary

```
plugin.xml
  ├── TPToolWindowFactory (right panel) ── Jungle / Module / Settings
  └── ClaudeToolWindowFactory (right panel) ── Claude Terminal
        └── ClaudeSessionService (multi-session)
              └── JBTerminalWidget + CardLayout

Actions (VCS menu / Editor popup):
  ├── GenerateCommitMessageAction ── claude -p "diff" → commit msg
  ├── CreateReviewPRAction ── git + gh CLI → review PR
  └── AskClaudeAction ── selected code → Claude terminal

Data Flow:
  UI → Event → ViewModel → UseCase → Repository → FileScanner/Settings → State → UI
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