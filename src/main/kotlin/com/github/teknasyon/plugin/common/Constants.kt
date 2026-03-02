package com.github.teknasyon.plugin.common

object Constants {
    const val EMPTY = ""

    const val DEFAULT_EXIT_CODE = 2

    // URLs
    const val JIRA_BASE_URL = "https://pozitim.atlassian.net"
    const val GH_CLI_INSTALL_URL = "https://cli.github.com"
    const val CLAUDE_SKILL_BEST_PRACTICES_URL =
        "https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices.md"

    // Regex patterns
    val JIRA_TICKET_REGEX = Regex("[A-Z]+-\\d+")

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
    const val DELAY_NEW_SESSION_CLI_MS = 3000L

    // Messages
    const val GH_CLI_NOT_FOUND_MESSAGE =
        "GitHub CLI (gh) not found. Install from $GH_CLI_INSTALL_URL and run 'gh auth login'."

    fun jiraBrowseUrl(ticketId: String): String = "$JIRA_BASE_URL/browse/$ticketId"
}