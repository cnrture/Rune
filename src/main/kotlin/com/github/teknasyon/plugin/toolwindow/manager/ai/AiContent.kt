package com.github.teknasyon.plugin.toolwindow.manager.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.teknasyon.plugin.data.repository.SkillRepositoryImpl
import com.github.teknasyon.plugin.domain.usecase.ExecuteSkillUseCase
import com.github.teknasyon.plugin.domain.usecase.ProcessReviewCommentsUseCase
import com.github.teknasyon.plugin.domain.usecase.ScanSkillsUseCase
import com.github.teknasyon.plugin.domain.usecase.ToggleFavoriteUseCase
import com.github.teknasyon.plugin.service.FileScanner
import com.github.teknasyon.plugin.service.SkillDockSettingsService
import com.github.teknasyon.plugin.service.TerminalExecutorImpl
import com.github.teknasyon.plugin.toolwindow.ai.SkillDockPanel
import com.github.teknasyon.plugin.toolwindow.ai.SkillDockViewModel
import com.intellij.openapi.project.Project

@Composable
fun AiContent(
    project: Project,
) {
    val viewModel = remember {
        createViewModel(project)
    }

    SkillDockPanel(viewModel = viewModel, project = project)
}

private fun createViewModel(project: Project): SkillDockViewModel {
    val settingsService = SkillDockSettingsService.getInstance(project)
    val fileScanner = FileScanner(project)
    val terminalExecutor = TerminalExecutorImpl()
    val repository = SkillRepositoryImpl(fileScanner, settingsService)
    val scanSkillsUseCase = ScanSkillsUseCase(repository, settingsService)
    val executeSkillUseCase = ExecuteSkillUseCase(terminalExecutor)
    val toggleFavoriteUseCase = ToggleFavoriteUseCase(settingsService)
    val processReviewCommentsUseCase = ProcessReviewCommentsUseCase(project)
    return SkillDockViewModel(
        project = project,
        settingsService = settingsService,
        scanSkillsUseCase = scanSkillsUseCase,
        executeSkillUseCase = executeSkillUseCase,
        toggleFavoriteUseCase = toggleFavoriteUseCase,
        processReviewCommentsUseCase = processReviewCommentsUseCase,
    )
}