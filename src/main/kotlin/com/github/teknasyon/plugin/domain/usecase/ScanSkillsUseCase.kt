package com.github.teknasyon.plugin.domain.usecase

import com.github.teknasyon.plugin.data.repository.SkillRepository
import com.github.teknasyon.plugin.service.SkillDockSettingsService
import com.github.teknasyon.plugin.domain.model.SkillFolder

class ScanSkillsUseCase(
    private val repository: SkillRepository,
    private val settingsService: SkillDockSettingsService,
) {
    operator fun invoke(rootPath: String, strictFilter: Boolean): Result<List<SkillFolder>> {
        if (rootPath.isEmpty()) {
            return Result.failure(IllegalStateException("Root path not configured. Please set the root path in settings."))
        }

        return repository.scanSkills(rootPath, strictFilter).map { folders ->
            val favorites = settingsService.getFavorites()
            markFavorites(folders, favorites)
        }
    }

    private fun markFavorites(folders: List<SkillFolder>, favorites: Set<String>): List<SkillFolder> {
        return folders.map { folder ->
            folder.copy(
                skills = folder.skills.map { skill ->
                    skill.copy(isFavorite = favorites.contains(skill.filePath))
                },
                subFolders = markFavorites(folder.subFolders, favorites)
            )
        }
    }
}
