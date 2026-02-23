package com.github.teknasyon.plugin.toolwindow.manager.ai

import androidx.compose.runtime.Composable
import com.github.teknasyon.plugin.toolwindow.ai.SkillDockPanel
import com.github.teknasyon.plugin.toolwindow.ai.SkillDockViewModel
import com.intellij.openapi.project.Project

@Composable
fun AiContent(
    project: Project,
    viewModel: SkillDockViewModel,
    onShowSettingsClick: () -> Unit,
) {
    SkillDockPanel(
        viewModel = viewModel,
        project = project,
        onShowSettingsClick = onShowSettingsClick,
    )
}