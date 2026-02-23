package com.github.teknasyon.plugin.service

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.panel
import javax.swing.JPasswordField
import javax.swing.JTextField

class SkillDockConfigurable(private val project: Project) :
    BoundConfigurable("SkillDock") {

    private val settingsService = SkillDockSettingsService.getInstance(project)
    private lateinit var skillsPathField: TextFieldWithBrowseButton
    private lateinit var agentsPathField: TextFieldWithBrowseButton
    private lateinit var jiraEmailField: JTextField
    private lateinit var jiraTokenField: JPasswordField

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

            group("Jira Integration") {
                row("Email:") {
                    jiraEmailField = JTextField(JiraService.getEmail() ?: "", 30)
                    cell(jiraEmailField)
                }
                row("API Token:") {
                    jiraTokenField = JPasswordField(30)
                    cell(jiraTokenField)
                }
                row { comment("Used to auto-select Fix Version labels from Jira tickets when creating PRs") }
            }

            group("Favorites") {
                row {
                    val favoritesCount = settingsService.getFavorites().size
                    label("Total favorites: $favoritesCount")
                }
                row {
                    button("Clear All Favorites") {
                        settingsService.clearFavorites()
                    }
                }
            }
        }
    }

    override fun apply() {
        settingsService.setSkillsRootPath(skillsPathField.text.trim())
        settingsService.setAgentsRootPath(agentsPathField.text.trim())

        val email = jiraEmailField.text.trim()
        val token = String(jiraTokenField.password).trim()
        if (email.isNotBlank() && token.isNotBlank()) {
            JiraService.saveCredentials(email, token)
        }
    }

    override fun isModified(): Boolean {
        val pathsModified = skillsPathField.text.trim() != settingsService.getSkillsRootPath() ||
            agentsPathField.text.trim() != settingsService.getAgentsRootPath()
        val jiraModified = jiraEmailField.text.trim() != (JiraService.getEmail() ?: "") ||
            String(jiraTokenField.password).trim().isNotBlank()
        return pathsModified || jiraModified
    }

    override fun reset() {
        skillsPathField.text = settingsService.getSkillsRootPath()
        agentsPathField.text = settingsService.getAgentsRootPath()
        jiraEmailField.text = JiraService.getEmail() ?: ""
        jiraTokenField.text = ""
    }
}
