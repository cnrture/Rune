package com.github.cnrture.rune.data.repository

import com.github.cnrture.rune.domain.model.SkillFolder
import kotlinx.coroutines.flow.Flow

interface SkillRepository {
    fun scanSkills(rootPath: String, strictFilter: Boolean): Result<List<SkillFolder>>
    fun getSkillContent(filePath: String): Result<String>
    fun observeRootPath(): Flow<String>
    fun invalidateCache()
}
