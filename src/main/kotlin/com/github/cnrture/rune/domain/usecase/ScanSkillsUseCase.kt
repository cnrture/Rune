package com.github.cnrture.rune.domain.usecase

import com.github.cnrture.rune.data.SkillRepository
import com.github.cnrture.rune.domain.model.SkillFolder

class ScanSkillsUseCase(
    private val repository: SkillRepository,
) {
    operator fun invoke(rootPath: String, strictFilter: Boolean): Result<List<SkillFolder>> {
        if (rootPath.isEmpty()) {
            return Result.failure(IllegalStateException("Root path not configured. Please set the root path in settings."))
        }

        return repository.scanSkills(rootPath, strictFilter)
    }
}
