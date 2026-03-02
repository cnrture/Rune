package com.github.teknasyon.plugin.data

import com.github.teknasyon.plugin.domain.model.SkillFolder
import com.github.teknasyon.plugin.service.FileScanner

class SkillRepositoryImpl(
    private val fileScanner: FileScanner,
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

    override fun invalidateCache() {
        fileScanner.invalidateCache()
    }
}
