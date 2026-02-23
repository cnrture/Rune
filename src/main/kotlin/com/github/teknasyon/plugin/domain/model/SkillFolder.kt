package com.github.teknasyon.plugin.domain.model

data class SkillFolder(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val skills: List<Skill> = emptyList(),
    val subFolders: List<SkillFolder> = emptyList(),
) {
    fun getAllSkills(): List<Skill> {
        val allSkills = mutableListOf<Skill>()
        allSkills.addAll(skills)
        subFolders.forEach { folder ->
            allSkills.addAll(folder.getAllSkills())
        }
        return allSkills
    }

    fun hasSkills(): Boolean = skills.isNotEmpty() || subFolders.any { it.hasSkills() }
}
