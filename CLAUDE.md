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

**Target platform:** Android Studio (AI type), version `2025.2.2.3`. JVM toolchain: Java 21.

## Architecture

This is an IntelliJ Platform plugin for Android Studio, built with Kotlin and Jetpack Compose Desktop for UI. The composition root is `TPToolWindowFactory`, which wires all dependencies manually (no DI framework) and registers the tool window.

### Layer structure

```
toolwindow/ (Compose UI)
    ↓
viewmodel (MutableStateFlow + sealed event classes)
    ↓
domain/usecase (business logic)
    ↓
data/repository (SkillRepository)
    ↓
service/ (FileScanner, TerminalExecutor, Settings services)
```

State management follows strict unidirectional data flow: UI fires `SkillDockEvent` → `SkillDockViewModel.onEvent()` → updates `MutableStateFlow<SkillDockState>` → UI recomposes.

### Key packages

| Package | Responsibility |
|---|---|
| `toolwindow/ai/` | SkillDock tab system – Skills, Agents, Commands tabs |
| `toolwindow/manager/` | AI tools, Module generator, Feature generator, Settings, Jungle |
| `domain/usecase/` | `ScanSkillsUseCase`, `ExecuteSkillUseCase`, `ToggleFavoriteUseCase`, `ProcessReviewCommentsUseCase` |
| `data/repository/` | `SkillRepositoryImpl` – scans markdown files, 5-minute cache |
| `service/` | `FileScanner`, `TerminalExecutor`, `SkillDockSettingsService`, `SettingsService` |
| `components/` | Reusable Compose components (`TPTabRow`, `TPActionCard`, `TPText`, etc.) |
| `theme/` | `TPTheme` / `TPColor` – always use `TPTheme.colors.*` for colors |
| `actions/` | IDE action contributions (commit message, create PR) |

### Domain models

- **`Skill`** – represents a markdown file; `commandName` is derived as `/filename-without-extension`
- **`SkillFolder`** – tree node with `skills: List<Skill>` and `subFolders: List<SkillFolder>`; `getAllSkills()` traverses recursively
- **`ReviewTask`** / **`ReviewChange`** – PR review pipeline data

### Settings persistence

Two independent settings services:
- **`SkillDockSettingsService`** (project-scoped, `skilldock.xml`) – skills/agents root paths, favorites set
- **`SettingsService`** (app-scoped, `gtcDevToolsSettings.xml`) – module/feature templates, API configs; auto-backs up to `~/.gtcdevtools/settings.json`

### Adding a new SkillDock tab

1. Add entry to `SkillDockTab` enum in `SkillDockState.kt`
2. Handle the new value in `currentTab` getter and `updateCurrentTab()`
3. Add `loadTab()` early-return guard if the tab has no file-system backing
4. Add a new event in `SkillDockEvent.kt` if needed
5. Handle the event in `SkillDockViewModel.onEvent()`
6. Add the `Tab` composable in `SkillDockPanel.kt` TabRow
7. Render the tab content in the `Box(modifier = Modifier.weight(1f))` block

### Terminal execution

`TerminalExecutor.executeCommand(project, command)` opens a new terminal widget and runs `command`. Falls back to clipboard copy if the terminal plugin is unavailable. Skills run their `commandName` directly; Claude CLI commands are prefixed with `claude `.

### Skill file format

Skills are discovered by scanning a configurable root directory for `.md` files. SKILLS tab uses strict filtering (`SKILL.md` suffix); AGENTS tab accepts any `.md` file. The first non-empty line of the file body becomes `description`.

### Template system

Module and Feature generators use FreeMarker templates. Templates are serialized as `ModuleTemplate` / `FeatureTemplate` data classes (Kotlinx Serialization) and stored in `SettingsService`. Default templates are defined inline in `SettingsState.kt`.
