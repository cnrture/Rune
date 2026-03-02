package com.github.teknasyon.plugin.data

import com.github.teknasyon.plugin.domain.model.SkillFolder
import kotlinx.coroutines.flow.Flow

interface SkillRepository {
    fun scanSkills(rootPath: String, strictFilter: Boolean): Result<List<SkillFolder>>
    fun getSkillContent(filePath: String): Result<String>
    fun observeRootPath(): Flow<String>
    fun invalidateCache()
}
