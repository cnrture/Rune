<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# IntelliJ-AI-Plugin Changelog

## [Unreleased]


## [0.0.3] - 2026-03-02

- **Fix Skiko native library loading on IntelliJ Ultimate** — Compose Desktop now works on IntelliJ IDEA (not just Android Studio) by extracting the Skiko native library from the platform JAR when the module classloader can't expose it
- **Rename plugin** from "Teknasyon IntelliJ AI" to "Teknasyon AI"
- **Improved error handling** — Tool window gracefully falls back to Swing UI if Compose initialization fails

## [0.0.2] - 2026-03-02

- Fix Compose panel initialization (skiko.renderApi set earlier via companion init)
- Improve Claude CLI detection (use user's default shell, add Homebrew/nvm/bun/volta fallback paths)
- Update plugin name to Teknasyon IntelliJ AI


## [0.0.1] - 2026-03-01

Initial internal release

[Unreleased]: https://github.com/Teknasyon/IntelliJ-AI-Plugin/compare/v0.0.3...HEAD
[0.0.3]: https://github.com/Teknasyon/IntelliJ-AI-Plugin/compare/v0.0.1...v0.0.3

[Unreleased]: https://github.com/Teknasyon/IntelliJ-AI-Plugin/compare/v0.0.2...HEAD
[0.0.2]: https://github.com/Teknasyon/IntelliJ-AI-Plugin/compare/v0.0.1...v0.0.2

[0.0.1]: https://github.com/Teknasyon/IntelliJ-AI-Plugin/commits/v0.0.1
