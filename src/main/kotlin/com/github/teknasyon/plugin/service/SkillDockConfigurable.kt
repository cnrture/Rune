package com.github.teknasyon.plugin.service

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.panel

class SkillDockConfigurable(private val project: Project) :
    BoundConfigurable("SkillDock") {

    private val settingsService = SkillDockSettingsService.getInstance(project)
    private lateinit var skillsPathField: TextFieldWithBrowseButton
    private lateinit var agentsPathField: TextFieldWithBrowseButton

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
    }

    override fun isModified(): Boolean {
        return skillsPathField.text.trim() != settingsService.getSkillsRootPath() ||
            agentsPathField.text.trim() != settingsService.getAgentsRootPath()
    }

    override fun reset() {
        skillsPathField.text = settingsService.getSkillsRootPath()
        agentsPathField.text = settingsService.getAgentsRootPath()
    }
}
