<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# IntelliJ-AI-Plugin Changelog

## [Unreleased]

## [0.0.7] - 2026-03-18

### ✨ New Features
- **Model Selection and Caching** — Added the ability to select and cache the model used in Claude terminal sessions for a more personalized experience.
- **Slash Command Support** — Terminal input bar now supports slash commands for quick access to plugin features.
- **URL Highlighting** — URLs in the input bar and command palette are now automatically highlighted for better visibility.
- **Character Counter** — Added a character counter to the input bar when the input is nearing its limit to help users manage their input length.
- **Draft Input Management** — Improved handling of draft inputs in the terminal input bar for a smoother user experience.
- **Skills/Commands Panel Auto-Close** — The skills/commands panel now automatically closes when clicking outside of it, providing a more intuitive interaction.
- **Base Branch Selection** — Added the ability to select the target branch when creating a PR, giving users more control over their PR creation process.

### 🔧 Improvements
- **Enhanced Terminal Panel** — Improved the terminal panel with animated visibility and better layout behavior for a more polished user experience.
- **Hover Effects** — Added improved hover effects and tooltips to icons and UI components for better interactivity.
- **Input Focus Management** — Improved focus behavior of the input bar and terminal interactions for a more seamless workflow.
- **Settings Layout** — Refactored the settings layout for skills and agents directories into a two-column format for better organization.
- **CLI Resolution** — Improved CLI tool resolution with path resolution through the login shell, making it more reliable across different environments.
- **UI Consistency** — Made layout and spacing improvements across SessionTabBar, TerminalInputBar, and TPSwitch components for a more consistent UI.
- **Borderless Action Cards** — Updated action cards to be borderless for a cleaner and more modern look.
- **SVG Icons** — Updated icon usage in dialogs to use SVGs for better scalability and visual quality.

### 🐛 Bug Fixes
- Improved error handling in CLI helper functions and PR comment dialogs to prevent crashes and provide better feedback to users.
- Cleaned up unused color definitions from TPDialogWrapper for a more maintainable codebase.
  - Fixed delayed Enter key handling in terminal input processing to improve stability and responsiveness.

## [0.0.5] - 2026-03-03

### ✨ New Features

- **Bitbucket Cloud Support** — You can now create review PRs and fix PR comments on Bitbucket Cloud repositories, in addition to GitHub. Select your VCS provider from plugin settings.
- **Image Preview** — Added image preview functionality with an enhanced image selection UI for a smoother visual workflow.

### 🔧 Improvements

- **Smarter PR Creation** — CLI tool resolution has been refactored with a REST API fallback, making PR creation more reliable across different environments.
- **Input Bar Layout** — `TerminalInputBar` now uses `FlowRow` for a more flexible and responsive layout.

## [0.0.4] - 2026-03-02
- **Bug Fixes** — Commit message input now properly clears after PR creation, and the plugin gracefully handles cases where the CLI tool is not found by showing an error notification instead of crashing.

## [0.0.3] - 2026-03-02

- **Fix Skiko native library loading on IntelliJ Ultimate** — Compose Desktop now works on IntelliJ IDEA (not just Android Studio) by extracting the Skiko native library from the platform JAR when the module classloader can't expose it
- **Rename plugin** from "Rune" to "Rune"
- **Improved error handling** — Tool window gracefully falls back to Swing UI if Compose initialization fails

## [0.0.2] - 2026-03-02

- Fix Compose panel initialization (skiko.renderApi set earlier via companion init)
- Improve Claude CLI detection (use user's default shell, add Homebrew/nvm/bun/volta fallback paths)
- Update plugin name to Rune


## [0.0.1] - 2026-03-01

Initial internal release

[Unreleased]: https://github.com/cnrture/Rune/compare/v0.0.3...HEAD
[0.0.3]: https://github.com/cnrture/Rune/compare/v0.0.1...v0.0.3

[Unreleased]: https://github.com/cnrture/Rune/compare/v0.0.2...HEAD
[0.0.2]: https://github.com/cnrture/Rune/compare/v0.0.1...v0.0.2

[0.0.1]: https://github.com/cnrture/Rune/commits/v0.0.1
