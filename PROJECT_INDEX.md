# Project Index: Rune

Generated: 2026-03-01

---

## Project Structure

```
Rune/
├── src/main/kotlin/com/github/cnrture/rune/
│   ├── actions/
│   │   ├── AskClaudeAction.kt                  ← Editor right-click → sends selected code to Claude
│   │   ├── GenerateCommitMessageAction.kt       ← Generates commit message via Claude CLI
│   │   ├── CreateReviewPRAction.kt              ← Detects base branch + creates PR via gh CLI
│   │   ├── FixPRCommentsAction.kt               ← Auto-fixes PR review comments
│   │   ├── SkillBestPracticesNotificationProvider.kt ← Editor banner for SKILL.md files
│   │   └── dialog/
│   │       ├── CreatePRDialog.kt                ← PR creation dialog (GitHub integration)
│   │       ├── CreateSkillDialog.kt             ← Skill file creation dialog
│   │       └── FixPRCommentsDialog.kt           ← PR comment fix dialog
│   ├── common/
│   │   ├── Constants.kt                         ← Global constants
│   │   ├── NoRippleTheme.kt                     ← Compose ripple theme
│   │   └── ProcessRunner.kt                     ← Unified process execution utility
│   ├── components/                              ← Shared Compose components (R prefix)
│   │   ├── RActionCard.kt                       ← Action card component
│   │   ├── RCheckbox.kt                         ← Checkbox component
│   │   ├── RDialogWrapper.kt                    ← Dialog wrapper component
│   │   ├── RErrorBanner.kt                      ← Error banner with retry button
│   │   ├── RText.kt                             ← Text component
│   │   └── RTextField.kt                        ← Text field component
│   ├── data/repository/
│   │   ├── SkillRepository.kt                   ← Repository interface
│   │   └── SkillRepositoryImpl.kt               ← FileScanner delegation, Result wrapping
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Skill.kt                         ← Skill data model (name, commandName, description, filePath, isFavorite)
│   │   │   └── SkillFolder.kt                   ← Tree structure folder model (skills + subFolders)
│   │   └── usecase/
│   │       └── ScanSkillsUseCase.kt             ← Root path validation → SkillRepository.scanSkills()
│   ├── service/
│   │   ├── CliDiscoveryService.kt               ← CLI path discovery (claude, gh) with fallback locations
│   │   ├── FileScanner.kt                       ← File system scanner (5min in-memory cache)
│   │   └── GitHubCacheService.kt                ← GitHub API data cache service
│   ├── settings/
│   │   ├── PluginSettingsService.kt             ← Project settings (runeplugin.xml) — rootPath, agentsRootPath, commitMessagePrompt
│   │   └── PluginConfigurable.kt                ← IDE Settings > Tools > Rune page
│   ├── theme/
│   │   ├── RColor.kt                            ← Color palette definitions
│   │   └── RTheme.kt                            ← MaterialTheme wrapper (RTheme.colors.*)
│   └── toolwindow/
│       ├── ClaudeToolWindowFactory.kt           ← "Claude" tool window factory (right panel)
│       ├── ClaudeSessionService.kt              ← Multi-session terminal management (StateFlow)
│       └── ClaudeTerminalContent.kt             ← Main Claude UI: session tabs, terminal, input bar, command palette
├── src/main/resources/
│   ├── META-INF/plugin.xml                      ← Plugin manifest
│   └── icons/pluginIcon.svg                     ← Plugin icon
├── .github/workflows/
│   ├── build.yml                                ← CI: build + test + Qodana + Plugin Verifier
│   ├── release.yml                              ← CD: JetBrains Marketplace publishing
│   └── run-ui-tests.yml                         ← Robot server UI tests
├── build.gradle.kts                             ← Main build configuration
├── settings.gradle.kts                          ← Gradle settings
├── gradle.properties                            ← Plugin/platform properties
└── gradle/libs.versions.toml                    ← Version catalog
```

**Total**: 31 Kotlin source files

---

## Entry Points

| File | Purpose |
|------|---------|
| `plugin.xml` | Plugin manifest: 1 tool window, 5 actions, 1 configurable, terminal dependency |
| `ClaudeToolWindowFactory.kt` | "Claude" tool window — embedded Claude CLI terminal (right panel) |

---

## Core Modules

### 1. Claude Terminal (`toolwindow/`)
- **ClaudeToolWindowFactory**: Creates tool window inside ComposePanel
- **ClaudeSessionService**: Multi-session terminal management (StateFlow-based reactive state)
- **ClaudeTerminalContent**: Main UI — session tabs, SwingPanel terminal, action buttons (Model, Skills, Agents, Commands), input bar
- **Features**: Multi-session (tab-based), Skills/Agents/Commands picker, image attachment, file injection, Claude CLI install check

### 2. IDE Actions (`actions/`)
- **GenerateCommitMessageAction**: Staged diff → `claude -p` → commit message (VCS menu, 30s timeout, configurable prompt)
- **CreateReviewPRAction**: Auto base branch detection → push → `gh pr create` with selectable base branch
- **FixPRCommentsAction**: Auto-fix unresolved PR review comments
- **AskClaudeAction**: Editor right-click → sends selected code to Claude terminal
- **SkillBestPracticesNotificationProvider**: Editor notification banner for SKILL.md files

### 3. Dialogs (`actions/dialog/`)
- **CreatePRDialog**: PR creation with GitHub integration — base branch selection, reviewer/label picker
- **CreateSkillDialog**: Skill file creation — state-based validation (nameErrors/nameWarnings/nameHints)
- **FixPRCommentsDialog**: PR comment fix dialog with thread selection

### 4. Domain Layer (`domain/`)
- **Skill**: `name`, `commandName` (/file-name), `description`, `filePath`, `isFavorite`
- **SkillFolder**: Tree structure — `skills + subFolders`, `getAllSkills()` recursive traversal
- **ScanSkillsUseCase**: Root path validation → `SkillRepository.scanSkills()`

### 5. Data Layer (`data/repository/`)
- **SkillRepository**: Interface — `scanSkills(rootPath, strictFilter): Result<List<SkillFolder>>`
- **SkillRepositoryImpl**: FileScanner delegation, Result wrapping

### 6. Services (`service/`)
- **CliDiscoveryService**: Centralized CLI path discovery for `claude` and `gh` with fallback locations
- **FileScanner**: VirtualFile system `.md` scanning, 5min in-memory cache
  - `strictFilter=true` → only `SKILL.md` files (Skills tab)
  - `strictFilter=false` → all `.md` files (Agents tab)
- **GitHubCacheService**: Caches GitHub API data (collaborators, labels) per owner/repo

### 7. Common Utilities (`common/`)
- **ProcessRunner**: Unified process execution — `run()` (silent), `runOrThrow()` (throwing), `git()` (shortcut)
- **Constants**: Global constants
- **NoRippleTheme**: Compose interaction source that disables ripple

### 8. Settings (`settings/`)
- **PluginSettingsService** (PROJECT-scoped): `runeplugin.xml` → `rootPath`, `agentsRootPath`, `commitMessagePrompt`
- **PluginConfigurable**: IDE Settings > Tools > Rune settings page

### 9. UI Components (`components/`)
- R prefix: `RActionCard`, `RCheckbox`, `RDialogWrapper`, `RErrorBanner`, `RText`, `RTextField`

### 10. Theme (`theme/`)
- **RTheme**: MaterialTheme wrapper — `RTheme.colors.*` access
- **RColor**: black, gray, blue, purple, white, lightGray, hintGray, red, primaryContainer

---

## Configuration

| File | Purpose |
|------|---------|
| `gradle.properties` | `pluginGroup=com.github.cnrture.rune`, `platformType=AI`, `platformVersion=2025.2.2.3` |
| `build.gradle.kts` | Compose Desktop, IntelliJ Platform |
| `gradle/libs.versions.toml` | Kotlin 2.3.0, IntelliJ Platform 2.11.0, Compose 1.10.1 |
| `plugin.xml` | 1 toolWindow, 5 actions, 1 configurable, terminal dependency |

**Build range**: `241` – `253.*` | **JVM**: Java 21 | **Gradle**: 8.13.2

---

## Service Access Pattern

No DI framework — companion `getInstance()` access:
```kotlin
PluginSettingsService.getInstance(project)  // Project-scoped
ClaudeSessionService.getInstance(project)   // Project-scoped
GitHubCacheService.getInstance()            // App-scoped
```

---

## CI/CD

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `build.yml` | push to master, PR | Build + test + Qodana + Plugin Verifier |
| `release.yml` | GitHub release | JetBrains Marketplace publishing |
| `run-ui-tests.yml` | Manual | Robot server UI tests (port 8082) |
