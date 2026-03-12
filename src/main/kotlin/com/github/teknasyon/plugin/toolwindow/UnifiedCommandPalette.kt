package com.github.teknasyon.plugin.toolwindow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.domain.model.Skill
import com.github.teknasyon.plugin.domain.usecase.ScanSkillsUseCase
import com.github.teknasyon.plugin.service.PluginSettingsService
import com.github.teknasyon.plugin.theme.TPTheme
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

internal enum class PaletteCategory { SKILL, AGENT, COMMAND, SC_COMMAND }

internal enum class PaletteFilter { ALL, SKILLS, AGENTS, COMMANDS, SC_COMMANDS }

internal data class PaletteItem(
    val category: PaletteCategory,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val filePath: String? = null,
    val terminalText: String,
    val autoRun: Boolean,
)

private data class ClaudeCommand(val command: String, val description: String, val icon: ImageVector)

private val claudeCommands = listOf(
    ClaudeCommand("/clear", "Clear conversation", Icons.Rounded.DeleteSweep),
    ClaudeCommand("/compact", "Compact conversation", Icons.Rounded.Compress),
    ClaudeCommand("/config", "Settings (Config)", Icons.Rounded.Settings),
    ClaudeCommand("/context", "Context usage", Icons.Rounded.GridOn),
    ClaudeCommand("/copy", "Copy last response", Icons.Rounded.ContentCopy),
    ClaudeCommand("/cost", "Token usage", Icons.Rounded.AttachMoney),
    ClaudeCommand("/debug", "Debug session", Icons.Rounded.BugReport),
    ClaudeCommand("/desktop", "Switch to desktop", Icons.Rounded.DesktopWindows),
    ClaudeCommand("/doctor", "Health check", Icons.Rounded.HealthAndSafety),
    ClaudeCommand("/exit", "Exit REPL", Icons.AutoMirrored.Rounded.ExitToApp),
    ClaudeCommand("/export", "Export conversation", Icons.Rounded.FileDownload),
    ClaudeCommand("/help", "Usage help", Icons.AutoMirrored.Rounded.Help),
    ClaudeCommand("/init", "Init CLAUDE.md", Icons.AutoMirrored.Rounded.NoteAdd),
    ClaudeCommand("/mcp", "MCP servers", Icons.Rounded.Hub),
    ClaudeCommand("/memory", "Edit memory files", Icons.Rounded.Memory),
    ClaudeCommand("/model", "Change model", Icons.Rounded.SmartToy),
    ClaudeCommand("/permissions", "Permissions", Icons.Rounded.Security),
    ClaudeCommand("/plan", "Plan mode", Icons.Rounded.Map),
    ClaudeCommand("/rename", "Rename session", Icons.Rounded.DriveFileRenameOutline),
    ClaudeCommand("/resume", "Resume session", Icons.Rounded.PlayArrow),
    ClaudeCommand("/rewind", "Rewind conversation", Icons.AutoMirrored.Rounded.Undo),
    ClaudeCommand("/stats", "Usage stats", Icons.Rounded.BarChart),
    ClaudeCommand("/status", "Settings (Status)", Icons.Rounded.Info),
    ClaudeCommand("/statusline", "Status line UI", Icons.Rounded.LinearScale),
    ClaudeCommand("/tasks", "Background tasks", Icons.Rounded.Checklist),
    ClaudeCommand("/teleport", "Remote session", Icons.Rounded.Cloud),
    ClaudeCommand("/theme", "Color theme", Icons.Rounded.Palette),
    ClaudeCommand("/todos", "TODO items", Icons.AutoMirrored.Rounded.FormatListBulleted),
    ClaudeCommand("/usage", "Usage limits", Icons.Rounded.DataUsage),
)

private val scCommands = listOf(
    ClaudeCommand("/sc:analyze", "Code analysis", Icons.Rounded.Analytics),
    ClaudeCommand("/sc:brainstorm", "Requirements discovery", Icons.Rounded.Lightbulb),
    ClaudeCommand("/sc:build", "Build & compile", Icons.Rounded.Build),
    ClaudeCommand("/sc:business-panel", "Business panel analysis", Icons.Rounded.Business),
    ClaudeCommand("/sc:cleanup", "Code cleanup", Icons.Rounded.CleaningServices),
    ClaudeCommand("/sc:design", "System design", Icons.Rounded.Architecture),
    ClaudeCommand("/sc:document", "Generate documentation", Icons.Rounded.Description),
    ClaudeCommand("/sc:estimate", "Development estimates", Icons.Rounded.Timer),
    ClaudeCommand("/sc:explain", "Code explanation", Icons.Rounded.School),
    ClaudeCommand("/sc:git", "Git operations", Icons.Rounded.Hub),
    ClaudeCommand("/sc:help", "SC help", Icons.AutoMirrored.Rounded.Help),
    ClaudeCommand("/sc:implement", "Feature implementation", Icons.Rounded.Code),
    ClaudeCommand("/sc:improve", "Code improvements", Icons.AutoMirrored.Rounded.TrendingUp),
    ClaudeCommand("/sc:index", "Project indexing", Icons.Rounded.FindInPage),
    ClaudeCommand("/sc:index:repo", "Project indexing with more details", Icons.Rounded.FindInPage),
    ClaudeCommand("/sc:load", "Load session context", Icons.Rounded.Download),
    ClaudeCommand("/sc:pm", "Project manager agent", Icons.Rounded.ManageAccounts),
    ClaudeCommand("/sc:recommend", "Command recommendation", Icons.Rounded.Recommend),
    ClaudeCommand("/sc:reflect", "Task reflection", Icons.Rounded.Psychology),
    ClaudeCommand("/sc:research", "Deep web research", Icons.Rounded.Search),
    ClaudeCommand("/sc:save", "Save session context", Icons.Rounded.Save),
    ClaudeCommand("/sc:select-tool", "MCP tool selection", Icons.Rounded.Handyman),
    ClaudeCommand("/sc:spawn", "Task orchestration", Icons.Rounded.AccountTree),
    ClaudeCommand("/sc:spec-panel", "Spec review panel", Icons.Rounded.RateReview),
    ClaudeCommand("/sc:task", "Task management", Icons.Rounded.Task),
    ClaudeCommand("/sc:test", "Test execution", Icons.Rounded.Science),
    ClaudeCommand("/sc:troubleshoot", "Issue diagnosis", Icons.Rounded.Troubleshoot),
    ClaudeCommand("/sc:workflow", "Workflow generation", Icons.Rounded.Route),
)

@Composable
internal fun InlineCommandPanel(
    project: Project,
    scanSkillsUseCase: ScanSkillsUseCase,
    settingsService: PluginSettingsService,
    superClaudeInstalled: Boolean,
    initialFilter: PaletteFilter,
    onDismiss: () -> Unit,
    onItemSelected: (PaletteItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(initialFilter) }
    val searchFocusRequester = remember { FocusRequester() }
    var skills by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var agents by remember { mutableStateOf<List<Skill>>(emptyList()) }
    var selectedIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val skillsRoot = settingsService.getSkillsRootPath()
            scanSkillsUseCase(skillsRoot, true).onSuccess { folders ->
                val all = folders.flatMap { it.getAllSkills() }
                ApplicationManager.getApplication().invokeLater { skills = all }
            }
        }
    }

    LaunchedEffect(Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val agentsRoot = settingsService.getAgentsRootPath()
            scanSkillsUseCase(agentsRoot, false).onSuccess { folders ->
                val all = folders.flatMap { it.getAllSkills() }
                ApplicationManager.getApplication().invokeLater { agents = all }
            }
        }
    }

    val allItems = remember(skills, agents, superClaudeInstalled) {
        buildList {
            skills.forEach { skill ->
                add(
                    PaletteItem(
                        category = PaletteCategory.SKILL,
                        title = skill.relativePath,
                        description = skill.description,
                        icon = Icons.Rounded.AutoFixHigh,
                        filePath = skill.filePath,
                        terminalText = skill.filePath,
                        autoRun = false,
                    )
                )
            }
            agents.forEach { agent ->
                add(
                    PaletteItem(
                        category = PaletteCategory.AGENT,
                        title = agent.relativePath,
                        description = agent.description,
                        icon = Icons.Rounded.Psychology,
                        filePath = agent.filePath,
                        terminalText = agent.filePath,
                        autoRun = false,
                    )
                )
            }
            claudeCommands.forEach { cmd ->
                add(
                    PaletteItem(
                        category = PaletteCategory.COMMAND,
                        title = cmd.command,
                        description = cmd.description,
                        icon = cmd.icon,
                        terminalText = cmd.command,
                        autoRun = true,
                    )
                )
            }
            if (superClaudeInstalled) {
                scCommands.forEach { cmd ->
                    add(
                        PaletteItem(
                            category = PaletteCategory.SC_COMMAND,
                            title = cmd.command,
                            description = cmd.description,
                            icon = cmd.icon,
                            terminalText = cmd.command,
                            autoRun = false,
                        )
                    )
                }
            }
        }
    }

    // Reset selection when search or filter changes
    LaunchedEffect(searchQuery, selectedFilter) {
        selectedIndex = -1
    }

    val filteredItems = remember(allItems, searchQuery, selectedFilter) {
        allItems.filter { item ->
            val matchesFilter = when (selectedFilter) {
                PaletteFilter.ALL -> true
                PaletteFilter.SKILLS -> item.category == PaletteCategory.SKILL || item.category == PaletteCategory.AGENT
                PaletteFilter.AGENTS -> item.category == PaletteCategory.AGENT
                PaletteFilter.COMMANDS -> item.category == PaletteCategory.COMMAND || item.category == PaletteCategory.SC_COMMAND
                PaletteFilter.SC_COMMANDS -> item.category == PaletteCategory.SC_COMMAND
            }
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    val groupedItems = remember(filteredItems) {
        filteredItems.groupBy { it.category }
    }

    Column(
        modifier = modifier
            .background(TPTheme.colors.black)
            .padding(8.dp)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Escape -> {
                            onDismiss()
                            true
                        }
                        Key.DirectionDown -> {
                            if (filteredItems.isNotEmpty()) {
                                selectedIndex = (selectedIndex + 1).coerceAtMost(filteredItems.size - 1)
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (filteredItems.isNotEmpty()) {
                                selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                            }
                            true
                        }
                        Key.Enter -> {
                            if (selectedIndex in filteredItems.indices) {
                                onItemSelected(filteredItems[selectedIndex])
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Search field
        BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = TPTheme.colors.gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .focusRequester(searchFocusRequester),
            textStyle = TextStyle(color = TPTheme.colors.white, fontSize = 14.sp),
            cursorBrush = SolidColor(TPTheme.colors.white),
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = TPTheme.colors.hintGray,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            TPText(
                                text = "Search...",
                                color = TPTheme.colors.hintGray,
                                fontSize = 14.sp,
                            )
                        }
                        innerTextField()
                    }
                }
            },
        )

        Spacer(Modifier.height(8.dp))

        // Category tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val filters = if (initialFilter == PaletteFilter.SKILLS) {
                listOf(PaletteFilter.SKILLS, PaletteFilter.AGENTS)
            } else {
                buildList {
                    add(PaletteFilter.COMMANDS)
                    if (superClaudeInstalled) add(PaletteFilter.SC_COMMANDS)
                }
            }
            filters.forEach { filter ->
                val label = when (filter) {
                    PaletteFilter.ALL -> "All"
                    PaletteFilter.SKILLS -> "Skills"
                    PaletteFilter.AGENTS -> "Agents"
                    PaletteFilter.COMMANDS -> "Commands"
                    PaletteFilter.SC_COMMANDS -> "SC"
                }
                val isSelected = selectedFilter == filter
                TPText(
                    text = label,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            color = if (isSelected) TPTheme.colors.primaryContainer else Color.Transparent,
                        )
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    color = if (isSelected) TPTheme.colors.white else TPTheme.colors.lightGray,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Items grid
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.SearchOff,
                        contentDescription = null,
                        tint = TPTheme.colors.hintGray,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    TPText(text = "No results found", color = TPTheme.colors.lightGray, fontSize = 13.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PaletteCategory.entries.forEach { category ->
                    val categoryItems = groupedItems[category] ?: return@forEach
                    if (categoryItems.isEmpty()) return@forEach

                    item(key = "inline-header-$category", span = { GridItemSpan(2) }) {
                        val headerText = when (category) {
                            PaletteCategory.SKILL -> "SKILLS"
                            PaletteCategory.AGENT -> "AGENTS"
                            PaletteCategory.COMMAND -> "COMMANDS"
                            PaletteCategory.SC_COMMAND -> "SC COMMANDS"
                        }
                        val headerColor = when (category) {
                            PaletteCategory.SKILL -> TPTheme.colors.blue
                            PaletteCategory.AGENT -> TPTheme.colors.blue
                            PaletteCategory.COMMAND -> TPTheme.colors.purple
                            PaletteCategory.SC_COMMAND -> TPTheme.colors.blue
                        }
                        TPText(
                            text = headerText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            color = headerColor.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                    }

                    items(categoryItems, key = { "inline-${it.category}-${it.title}" }) { paletteItem ->
                        val flatIndex = filteredItems.indexOf(paletteItem)
                        val isSelected = flatIndex == selectedIndex
                        val accentColor = when (paletteItem.category) {
                            PaletteCategory.SKILL -> TPTheme.colors.blue
                            PaletteCategory.AGENT -> TPTheme.colors.blue
                            PaletteCategory.COMMAND -> TPTheme.colors.purple
                            PaletteCategory.SC_COMMAND -> TPTheme.colors.blue
                        }
                        val itemHover = remember { MutableInteractionSource() }
                        val isItemHovered by itemHover.collectIsHoveredAsState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    color = when {
                                        isSelected -> accentColor.copy(alpha = 0.25f)
                                        isItemHovered -> accentColor.copy(alpha = 0.15f)
                                        else -> TPTheme.colors.gray.copy(alpha = 0.5f)
                                    },
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        width = 1.dp,
                                        color = accentColor.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp),
                                    ) else Modifier
                                )
                                .hoverable(itemHover)
                                .clickable { onItemSelected(paletteItem) }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = paletteItem.icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                TPText(
                                    text = paletteItem.title,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TPTheme.colors.white,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                )
                                if (paletteItem.description.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    TPText(
                                        text = paletteItem.description,
                                        color = TPTheme.colors.hintGray,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                    )
                                }
                            }
                            if (paletteItem.filePath != null) {
                                @Suppress("DEPRECATION")
                                Icon(
                                    imageVector = Icons.Rounded.OpenInNew,
                                    contentDescription = "Open in editor",
                                    tint = TPTheme.colors.hintGray,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            val vf = LocalFileSystem.getInstance()
                                                .findFileByPath(paletteItem.filePath)
                                            if (vf != null) {
                                                ApplicationManager.getApplication().invokeLater {
                                                    FileEditorManager.getInstance(project)
                                                        .openFile(vf, true)
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
