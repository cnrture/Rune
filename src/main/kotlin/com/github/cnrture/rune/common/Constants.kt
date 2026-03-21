package com.github.cnrture.rune.common

object Constants {
    const val EMPTY = ""

    const val DEFAULT_EXIT_CODE = 2

    // URLs
    const val CLAUDE_SKILL_BEST_PRACTICES_URL =
        "https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices.md"

    // Regex patterns
    val JIRA_TICKET_REGEX = Regex("[A-Za-z]+-\\d+")

    // Timeouts (seconds)
    const val TIMEOUT_CLI_LOOKUP_SECONDS = 5L
    const val TIMEOUT_PROCESS_DEFAULT_SECONDS = 30L
    const val TIMEOUT_HTTP_MS = 10_000
    const val TIMEOUT_CLAUDE_STREAM_MS = 30_000L
    const val TIMEOUT_PROCESS_CLEANUP_SECONDS = 5L

    // Terminal interaction delays (milliseconds)
    const val DELAY_MENU_RENDER_MS = 1000L
    const val DELAY_KEY_INPUT_MS = 200L
    const val DELAY_CLI_STARTUP_MS = 1500L
    const val DELAY_SEND_ENTER_MS = 150L
    const val DELAY_NEW_SESSION_CLI_MS = 3000L

    // Messages
    const val GITHUB_TOKEN_MISSING_MESSAGE =
        "GitHub token not configured. Go to Settings > Tools > Rune Settings."
    const val BITBUCKET_CREDENTIALS_MISSING_MESSAGE =
        "Bitbucket credentials not configured. Go to Settings > Tools > Rune Settings."
}
