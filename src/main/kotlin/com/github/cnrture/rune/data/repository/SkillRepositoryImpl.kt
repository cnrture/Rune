package com.github.cnrture.rune.data.repository

import com.github.cnrture.rune.domain.model.SkillFolder
import com.github.cnrture.rune.service.FileScanner
import com.github.cnrture.rune.settings.PluginSettingsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SkillRepositoryImpl(
    private val fileScanner: FileScanner,
    private val settingsService: PluginSettingsService,
) : SkillRepository {

    override fun scanSkills(
        rootPath: String,
        strictFilter: Boolean,
    ): Result<List<SkillFolder>> {
        return try {
            if (rootPath.isBlank()) {
                return Result.failure(IllegalArgumentException("Root path cannot be empty"))
            }
            val folders = fileScanner.scanDirectory(rootPath, strictFilter)
            Result.success(folders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSkillContent(filePath: String): Result<String> {
        return try {
            val content = fileScanner.readFileContent(filePath)
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeRootPath(): Flow<String> = flow {
        emit(settingsService.getSkillsRootPath())
    }

    override fun invalidateCache() {
        fileScanner.invalidateCache()
    }
}
