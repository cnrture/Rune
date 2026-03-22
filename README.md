# Rune

![Build](https://github.com/cnrture/Rune/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

**[Turkce](README.tr.md)** | English

<!-- Plugin description -->
Seamlessly integrate **Claude CLI** into your JetBrains IDE with a powerful terminal-based workflow. Rune brings AI-powered commit messages, PR management, code queries, and an extensible skill/agent system — all from a single tool window.
<!-- Plugin description end -->

## Features

### Claude Terminal

Embedded Claude CLI sessions in the IDE's right sidebar.

- **Multi-session** — Open multiple Claude terminals, switch between them
- **Model picker** — Quick switch between Opus, Sonnet, and Haiku models
- **Command palette** — Access skills, agents, Claude commands and SuperClaude commands by typing `/`
- **File injection** — Append the current file's path to your message with the `@` button
- **Image attachment** — Select and send images along with your message
- **Remote control** — Start a remote Claude session with optional `caffeinate` sleep prevention (macOS)

---

### Commit Message Generation

**"Generate Commit Message with Claude"** button in the VCS commit dialog.

- Generates conventional commit messages from staged and unstaged diffs
- Customizable prompt template
- Optional Jira integration — ticket ID extracted from branch name, URL appended to message
- Fallback logic when Claude is unavailable — determines type by analyzing file extensions and change ratios

---

### PR Creation & Review

**"Create Review PR"** button — create a pull request with a single click.

- Base branch auto-detection (develop, main, master, staging, release)
- Optional `review/` branch prefix
- Searchable reviewer selection from repo contributors
- Label selection — pick existing or create new
- Jira integration — auto-selects labels based on ticket Fix Version
- **Supports GitHub (via `gh` CLI) and Bitbucket Cloud (via REST API)**

---

### Fix PR Comments

**"Fix PR Comments"** button — resolve unresolved review comments with Claude.

- Fetches unresolved review threads via GitHub GraphQL API or Bitbucket REST API
- Lists comments with file path, line number, reviewer name, and code context
- Select specific comments to fix — formatted and sent to Claude terminal

---

### Ask Claude

Right-click any code selection in the editor and choose **"Ask Claude"**.

- Selected code is sent to Claude with file path and line number context
- Claude tool window opens automatically
- Also works in console output

---

### Skill & Agent System

Custom skills and agents defined as Markdown files, automatically discovered by the plugin.

- **Skills** — Files with `SKILL.md` suffix in the configured skills directory
- **Agents** — Any `.md` file in the configured agents directory
- **Skill creation dialog** — Define name, description, workflow, examples, and references
- **Validation** — Name format, description quality, and frontmatter structure checks
- **Best practices check** — Notification banner on SKILL.md files with "Check with Claude" action

---

### Command Palette

Type `/` in the terminal input or click the palette button. Searchable content in 4 categories:

| Category | Source |
|---|---|
| Skills | SKILL.md files in the skills directory |
| Agents | .md files in the agents directory |
| Commands | 29 built-in Claude commands (`/clear`, `/model`, `/compact`...) |
| SC Commands | 25+ SuperClaude commands (`/sc:analyze`, `/sc:design`...) |

---

## Settings

**Settings > Tools > Rune Settings**

| Setting | Description |
|---|---|
| Skills Directory | Directory to scan for skill files (default: `.claude/skills`) |
| Agents Directory | Directory to scan for agent files (default: `.claude/agents`) |
| Commit Message Prompt | Customizable prompt template sent to Claude |
| Jira Ticket URL | Append Jira ticket link to commit messages |
| Review Branch | Use `review/` branch prefix for PRs |
| VCS Provider | GitHub or Bitbucket Cloud (auto-detected from git remote) |
| GitHub Credentials | GitHub token (securely stored in IDE credential store) |
| Bitbucket Credentials | Bitbucket username and API token |
| Jira Credentials | Jira email and API token |

---

## Installation

1. In your IDE, go to <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd>
2. Search for `"Rune"` and click <kbd>Install</kbd>

## Requirements

- JetBrains IDE (IntelliJ IDEA, Android Studio, WebStorm, PyCharm, GoLand, etc.) **2024.1+**
- [Claude CLI](https://docs.anthropic.com/en/docs/claude-code) installed and accessible in PATH
- [GitHub CLI](https://cli.github.com/) (`gh`) — required for GitHub PR operations
- Java 21+

## Development

```bash
# Run plugin in a sandboxed IDE instance
./gradlew runIde

# Run tests
./gradlew check

# Build distribution ZIP
./gradlew buildPlugin

# Verify plugin compatibility
./gradlew runPluginVerifier
```

**Tech stack:** Kotlin 2.3.10 · Jetpack Compose Desktop 1.10.1 · IntelliJ Platform SDK · FreeMarker 2.3.34

---

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
