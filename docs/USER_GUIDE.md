# Rune Plugin - User Guide

A comprehensive guide for using the Rune IntelliJ plugin — Claude CLI integration for JetBrains IDEs.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Getting Started](#getting-started)
- [Claude Terminal](#claude-terminal)
  - [Sessions](#sessions)
  - [Model Picker](#model-picker)
  - [Input Bar](#input-bar)
  - [Remote Control](#remote-control)
- [Command Palette](#command-palette)
- [Commit Message Generation](#commit-message-generation)
- [PR Creation & Review](#pr-creation--review)
- [Fix PR Comments](#fix-pr-comments)
- [Ask Claude](#ask-claude)
- [Skills & Agents](#skills--agents)
  - [Creating a Skill](#creating-a-skill)
  - [Managing Agents](#managing-agents)
  - [Best Practices Check](#best-practices-check)
- [Settings](#settings)
  - [Skills & Agents Directory](#skills--agents-directory)
  - [Commit Message Prompt](#commit-message-prompt)
  - [VCS Provider](#vcs-provider)
  - [GitHub Credentials](#github-credentials)
  - [Bitbucket Cloud Credentials](#bitbucket-cloud-credentials)
  - [Jira Integration](#jira-integration)
- [VCS Platform Support](#vcs-platform-support)
- [Keyboard Shortcuts](#keyboard-shortcuts)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Requirement | Details |
|---|---|
| **JetBrains IDE** | IntelliJ IDEA, Android Studio, WebStorm, PyCharm, GoLand, etc. — version **2024.1+** |
| **Claude CLI** | Installed and accessible in PATH ([install guide](https://docs.anthropic.com/en/docs/claude-code)) |
| **GitHub CLI** | (`gh`) — required only for GitHub PR operations ([install](https://cli.github.com/)) |
| **Java** | 21+ |

---

## Installation

1. Open your IDE and navigate to **Settings** > **Plugins** > **Marketplace**
2. Search for `"Rune"` and click **Install**.
3. Restart your IDE.

After installation, the **Claude** tool window appears in the right sidebar.

---

## Getting Started

1. **Open the Claude panel** — Click the "Claude" tab in the right sidebar.
2. **CLI check** — If Claude CLI is not detected, Rune shows an installation guide with a one-click copy command (`npm install -g @anthropic-ai/claude-code`). Click **Retry** after installing.
3. **Start your first session** — A default session is created automatically. Type a message and press **Ctrl+Enter** to send.

---

## Claude Terminal

The Claude terminal is the core of Rune. It embeds Claude CLI directly inside your IDE with a rich Compose Desktop UI.

### Sessions

Rune supports **multiple concurrent Claude sessions**, each running an independent Claude CLI process.

- **Add session** — Click the **+** button in the session tab bar. A new terminal opens as "Claude 2", "Claude 3", etc.
- **Switch sessions** — Click on any session tab to switch.
- **Close session** — Click the **×** on a session tab. If it's the last session, a new empty one is created.
- **Rename session** — Right-click or use the rename option to give sessions meaningful names.
- **Draft preservation** — Unsent text is saved when switching sessions and restored when you return.

### Model Picker

Click the **model button** in the session tab bar to switch between Claude models:

| Model | ID |
|---|---|
| Opus 4.6 | `claude-opus-4-6` |
| Sonnet 4.6 | `claude-sonnet-4-6` |
| Haiku 4.5 | `claude-haiku-4-5-20251001` |

Your model selection is cached per project and persists across IDE restarts.

### Input Bar

The terminal input bar provides several productivity features:

| Feature | How to Use |
|---|---|
| **Send message** | Type your message and press **Ctrl+Enter** |
| **New line** | Press **Shift+Enter** |
| **File injection** | Click the **@** button to append the current file's relative path |
| **Image attachment** | Click the image button to select and attach an image (PNG, JPG, JPEG, GIF, WEBP, BMP) |
| **Command palette** | Type `/` to open the command palette |
| **URL highlighting** | URLs in your input are visually highlighted |

### Remote Control

Start a Claude remote session that you can access from a browser or phone.

1. Click the **remote control** button in the session tab bar.
2. Optionally enable **"Prevent sleep mode (caffeinate)"** — prevents macOS from sleeping while the remote session is active (screen idle, system idle, and lid-close-while-charging).
3. The session starts with Claude's remote control feature enabled.

---

## Command Palette

Access the command palette by typing `/` in the terminal input or clicking the palette button.

The palette organizes commands into **four categories**:

| Category | Source | Count |
|---|---|---|
| **Skills** | `*SKILL.md` files from your skills directory | Variable |
| **Agents** | Any `.md` files from your agents directory | Variable |
| **Commands** | Built-in Claude CLI commands | 29 |
| **SC Commands** | SuperClaude commands | 25+ |

**Features:**
- **Search filter** — Type to filter commands across all categories
- **Category tabs** — Switch between Skills, Agents, Commands, and SC Commands
- **Grid layout** — Each command shows an icon, title, and description
- **Quick insert** — Click a command to insert it into the terminal input

### Built-in Claude Commands

`/clear`, `/compact`, `/config`, `/context`, `/copy`, `/cost`, `/debug`, `/desktop`, `/doctor`, `/exit`, `/export`, `/help`, `/init`, `/mcp`, `/memory`, `/model`, `/permissions`, `/plan`, `/rename`, `/resume`, `/rewind`, `/stats`, `/status`, `/statusline`, `/tasks`, `/teleport`, `/theme`, `/todos`, `/usage`

### SuperClaude Commands

`/sc:analyze`, `/sc:brainstorm`, `/sc:build`, `/sc:business-panel`, `/sc:cleanup`, `/sc:design`, `/sc:document`, `/sc:estimate`, `/sc:explain`, `/sc:git`, `/sc:help`, `/sc:implement`, `/sc:improve`, `/sc:index`, `/sc:load`, `/sc:pm`, `/sc:recommend`, `/sc:reflect`, `/sc:research`, `/sc:save`, `/sc:select-tool`, `/sc:spec-panel`, `/sc:task`, `/sc:test`, `/sc:troubleshoot`, `/sc:workflow`

---

## Commit Message Generation

Generate AI-powered conventional commit messages from the VCS commit dialog.

### How to Use

1. Stage your changes in the commit dialog.
2. Click **"Generate Commit Message with Claude"** in the commit message toolbar.
3. Claude analyzes staged and unstaged diffs, then streams a conventional commit message directly into the message field.

### How It Works

- Diffs are collected via `git diff --cached` (staged) and `git diff` (unstaged).
- The diff is sent to Claude CLI via stdin with your customizable prompt template.
- Response streams in real-time to the commit message field.
- Diffs are capped at **8,000 characters** to avoid token overflow.
- A **30-second timeout** prevents indefinite hangs.

### Fallback Logic

If Claude is unavailable or times out, Rune generates a deterministic commit message by analyzing:

- **File extensions** — `.md` files → `docs:`, test files → `test:`
- **Change ratios** — More additions → `feat:`, more removals → `refactor:`

### Jira Integration

When enabled in settings, Rune:
1. Extracts the Jira ticket ID from your branch name (pattern: `[A-Za-z]+-\d+`, e.g., `feature/PROJ-123-add-login`)
2. Appends the Jira issue URL to the commit message

---

## PR Creation & Review

Create pull requests with a single click from the commit dialog.

### How to Use

1. Click **"Create Review PR"** in the VCS commit dialog toolbar.
2. Rune auto-detects:
   - Repository owner and name from the git remote URL
   - Base branch (checks for `develop`, `main`, `master`, `staging`, `release`)
3. The PR creation dialog opens with:
   - **Reviewer selection** — Searchable list of repository contributors
   - **Label selection** — Pick existing labels or create new ones (GitHub only)
   - **Base branch override** — Dropdown to change the target branch
4. Click **Create** — the current branch is pushed and the PR is opened.

### Review Branch Prefix

When **"Use review/ branch prefix"** is enabled in settings, Rune creates a `review/{branch}` target branch from the base. This is useful for code review workflows where reviews happen on separate branches before merging to the main branch.

### Jira Auto-Label

If Jira credentials are configured and a ticket ID is found in the branch name, Rune automatically selects labels that match the Jira ticket's Fix Version.

---

## Fix PR Comments

Resolve unresolved PR review comments using Claude.

### How to Use

1. Click **"Fix PR Comments"** in the VCS commit dialog toolbar.
2. Enter the PR URL when prompted.
3. Rune fetches all unresolved review threads showing:
   - File path and line number
   - Reviewer name
   - Comment body
   - Surrounding code context (diff hunk)
4. Select the comments you want Claude to fix.
5. The selected comments are formatted and sent to the Claude terminal for resolution.

### Supported Platforms

| Platform | API Used |
|---|---|
| GitHub | GraphQL API (unresolved review threads) |
| Bitbucket Cloud | REST API (comments with status != RESOLVED) |

---

## Ask Claude

Send any code selection to Claude for explanation, review, or help.

### How to Use

1. Select code in the editor (or console output).
2. Right-click and choose **"Ask Claude"**.
3. The selected code is sent to the Claude terminal along with:
   - File path
   - Line number context
4. The Claude tool window opens automatically if not already visible.

---

## Skills & Agents

Rune supports custom skills and agents defined as Markdown files.

- **Skills** — Files matching `*SKILL.md` in the configured skills directory
- **Agents** — Any `.md` file in the configured agents directory

Both are automatically discovered, cached for 5 minutes, and accessible via the command palette.

### Creating a Skill

1. Click **"Create Skill"** in the session tab bar (or access via right-click menu).
2. Fill in the creation dialog:

| Field | Requirements |
|---|---|
| **Name** | kebab-case (`my-skill`), max 64 characters, no reserved words (`anthropic`, `claude`) |
| **Description** | Max 1,024 characters, no XML tags |
| **Save Path** | Directory to save the file (defaults to your configured skills root) |
| **Workflow section** | Optional — adds a workflow template |
| **Examples section** | Optional — adds an examples template |
| **References section** | Optional — adds a references template |

**Validation rules:**
- Name must match: `^[a-z0-9]+(-[a-z0-9]+)*$`
- Name cannot contain reserved words: `anthropic`, `claude`
- Name cannot be vague: `helper`, `utils`, `tools`, `data`, `files`, `stuff`, `misc`
- Description warnings for vague language or generic phrases

The skill is saved as `{name}-SKILL.md` in the selected directory.

### Managing Agents

Agents are simpler than skills — any `.md` file in the agents directory is automatically discovered. The first non-empty line of the file body becomes the agent's description in the UI.

### Best Practices Check

When you open a `SKILL.md` file, an editor notification banner appears with a **"Check with Claude"** action.

The check validates **8 categories**:

1. **Frontmatter** — Name format, description quality
2. **Naming** — Gerund forms, avoiding vague words
3. **Description** — Third person, non-time-sensitive language
4. **Body size** — Under 500 lines
5. **Structure** — Progressive disclosure, table of contents for large files
6. **Workflows** — Checklist patterns, feedback loops
7. **Content** — No magic numbers, consistent terminology
8. **Code** — Explicit error handling, no Windows-specific paths

---

## Settings

Open **Settings** > **Tools** > **Rune Settings** to configure the plugin.

### Skills & Agents Directory

| Setting | Default | Description |
|---|---|---|
| **Skills Directory** | `.claude/skills` | Directory scanned for `*SKILL.md` files |
| **Agents Directory** | `.claude/agents` | Directory scanned for any `.md` files |

Both have a file browser selector. Changing these paths automatically invalidates the file scanner cache.

### Commit Message Prompt

A multi-line text area where you customize the prompt sent to Claude for commit message generation.

- Use the `{diff}` placeholder to inject the git diff
- Click **Reset** to restore the default prompt
- The default prompt instructs Claude to generate conventional commits based on the diff

### VCS Provider

| Option | Description |
|---|---|
| **GitHub** | Uses GitHub REST API and `gh` CLI |
| **Bitbucket Cloud** | Uses Bitbucket REST API with basic auth |

The provider is **auto-detected** from your git remote URL (SSH or HTTPS). You can override it manually. Settings visibility adjusts based on the selected provider.

### GitHub Credentials

- **Personal Access Token** — Required scope: `repo`
- Create at: GitHub > Settings > Developer settings > Personal access tokens
- Securely stored in the IDE credential store
- Shows "Token saved" indicator when configured

### Bitbucket Cloud Credentials

- **Email** — Your Atlassian account email
- **API Token** — Create at: id.atlassian.com > Security > API tokens
- Securely stored in the IDE credential store

### Jira Integration

| Setting | Description |
|---|---|
| **Base URL** | Your Jira instance URL (e.g., `https://yourcompany.atlassian.net`) |
| **Email** | Atlassian account email |
| **API Token** | Atlassian API token |

When configured, Jira integration enables:
- Automatic ticket ID extraction from branch names
- Ticket URL appended to commit messages
- Auto-selection of labels matching Jira Fix Version when creating PRs

---

## VCS Platform Support

| Feature | GitHub | Bitbucket Cloud |
|---|---|---|
| PR creation | Yes | Yes |
| Reviewer selection | Yes (contributors) | Yes (workspace members) |
| Label selection | Yes (create new labels) | No |
| Assignee support | Yes | No |
| PR comment fetching | Yes (GraphQL API) | Yes (REST API) |
| Auto-detect from remote | Yes | Yes |
| Authentication | Token (repo scope) | Email + API token |

**Remote URL detection** supports both SSH (`git@github.com:owner/repo.git`) and HTTPS (`https://github.com/owner/repo.git`) formats.

---

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| **Ctrl+Enter** | Send message in Claude terminal |
| **Shift+Enter** | Insert new line in terminal input |
| **/** (in terminal input) | Open command palette |
| **Right-click > Ask Claude** | Send selected code to Claude |
| **Right-click > Check Skill with Claude** | Validate SKILL.md file |

---

## Troubleshooting

### Claude CLI not found

**Symptom:** The Claude panel shows an installation guide instead of a terminal.

**Solution:**
1. Install Claude CLI: `npm install -g @anthropic-ai/claude-code`
2. Ensure `claude` is accessible in your PATH.
3. Click **Retry** in the Rune panel.

Rune searches for `claude` in the following locations:
- Login shell `which claude` result
- `/usr/local/bin/claude`
- `/usr/bin/claude`
- `/opt/homebrew/bin/claude` (Homebrew)
- `~/.npm-global/bin/claude`
- `~/.local/bin/claude`
- `~/.nvm/current/bin/claude` (NVM)
- `~/.bun/bin/claude` (Bun)
- `~/.volta/bin/claude` (Volta)

### Commit message generation fails

- **Timeout:** Claude has a 30-second timeout. Large diffs (>8,000 chars) are truncated automatically.
- **Fallback:** If Claude is unavailable, a deterministic message is generated based on file types and change ratios.
- **No changes:** If there are no staged or unstaged changes, a warning notification is shown.

### PR creation issues

- **"Token missing"** — Configure your GitHub/Bitbucket credentials in Settings > Tools > Rune Settings.
- **"PR already exists"** — A PR for this branch already exists. The existing PR URL is shown in the notification.
- **Base branch not detected** — Override the base branch manually in the PR creation dialog.

### Skills/Agents not appearing

- Verify your skills/agents directory path in Settings > Tools > Rune Settings.
- Skills must match the `*SKILL.md` pattern; agents can be any `.md` file.
- The file scanner cache refreshes every 5 minutes. Change the directory path to force a refresh.

### UI rendering issues

Rune uses Jetpack Compose Desktop with **software rendering** (Skiko) for maximum compatibility. If you experience visual glitches:
- Ensure your IDE is up to date (2024.1+).
- Check that Java 21+ is being used.
- Try restarting the IDE.
