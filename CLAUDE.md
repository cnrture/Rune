# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Run plugin in a sandboxed IDE instance
./gradlew runIde

# Run tests
./gradlew check

# Run a single test class
./gradlew test --tests "com.github.cnrture.rune.SomeTest"

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

**Note:** No tests exist yet (`src/test/` is empty). Test dependencies (JUnit, opentest4j, IntelliJ TestFramework) are configured in `build.gradle.kts`.

## Architecture

IntelliJ Platform plugin for Android Studio, built with Kotlin and Jetpack Compose Desktop for UI. No DI framework — all dependencies are wired manually.

**One tool window** registered in `plugin.xml`:
- **`ClaudeToolWindowFactory`** → "Claude" panel (right sidebar): Claude CLI terminal integration with multi-session support, skill/agent discovery, and command palette

### Layer structure

```
toolwindow/ (Compose UI in ComposePanel)
    ↓
domain/usecase (business logic)
    ↓
data/repository (SkillRepository)
    ↓
service/ (FileScanner, Settings, CliDiscovery, GitHub services)
```

### Key packages

| Package | Responsibility |
|---|---|
| `toolwindow/` | `ClaudeToolWindowFactory`, `ClaudeSessionService`, `ClaudeTerminalContent` — terminal sessions, full Claude panel UI |
| `domain/usecase/` | `ScanSkillsUseCase` |
| `data/repository/` | `SkillRepositoryImpl` – scans markdown files, 5-minute cache |
| `service/` | `FileScanner`, `PluginSettingsService`, `CliDiscoveryService`, `GitHubCacheService` |
| `components/` | Reusable Compose components (all prefixed `R`: `RActionCard`, `RCheckbox`, `RText`, `RTextField`, `RDialogWrapper`, `RErrorBanner`) |
| `theme/` | `RTheme` / `RColor` – always use `RTheme.colors.*` for colors |
| `actions/` | VCS actions, editor notifications |
| `actions/dialog/` | `CreateSkillDialog`, `CreatePRDialog`, `FixPRCommentsDialog` |
| `settings/` | `PluginSettingsService`, `PluginConfigurable` (IDE Settings > Tools > Rune Settings) |
| `common/` | `Constants`, `NoRippleTheme`, `ProcessRunner` |

### Service access pattern

Services use `@Service` annotations (auto-discovered by IntelliJ Platform Gradle Plugin, **not** declared in `plugin.xml`) with companion `getInstance()`:
```kotlin
PluginSettingsService.getInstance(project)  // @Service(Service.Level.PROJECT)
ClaudeSessionService.getInstance(project)   // @Service(Service.Level.PROJECT)
GitHubCacheService.getInstance()            // @Service(Service.Level.APP)
```

### Settings persistence

**`PluginSettingsService`** (project-scoped, `runeplugin.xml`) – skills root path, agents root path, and commit message prompt. Configured via IDE Settings > Tools > Rune Settings (`PluginConfigurable`).

### Claude terminal integration

`ClaudeSessionService` manages terminal sessions via `MutableStateFlow<ClaudeSessionState>`. State tracks: active sessions, Claude CLI installation status, and SuperClaude availability (`~/.claude/commands/` directory).

`ClaudeTerminalContent.kt` (~900 lines) renders the full Claude panel UI:
- **Session tabs** with add/close/switch
- **Terminal view** (`JBTerminalWidget` in `SwingPanel` via `SessionManager` with `CardLayout`)
- **UnifiedCommandPalette** — searchable overlay with skills, agents, 27 built-in Claude `/commands`, and SuperClaude `/sc:*` commands
- **TerminalInputBar** — multi-line input with `@` file injection, image picker, slash command trigger, Enter to send

The `SessionManager` inner class manages a `CardLayout` + `JPanel` containing multiple `JBTerminalWidget` panels. New sessions automatically send `"claude\n"` to terminal after a 1.5s delay.

CLI discovery is handled by `CliDiscoveryService` — uses `bash -l -c "which claude"` (login shell for full PATH) with fallback to common install locations. `GenerateCommitMessageAction` runs `claude -p <prompt>` with a 30-second timeout. The prompt is configurable via `PluginSettingsService` using `{diff}` as a placeholder.

### Skill file format

Skills are discovered by `FileScanner` scanning a configurable root directory for `.md` files:
- **Skills tab** uses strict filtering: only files named exactly `SKILL.md`
- **Agents tab** accepts any `.md` file

Description parsing priority: `description:` frontmatter → `#` heading → first non-blank paragraph line (max 100 chars).

### Plugin actions (registered in plugin.xml)

| Action | Trigger | Description |
|---|---|---|
| `GenerateCommitMessageAction` | VCS commit dialog | Generates commit message from staged+unstaged diff via Claude CLI with configurable prompt |
| `CreateReviewPRAction` | VCS commit dialog | Detects base branch, pushes current branch, creates PR via `gh` CLI with selectable base branch, reviewers, and labels |
| `FixPRCommentsAction` | VCS commit dialog | Fetches unresolved PR review threads via GitHub GraphQL API, sends fix prompt to Claude terminal |
| `AskClaudeAction` | Editor right-click menu | Sends selected code with file context to Claude terminal via `pendingInput` flow |
| `SkillBestPracticesNotificationProvider` | Opens SKILL.md files | Editor notification banner: "Open best practices" link + "Check with Claude" validation |

### Compose Desktop notes

- UI uses Jetpack Compose Desktop via `org.jetbrains.compose` plugin, rendered inside `ComposePanel` (Swing interop)
- Skiko render API is set to SOFTWARE for compatibility: `System.setProperty("skiko.renderApi", "SOFTWARE")`
- Always wrap composables in `RTheme { }` and use `RTheme.colors.*` for theming
- Only light colors are currently defined (no dark theme variant)
- Dialogs extend `RDialogWrapper` which uses `ComposePanel` with custom dark background (`0xFF18181B`)

## Conventions

### Error handling

Repositories return `Result<T>` (success/failure) instead of throwing exceptions. External process calls (Claude CLI, git, gh) use `ProcessRunner` utility: `ProcessRunner.run()` returns empty string on failure (silent mode), `ProcessRunner.runOrThrow()` throws on failure (used in dialogs). CLI discovery is centralized in `CliDiscoveryService`.

### Dialog validation pattern

Dialogs like `CreateSkillDialog` use a state data class with computed validation functions (`nameErrors()`, `nameWarnings()`, `nameHints()`) that return nullable strings. The "Create" button is disabled until all error functions return null. Warnings and hints are displayed but don't block submission.

### Editor notification providers

Implement `EditorNotificationProvider` + `DumbAware` for file-specific banners. See `SkillBestPracticesNotificationProvider` for the pattern: check file name in `collectNotificationData()`, return `EditorNotificationPanel` with action links.

### Caching

- `FileScanner` uses a 5-minute in-memory cache for directory scans. Call `invalidateCache()` explicitly when settings change (e.g., root path updates).
- `GitHubCacheService` persists collaborators and labels per `owner/repo` key in `githubCache.xml`.

### External dependencies

- **Gson** (`com.google.gson.JsonParser`) is used in `FixPRCommentsDialog` — bundled with IntelliJ Platform, not declared in `build.gradle.kts`
- `compose.desktop.currentOs` excludes `kotlinx-coroutines-core/jvm/swing` to avoid conflict with IntelliJ's bundled coroutines

## CI/CD

GitHub Actions workflows in `.github/workflows/`:
- **`build.yml`** — triggered on push to master and PRs: build, test, Qodana inspection, Plugin Verifier, draft release
- **`release.yml`** — triggered on GitHub release: publish to JetBrains Marketplace (requires `PUBLISH_TOKEN`, `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` secrets)
- **`run-ui-tests.yml`** — manual trigger: robot server UI tests on ubuntu/windows/macOS matrix
