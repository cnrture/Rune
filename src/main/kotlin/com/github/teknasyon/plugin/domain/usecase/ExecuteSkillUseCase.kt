package com.github.teknasyon.plugin.domain.usecase

import com.github.teknasyon.plugin.domain.model.Skill
import com.github.teknasyon.plugin.service.TerminalExecutor
import com.intellij.openapi.project.Project

class ExecuteSkillUseCase(
    private val terminalExecutor: TerminalExecutor,
) {
    operator fun invoke(project: Project, skill: Skill, input: String) {
        val command = buildCommand(skill, input)
        terminalExecutor.executeCommand(project, command)
    }

    private fun buildCommand(skill: Skill, input: String): String {
        return if (input.isBlank()) {
            skill.commandName
        } else {
            "${skill.commandName} $input"
        }
    }
}
