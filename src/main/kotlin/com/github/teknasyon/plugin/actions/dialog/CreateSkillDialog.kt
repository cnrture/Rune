package com.github.teknasyon.plugin.actions.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.common.Constants
import com.github.teknasyon.plugin.components.*
import com.github.teknasyon.plugin.service.PluginSettingsService
import com.github.teknasyon.plugin.theme.TPTheme
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import java.io.File
import java.util.*

private data class CreateSkillState(
    val name: String = "",
    val description: String = "",
    val savePath: String = "",
    val addWorkflow: Boolean = false,
    val addExamples: Boolean = false,
    val addReferences: Boolean = false,
    val errorMessage: String? = null,
)

class CreateSkillDialog(
    private val project: Project,
) : TPDialogWrapper(
    width = 700,
    height = 600,
) {

    private var state = mutableStateOf(
        CreateSkillState(
            savePath = resolveDefaultSavePath(),
        )
    )

    private val nameRegex = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    private val reservedWords = listOf("anthropic", "claude")

    init {
        title = "Create New Skill"
    }

    private fun resolveDefaultSavePath(): String {
        val configured = PluginSettingsService.getInstance(project).getSkillsRootPath()
        if (configured.isNotBlank()) return configured
        return project.basePath ?: ""
    }

    // region Validation

    private fun nameErrors(name: String): List<String> {
        if (name.isBlank()) return listOf("Name is required")
        val errors = mutableListOf<String>()
        if (name.contains('<') || name.contains('>')) errors.add("Name cannot contain XML tags")
        if (name.length > 64) errors.add("Name must be 64 characters or less")
        if (!nameRegex.matches(name)) errors.add("Only lowercase letters, numbers, and hyphens allowed (e.g. my-skill)")
        reservedWords.forEach { word ->
            if (name.contains(word)) errors.add("Name cannot contain \"$word\"")
        }
        return errors
    }

    private fun descriptionErrors(description: String): List<String> {
        if (description.isBlank()) return listOf("Description is required")
        val errors = mutableListOf<String>()
        if (description.length > 1024) errors.add("Description must be 1024 characters or less")
        if (Regex("<[^>]+>").containsMatchIn(description)) errors.add("Description cannot contain XML tags")
        return errors
    }

    private fun descriptionWarnings(description: String): List<String> {
        if (description.isBlank()) return emptyList()
        val warnings = mutableListOf<String>()
        if (description.startsWith("I ") || description.startsWith("You ")) {
            warnings.add("Description should be in third person")
        }
        if (description.length in 1..19) {
            warnings.add("Description seems too short, be more specific")
        }
        val vagueStarts = listOf("helps with", "does stuff", "processes data")
        if (vagueStarts.any { description.lowercase().startsWith(it) }) {
            warnings.add("Description is too vague, describe specific capabilities")
        }
        return warnings
    }

    private val vagueNameWords = setOf("helper", "utils", "tools", "documents", "data", "files", "stuff", "misc")

    private fun nameWarnings(name: String): List<String> {
        if (name.isBlank() || nameErrors(name).isNotEmpty()) return emptyList()
        val warnings = mutableListOf<String>()
        val parts = name.split("-")
        if (parts.any { it in vagueNameWords }) {
            warnings.add("Avoid vague names, be more specific about what the skill does")
        }
        return warnings
    }

    private fun descriptionHints(description: String): List<String> {
        if (description.isBlank() || descriptionErrors(description).isNotEmpty()) return emptyList()
        val hints = mutableListOf<String>()
        if (!description.lowercase().contains("when")) {
            hints.add("Include when to use this skill, e.g. 'Use when...'")
        }
        return hints
    }

    private fun nameHints(name: String): List<String> {
        if (name.isBlank() || nameErrors(name).isNotEmpty()) return emptyList()
        val hints = mutableListOf<String>()
        if (!name.endsWith("ing") && !name.contains("-ing-") && !name.split("-").any { it.endsWith("ing") }) {
            hints.add("Consider using gerund form: e.g. processing-pdfs")
        }
        return hints
    }

    private fun isValid(): Boolean {
        val s = state.value
        return nameErrors(s.name).isEmpty() &&
            descriptionErrors(s.description).isEmpty() &&
            s.savePath.isNotBlank()
    }

    // endregion

    // region Preview generation

    private fun generatePreview(): String {
        val s = state.value
        val displayName = s.name.ifBlank { "my-skill" }
        val displayDesc = s.description.ifBlank { "A short description of what this skill does" }
        val titleCase = displayName.split("-").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

        return buildString {
            appendLine("---")
            appendLine("name: $displayName")
            appendLine("description: $displayDesc")
            appendLine("---")
            appendLine()
            appendLine("# $titleCase")
            appendLine()
            appendLine("## Quick start")
            appendLine("<!-- Your instructions here -->")

            if (s.addWorkflow) {
                appendLine()
                appendLine("## Workflow")
                appendLine()
                appendLine("Copy this checklist and track progress:")
                appendLine()
                appendLine("```")
                appendLine("Task Progress:")
                appendLine("- [ ] Step 1: ...")
                appendLine("- [ ] Step 2: ...")
                appendLine("- [ ] Step 3: ...")
                appendLine("```")
                appendLine()
                appendLine("**Step 1: ...**")
                appendLine("<!-- Describe step details -->")
                appendLine()
                appendLine("**Step 2: ...**")
                appendLine("<!-- Describe step details -->")
                appendLine()
                appendLine("**Step 3: ...**")
                appendLine("<!-- Describe step details -->")
            }

            if (s.addExamples) {
                appendLine()
                appendLine("## Examples")
                appendLine("**Example 1:**")
                appendLine("Input: ...")
                appendLine("Output: ...")
            }

            if (s.addReferences) {
                appendLine()
                appendLine("## References")
                appendLine("- [Details](DETAILS.md)")
                appendLine("- [API Reference](REFERENCE.md)")
            }
        }.trimEnd() + "\n"
    }

    // endregion

    // region File operations

    private fun createAndOpen() {
        val s = state.value
        val skillDir = File(s.savePath, s.name)
        val skillFile = File(skillDir, "SKILL.md")

        if (skillFile.exists()) {
            state.value = s.copy(errorMessage = "File already exists: ${skillFile.path}")
            return
        }

        try {
            skillDir.mkdirs()
            skillFile.writeText(generatePreview())

            VfsUtil.markDirtyAndRefresh(false, true, true, skillDir)
            val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(skillFile)
            if (vf != null) {
                FileEditorManager.getInstance(project).openFile(vf, true)
            }
            close(0)
        } catch (e: Exception) {
            state.value = s.copy(errorMessage = "Failed to create skill: ${e.message}")
        }
    }

    // endregion

    // region UI

    @Composable
    override fun createDesign() {
        val currentState by remember { state }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = TPTheme.colors.black,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Error banner
                currentState.errorMessage?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = TPTheme.colors.red.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(12.dp),
                    ) {
                        TPText(
                            text = error,
                            color = TPTheme.colors.red,
                            style = TextStyle(fontSize = 12.sp),
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                }

                // Skills root path warning
                if (PluginSettingsService.getInstance(project).getSkillsRootPath().isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = WarningYellow.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(12.dp),
                    ) {
                        TPText(
                            text = "Skills root path not configured. Configure in Settings > Tools > Teknasyon Plugin Settings",
                            color = WarningYellow,
                            style = TextStyle(fontSize = 12.sp),
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        // Name field
                        TPText(
                            text = "Skill Name *",
                            color = TPTheme.colors.blue,
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        TPTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = currentState.name,
                            onValueChange = { input ->
                                state.value = state.value.copy(
                                    name = input.lowercase(),
                                    errorMessage = null,
                                )
                            },
                            placeholder = "my-skill-name",
                            isSingleLine = true,
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        ValidationMessages(
                            errors = nameErrors(currentState.name),
                            warnings = nameWarnings(currentState.name),
                            hints = nameHints(currentState.name),
                        )
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        // Save location
                        TPText(
                            text = "Save Location",
                            color = TPTheme.colors.blue,
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TPText(
                                modifier = Modifier.weight(1f),
                                text = currentState.savePath,
                                color = TPTheme.colors.white.copy(alpha = 0.5f),
                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = "Select directory",
                                tint = TPTheme.colors.lightGray,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
                                            .apply { title = "Select Skills Directory" }
                                        FileChooser.chooseFile(descriptor, project, null) { file ->
                                            state.value = state.value.copy(savePath = file.path)
                                        }
                                    }
                                    .padding(2.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(16.dp))

                // Description field
                TPText(
                    text = "Description *",
                    color = TPTheme.colors.blue,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.size(6.dp))
                TPTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = currentState.description,
                    onValueChange = { input ->
                        if (input.length <= 1024) {
                            state.value = state.value.copy(
                                description = input,
                                errorMessage = null,
                            )
                        }
                    },
                    placeholder = "Processes X and generates Y. Use when...",
                    isSingleLine = false,
                )
                Spacer(modifier = Modifier.size(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ValidationMessages(
                            errors = descriptionErrors(currentState.description),
                            warnings = descriptionWarnings(currentState.description),
                            hints = descriptionHints(currentState.description),
                        )
                    }
                    TPText(
                        text = "(${currentState.description.length}/1024)",
                        color = TPTheme.colors.blue,
                        style = TextStyle(fontSize = 11.sp),
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))

                // Optional sections
                TPText(
                    text = "Optional Sections",
                    color = TPTheme.colors.blue,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.size(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TPCheckbox(
                        checked = currentState.addWorkflow,
                        label = "Add workflow",
                        onCheckedChange = { state.value = state.value.copy(addWorkflow = it) },
                    )
                    TPCheckbox(
                        checked = currentState.addExamples,
                        label = "Add examples",
                        onCheckedChange = { state.value = state.value.copy(addExamples = it) },
                    )
                    TPCheckbox(
                        checked = currentState.addReferences,
                        label = "Add references",
                        onCheckedChange = { state.value = state.value.copy(addReferences = it) },
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))

                // Preview
                TPText(
                    text = "Preview",
                    color = TPTheme.colors.blue,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = TPTheme.colors.gray,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(12.dp),
                ) {
                    SelectionContainer {
                        Text(
                            text = generatePreview(),
                            color = TPTheme.colors.lightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.size(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TPActionCard(
                        title = "Cancel",
                        icon = Icons.Rounded.Cancel,
                        actionColor = TPTheme.colors.lightGray,
                        type = TPActionCardType.SMALL,
                        onClick = { close(Constants.DEFAULT_EXIT_CODE) },
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    TPActionCard(
                        title = "Create & Open",
                        icon = Icons.Rounded.CheckCircle,
                        actionColor = TPTheme.colors.blue,
                        type = TPActionCardType.SMALL,
                        isEnabled = isValid(),
                        onClick = { createAndOpen() },
                    )
                }
            }
        }
    }

    @Composable
    private fun ValidationMessages(
        errors: List<String>,
        warnings: List<String>,
        hints: List<String>,
    ) {
        Column {
            errors.forEach { msg ->
                TPText(
                    text = "✗ $msg",
                    color = TPTheme.colors.red,
                    style = TextStyle(fontSize = 11.sp),
                )
            }
            warnings.forEach { msg ->
                TPText(
                    text = "⚠ $msg",
                    color = WarningYellow,
                    style = TextStyle(fontSize = 11.sp),
                )
            }
            hints.forEach { msg ->
                TPText(
                    text = "💡 $msg",
                    color = TPTheme.colors.hintGray,
                    style = TextStyle(fontSize = 11.sp),
                )
            }
        }
    }

    // endregion

    companion object {
        private val WarningYellow = Color(0xFFFFD54F)
    }
}
