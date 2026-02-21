package com.github.teknasyon.plugin.domain.model

data class Skill(
    val name: String,
    val commandName: String,
    val description: String,
    val filePath: String,
    val relativePath: String,
    val isFavorite: Boolean = false,
) {
    companion object {
        fun deriveCommandName(fileName: String) = "/${fileName.removeSuffix(".md")}"
    }
}
