package com.github.cnrture.rune.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.panel
import javax.swing.JTextArea

class PluginConfigurable(private val project: Project) :
    BoundConfigurable("Rune Settings") {

    private val settingsService = PluginSettingsService.getInstance(project)
    private lateinit var skillsPathField: TextFieldWithBrowseButton
    private lateinit var agentsPathField: TextFieldWithBrowseButton
    private lateinit var commitPromptField: JTextArea

    override fun createPanel(): DialogPanel {
        return panel {
            group("Skills Directory") {
                row("Root Path:") {
                    skillsPathField = textFieldWithBrowseButton(
                        FileChooserDescriptorFactory.createSingleFolderDescriptor()
                            .withTitle("Select Skills Directory"),
                        project
                    ) { it.path }.component
                    skillsPathField.text = settingsService.getSkillsRootPath()
                }
                row { comment("Directory containing your SKILLS.md files") }
            }

            group("Agents Directory") {
                row("Root Path:") {
                    agentsPathField = textFieldWithBrowseButton(
                        FileChooserDescriptorFactory.createSingleFolderDescriptor()
                            .withTitle("Select Agents Directory"),
                        project
                    ) { it.path }.component
                    agentsPathField.text = settingsService.getAgentsRootPath()
                }
                row { comment("Directory containing your agent SKILLS.md files") }
            }

            group("Commit Message") {
                row {
                    commitPromptField = JTextArea(settingsService.getCommitMessagePrompt(), 4, 60).apply {
                        lineWrap = true
                        wrapStyleWord = true
                    }
                    scrollCell(commitPromptField)
                }
                row {
                    comment(
                        "Prompt sent to Claude when generating commit messages. " +
                            "Use <b>{diff}</b> as a placeholder for the git diff. " +
                            "You can customize the format, add ticket references, or any other instructions."
                    )
                }
                row {
                    link("Reset to Default") {
                        commitPromptField.text = PluginSettingsService.DEFAULT_COMMIT_PROMPT
                    }
                }
            }
        }
    }

    override fun apply() {
        settingsService.setSkillsRootPath(skillsPathField.text.trim())
        settingsService.setAgentsRootPath(agentsPathField.text.trim())
        settingsService.setCommitMessagePrompt(commitPromptField.text.trim().ifBlank { PluginSettingsService.DEFAULT_COMMIT_PROMPT })
    }

    override fun isModified(): Boolean {
        val promptModified = commitPromptField.text.trim() != settingsService.getCommitMessagePrompt()
        val pathsModified = skillsPathField.text.trim() != settingsService.getSkillsRootPath() ||
            agentsPathField.text.trim() != settingsService.getAgentsRootPath()
        return promptModified || pathsModified
    }

    override fun reset() {
        skillsPathField.text = settingsService.getSkillsRootPath()
        agentsPathField.text = settingsService.getAgentsRootPath()
        commitPromptField.text = settingsService.getCommitMessagePrompt()
    }
}
