<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Rune Changelog

## [Unreleased]

## [0.0.4] - 2026-03-25

### 🐛 Bug Fixes

- **Version Parsing Fix** — Fixed version parsing in terminal input bar by limiting version part length to 2 digits, preventing incorrect version detection.

## [0.0.3] - 2026-03-25

### ✨ New Features

- **Key Event Dispatcher**: Added key event dispatcher for terminal interactions, enabling keyboard-driven navigation and actions

## [0.0.2] - 2026-03-24

### ✨ New Features

- **Bitbucket Support**: Added Bitbucket Cloud integration with PR creation and VCS provider auto-detection alongside GitHub
- **Image Preview**: Added image preview functionality with enhanced image selection UI
- **SKILL.md Best Practices Checker**: New action and editor notification to validate SKILL.md files against Claude best practices
- **Remote Command Fetching**: Commands are now fetched remotely with local caching and stale check mechanism
- **Model Selection**: Implemented model selection and caching for Claude terminal sessions
- **`/stats` Command**: New command to display usage statistics
- **Slash Command Handling**: Full slash command support in the terminal input bar
- **URL Highlighting**: URLs in terminal input are now highlighted with enhanced keyboard navigation
- **Draft Input Handling**: Terminal input bar now preserves draft text for improved user experience
- **Character Count**: Input field displays character count when text is entered

### 🔧 Improvements

- **Configurable Jira Base URL**: Jira integration now supports custom base URLs
- **Enhanced PR Dialog**: Added base branch selection and GitHub API client for better credential management
- **CLI Resolution**: Improved CLI tool resolution with login shell path detection and REST API fallback for PR creation
- **Cross-Platform Compatibility**: Bundled all platform Skiko natives (macOS arm64/x64, Linux x64/arm64, Windows x64)
- **Animated Terminal Panel**: Terminal panel now features animated visibility transitions and improved layout
- **Command Palette Enhancements**: New commands added with fallback icons and loading indicator
- **Refreshed Theme**: Updated color scheme and plugin icon with new gradient design
- **Light & Dark Theme Icons**: Added dedicated SVG icons for both light and dark IDE themes
- **Streamlined Settings**: Skills and Agents directory settings combined into a two-column layout

### 🐛 Fixes

- **Terminal Starter Path**: Fixed command path resolution for terminal starter in ClaudeSessionService
- **Unused Dependencies**: Removed unused FreeMarker and kotlinx-serialization dependencies
- **Plugin Compatibility**: Updated `pluginSinceBuild` to 243 for broader IDE compatibility
- **Compose Panel Init**: Added error handling for Compose panel initialization
- **Panel Close Behavior**: Skills/commands panel now closes correctly on terminal area click

### 📝 Other

- Added Apache License 2.0
- Renamed plugin to "Rune" with updated configurations
- Updated to Kotlin 2.3.20 and Gradle 9.4.1

## [0.0.1] - 2026-03-01

Initial release

[Unreleased]: https://github.com/cnrture/Rune/compare/v0.0.3...HEAD
[0.0.4]: https://github.com/cnrture/Rune/compare/v0.0.3...v0.0.4
[0.0.3]: https://github.com/cnrture/Rune/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/cnrture/Rune/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/cnrture/Rune/commits/v0.0.1
