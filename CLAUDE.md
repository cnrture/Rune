# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Run plugin in a sandboxed IDE instance
./gradlew runIde

# Run tests
./gradlew check

# Build distribution ZIP
./gradlew buildPlugin

# Publish to JetBrains Marketplace
./gradlew publishPlugin

# Run UI tests (starts a robot server on port 8082)
./gradlew runIdeForUiTests

# Verify plugin compatibility
./gradlew runPluginVerifier
```

**Target platform:** Android Studio (AI type), version `2025.2.2.3`. JVM toolchain: Java 21. Kotlin 2.3.0, Compose Desktop 1.10.1.

## Architecture

IntelliJ Platform plugin for Android Studio, built with Kotlin and Jetpack Compose Desktop for UI. No DI framework — all dependencies are wired manually in factory classes.

**Two independent tool windows** registered in `plugin.xml`:
- **`TPToolWindowFactory`** → "Teknasyon Plugin" panel (right sidebar): Jungle, Module Generator, Settings
- **`ClaudeToolWindowFactory`** → "Claude" panel (right sidebar): Claude CLI terminal integration

### Layer structure

```
toolwindow/ (Compose UI in ComposePanel)
    ↓
domain/usecase (business logic)
    ↓
data/repository (SkillRepository)
    ↓
service/ (FileScanner, Settings, Jira, GitHub services)
```

### Key packages

| Package | Responsibility |
|---|---|
| `toolwindow/claude/` | Claude terminal: session management, CLI commands, skill/agent pickers |
| `toolwindow/manager/` | Module generator, Feature generator, Settings, Jungle |
| `toolwindow/template/` | FreeMarker code generation (Gradle, Manifest, .gitignore, README) |
| `domain/usecase/` | `ScanSkillsUseCase` |
| `data/repository/` | `SkillRepositoryImpl` – scans markdown files, 5-minute cache |
| `service/` | `FileScanner`, `SettingsService`, `PluginSettingsService`, `JiraService`, `GitHubCacheService` |
| `components/` | Reusable Compose components (all prefixed `TP`: `TPTabRow`, `TPActionCard`, `TPText`, etc.) |
| `theme/` | `TPTheme` / `TPColor` – always use `TPTheme.colors.*` for colors |
| `actions/` | VCS actions: commit message generation with Claude, PR creation, Ask Claude from editor |

### Settings persistence

Two independent settings services:
- **`PluginSettingsService`** (project-scoped, `teknasyonandroidstudioplugin.xml`) – skills/agents root paths. Configured via IDE Settings > Tools > Teknasyon Plugin Settings (`PluginConfigurable`)
- **`SettingsService`** (app-scoped, `gtcDevToolsSettings.xml`) – module/feature templates, UI state; auto-backs up to `~/.gtcdevtools/settings.json`

### Claude terminal integration

`ClaudeSessionService` manages terminal sessions (create, switch, close). `ClaudeTerminalContent.kt` renders the full Claude panel UI: session tabs, terminal view (SwingPanel), action buttons (Model, Skills, Agents, Commands), and input bar.

The terminal finds `claude` CLI via PATH/shell with fallback to common install locations. `GenerateCommitMessageAction` runs `claude -p <prompt>` with a 30-second timeout to generate commit messages.

### Skill file format

Skills are discovered by scanning a configurable root directory for `.md` files. Skills tab uses strict filtering (`SKILL.md` suffix); Agents tab accepts any `.md` file. The first non-empty line of the file body becomes `description`.

### Template system

Module and Feature generators use FreeMarker (2.3.34) templates. Templates are serialized as `ModuleTemplate` / `FeatureTemplate` data classes (Kotlinx Serialization) and stored in `SettingsService`. Default templates are defined inline in `SettingsState.kt`. Variable substitution: `{NAME}` for module/feature name, `{FILE_PACKAGE}` for package.

### Plugin actions (registered in plugin.xml)

| Action | Trigger | Description |
|---|---|---|
| `GenerateCommitMessageAction` | VCS commit dialog | Generates commit message from staged diff via Claude CLI |
| `CreateReviewPRAction` | VCS commit dialog | Creates review branch and PR |
| `AskClaudeAction` | Editor right-click menu | Sends selected code to Claude terminal |
| `FeatureGeneratorAction` | File > New menu | Creates a new feature from template |
| `ModuleGeneratorAction` | File > New menu | Creates a new module with selected files |

### Compose Desktop notes

- UI uses Jetpack Compose Desktop via `org.jetbrains.compose` plugin, rendered inside `ComposePanel` (Swing interop)
- Skiko render API is set to SOFTWARE for compatibility
- Always wrap composables in `TPTheme { }` and use `TPTheme.colors.*` for theming
