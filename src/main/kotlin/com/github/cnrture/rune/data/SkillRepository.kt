package com.github.cnrture.rune.data

import com.github.cnrture.rune.domain.model.SkillFolder

interface SkillRepository {
    fun scanSkills(rootPath: String, strictFilter: Boolean): Result<List<SkillFolder>>
    fun getSkillContent(filePath: String): Result<String>
    fun invalidateCache()
}
