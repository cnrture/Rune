package com.github.teknasyon.plugin.domain.model

data class SkillFolder(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val skills: List<Skill> = emptyList(),
    val subFolders: List<SkillFolder> = emptyList(),
)
