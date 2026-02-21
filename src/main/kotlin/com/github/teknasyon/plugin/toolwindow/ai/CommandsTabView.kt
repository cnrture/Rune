package com.github.teknasyon.plugin.toolwindow.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.teknasyon.plugin.theme.TPTheme

data class ClaudeCommand(val name: String, val description: String)

private fun claudeCommands() = listOf(
    ClaudeCommand("/clear", "Clear conversation history"),
    ClaudeCommand("/compact", "Compact conversation with optional focus instructions"),
    ClaudeCommand("/config", "Open the Settings interface (Config tab)"),
    ClaudeCommand("/context", "Visualize current context usage as a colored grid"),
    ClaudeCommand("/cost", "Show token usage statistics"),
    ClaudeCommand("/debug", "Troubleshoot the current session by reading the debug log"),
    ClaudeCommand("/doctor", "Checks the health of your Claude Code installation"),
    ClaudeCommand("/exit", "Exit the REPL"),
    ClaudeCommand("/export", "Export the current conversation to a file or clipboard"),
    ClaudeCommand("/help", "Get usage help"),
    ClaudeCommand("/init", "Initialize project with CLAUDE.md guide"),
    ClaudeCommand("/mcp", "Manage MCP server connections and OAuth authentication"),
    ClaudeCommand("/memory", "Edit CLAUDE.md memory files"),
    ClaudeCommand("/model", "Select or change the AI model"),
    ClaudeCommand("/permissions", "View or update permissions"),
    ClaudeCommand("/plan", "Enter plan mode directly from the prompt"),
    ClaudeCommand("/rename", "Rename the current session for easier identification"),
    ClaudeCommand("/resume", "Resume a conversation by ID or name, or open the session picker"),
    ClaudeCommand("/rewind", "Rewind the conversation and/or code"),
    ClaudeCommand("/stats", "Visualize daily usage, session history, streaks, and model preferences"),
    ClaudeCommand("/status", "Open the Settings interface (Status tab)"),
    ClaudeCommand("/statusline", "Set up Claude Code's status line UI"),
    ClaudeCommand("/copy", "Copy the last assistant response to clipboard"),
    ClaudeCommand("/tasks", "List and manage background tasks"),
    ClaudeCommand("/teleport", "Resume a remote session from claude.ai"),
    ClaudeCommand("/desktop", "Hand off the current CLI session to the Claude Code Desktop app"),
    ClaudeCommand("/theme", "Change the color theme"),
    ClaudeCommand("/todos", "List current TODO items"),
    ClaudeCommand("/usage", "Show plan usage limits and rate limit status"),
)

@Composable
fun CommandsTabView(
    searchQuery: String,
    onRunCommand: (String) -> Unit,
) {
    val allCommands = remember { claudeCommands() }
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) allCommands
        else allCommands.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    if (filtered.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No commands found",
                style = MaterialTheme.typography.body2,
                color = TPTheme.colors.lightGray,
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(filtered) { command ->
                CommandCard(command = command, onRun = { onRunCommand(command.name) })
            }
        }
    }
}

@Composable
private fun CommandCard(command: ClaudeCommand, onRun: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = TPTheme.colors.gray,
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = command.name,
                    color = TPTheme.colors.blue,
                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                    onClick = onRun,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = TPTheme.colors.white,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = command.description,
                color = TPTheme.colors.lightGray,
                style = MaterialTheme.typography.caption,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
