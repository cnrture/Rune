# 🤖 Teknasyon IntelliJ AI

![Build](https://github.com/Teknasyon/IntelliJ-AI-Plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

🌐 **[Türkçe](README.tr.md)** | English

<!-- Plugin description -->
AI-powered development assistant for JetBrains IDEs. Claude CLI integration, automatic commit message generation, PR management, skill/agent system and more. Works with IntelliJ IDEA, Android Studio, WebStorm, PyCharm and all other JetBrains IDEs.
<!-- Plugin description end -->

## ✨ Features

### 🖥️ Claude Terminal Integration

The plugin adds a **Claude** tool window to the IDE's right sidebar. Claude CLI runs directly inside this panel.

- 📑 **Multi-session support** — Open multiple Claude terminals and switch between them
- 🎯 **Command palette** — Access skills, agents, Claude commands and SuperClaude commands by typing `/` or clicking the button
- 📎 **File injection** — Append the current file's path to your message with the `@` button
- 🖼️ **Image attachment** — Select and send images along with your message
- 🌐 **Remote control** — Start a remote Claude session with optional `caffeinate` sleep prevention (macOS)

> **⚙️ How it works:** `ClaudeSessionService` manages sessions. The terminal uses IntelliJ's native `JBTerminalWidget`. Claude CLI is located via PATH; if not found, common locations like `~/.npm-global/bin`, `~/.local/bin`, `/usr/local/bin` are checked as fallback.

---

### 💬 Automatic Commit Message Generation

The **"Generate Commit Message with Claude"** button in the VCS commit dialog generates commit messages from staged changes.

- 📝 `git diff --cached` and `git diff` outputs are sent to Claude CLI
- 📏 Messages are generated in conventional commit format
- 🔗 Optional Jira integration: ticket ID is extracted from the branch name and its URL is appended to the commit message
- 🔄 If Claude is unavailable, a fallback logic kicks in (determines type by analyzing file extensions and changes)

> **⚙️ How it works:** `GenerateCommitMessageAction` runs `claude -p <prompt>` with a 30-second timeout. The output is streamed into the commit message field.

---

### 🚀 PR Creation

Create a pull request with a single click using the **"Create Review PR"** button.

- 🌿 Base branch is auto-detected (via git reflog or closest ancestor branch: develop, main, master, staging, release)
- 🏷️ Optional `review/` branch prefix
- 👥 Reviewer selection — searchable list from repo contributors
- 🏷️ Label selection — pick from existing labels or create new ones
- 🎫 Jira integration: auto-selects labels based on ticket Fix Version

> **⚙️ How it works:** `CreateReviewPRAction` detects the base branch, pushes the branch, then opens `CreatePRDialog`. The PR is created via GitHub CLI (`gh pr create`). Collaborator and label data is cached by `GitHubCacheService`.

---

### 🔧 Fix PR Comments

The **"Fix PR Comments"** button resolves unresolved PR review comments with Claude.

- 🔗 Enter the PR URL
- 📡 Unresolved review threads are fetched via GitHub GraphQL API
- 📋 Comments are listed with file path, line number, reviewer name and code context
- ✅ Select the comments you want to fix and send them to Claude

> **⚙️ How it works:** `FixPRCommentsAction` fetches comment data with a GraphQL query. `FixPRCommentsDialog` displays the comments. Selected comments are formatted and pasted into the Claude terminal.

---

### 💡 Ask Claude (Code Query)

Select code in the editor and use **"Ask Claude"** from the right-click menu.

- 📄 Selected code is sent to Claude with file path and line number context
- 🪟 Claude tool window opens automatically
- 🖥️ Also works for console output

> **⚙️ How it works:** `AskClaudeAction` injects the selected text and file context into the terminal input field via `ClaudeSessionService.setPendingInput()`.

---

### 🧩 Skill & Agent System

Skills and agents are defined as Markdown files and are automatically discovered by the plugin.

- 📘 **Skills** — Files with `SKILL.md` suffix, scanned from a configured directory
- 🤖 **Agents** — Any `.md` file, scanned from a separate configured directory
- ✏️ **Skill creation** — Define name, description, workflow, examples and references via `CreateSkillDialog`
- ✅ **Validation** — Name format, description quality and frontmatter structure are checked

> **⚙️ How it works:** `SkillRepositoryImpl` recursively scans the configured directory. Uses a 5-minute in-memory cache. The first non-empty line of the file body becomes the description.

---

### 📋 Skill Best Practices Check

A notification banner appears in the editor when a `SKILL.md` file is opened.

- 📖 **"Open best practices"** — Opens the Claude documentation in the browser
- 🔍 **"Check with Claude"** — Sends the skill content to Claude for a detailed review (frontmatter, naming, structure, content quality)

---

### 🎨 Command Palette

Opens by typing `/` in the terminal input field or clicking the button. Lists searchable content in 4 categories:

| Category | Source |
|---|---|
| 📘 Skills | SKILL.md files in the skills directory |
| 🤖 Agents | .md files in the agents directory |
| ⌨️ Commands | 29 built-in Claude commands (`/clear`, `/model`, `/compact`...) |
| ⚡ SC Commands | 25 SuperClaude commands (`/sc:analyze`, `/sc:design`...) |

---

## ⚙️ Settings

Configured via **Settings > Tools > Teknasyon Plugin Settings**.

| Setting | Description |
|---|---|
| 📁 Skills Directory | Directory to scan for skill files |
| 📁 Agents Directory | Directory to scan for agent files |
| 💬 Commit Message Prompt | Customizable prompt template sent to Claude |
| 🔗 Jira Ticket URL | Append Jira ticket link to commit messages |
| 🌿 Review Branch | Use `review/` branch prefix for PRs |
| 🔑 Jira Credentials | Jira email and API token (securely stored in IDE credential store) |

---

## 📦 Installation

1. In your IDE, go to <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Manage Plugin Repositories...</kbd>
2. Add the following URL:
   ```
   https://raw.githubusercontent.com/Teknasyon/intellij-ai-plugin-releases/main/updatePlugins.xml
   ```
3. In the <kbd>Marketplace</kbd> tab, search for `"Teknasyon IntelliJ AI"` and click <kbd>Install</kbd>

## 📋 Requirements

- 🧰 JetBrains IDE (IntelliJ IDEA, Android Studio, WebStorm, PyCharm, GoLand, etc.) 2024.1+
- 🤖 [Claude CLI](https://docs.anthropic.com/en/docs/claude-code) installed and accessible in PATH
- 🐙 [GitHub CLI](https://cli.github.com/) (`gh`) — for PR operations
- ☕ Java 21+

## 🛠️ Development

```bash
# Run plugin in a sandboxed IDE instance
./gradlew runIde

# Run tests
./gradlew check

# Build distribution ZIP
./gradlew buildPlugin
```

**Tech stack:** Kotlin 2.3.0 · Jetpack Compose Desktop 1.10.1 · IntelliJ Platform SDK · FreeMarker

---

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
