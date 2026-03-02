package com.github.teknasyon.plugin.data

import com.github.teknasyon.plugin.domain.model.SkillFolder

interface SkillRepository {
    fun scanSkills(rootPath: String, strictFilter: Boolean): Result<List<SkillFolder>>
    fun getSkillContent(filePath: String): Result<String>
    fun invalidateCache()
}
