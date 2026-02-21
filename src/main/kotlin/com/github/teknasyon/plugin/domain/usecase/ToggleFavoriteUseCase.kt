package com.github.teknasyon.plugin.domain.usecase

import com.github.teknasyon.plugin.service.SkillDockSettingsService

class ToggleFavoriteUseCase(
    private val settingsService: SkillDockSettingsService,
) {
    operator fun invoke(skillPath: String) {
        if (settingsService.isFavorite(skillPath)) {
            settingsService.removeFavorite(skillPath)
        } else {
            settingsService.addFavorite(skillPath)
        }
    }
}
