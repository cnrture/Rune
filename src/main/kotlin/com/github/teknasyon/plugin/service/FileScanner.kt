package com.github.teknasyon.plugin.service

import com.github.teknasyon.plugin.domain.model.Skill
import com.github.teknasyon.plugin.domain.model.SkillFolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class FileScanner(private val project: Project) {

    private data class CacheEntry(
        val path: String,
        val strictFilter: Boolean,
        val timestamp: Long,
        val result: List<SkillFolder>,
    )

    @Volatile
    private var cache: CacheEntry? = null
    private val cacheExpirationMs = 5 * 60 * 1000L // 5 minutes

    fun scanDirectory(rootPath: String, strictFilter: Boolean): List<SkillFolder> {
        val absolutePath = resolveAbsolutePath(rootPath)

        val now = System.currentTimeMillis()
        cache?.let { entry ->
            if (entry.path == absolutePath && entry.strictFilter == strictFilter && now - entry.timestamp < cacheExpirationMs) {
                return entry.result
            }
        }

        val result = performScan(absolutePath, strictFilter)
        cache = CacheEntry(absolutePath, strictFilter, now, result)
        return result
    }

    private fun resolveAbsolutePath(path: String): String {
        if (path.startsWith("/")) return path
        val basePath = project.basePath ?: return path
        val resolved = "$basePath/$path".trimEnd('/')
        return resolved
    }

    fun readFileContent(filePath: String): String {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $filePath")
        }
        return file.readText()
    }

    fun invalidateCache() {
        cache = null
    }

    private fun performScan(
        rootPath: String,
        strictFilter: Boolean,
    ): List<SkillFolder> {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(rootPath) ?: return emptyList()
        if (!virtualFile.isDirectory) return emptyList()
        return buildSkillTree(virtualFile, rootPath, strictFilter)
    }

    private fun buildSkillTree(
        virtualFile: VirtualFile,
        rootPath: String,
        strictFilter: Boolean,
    ): List<SkillFolder> {
        val result = mutableListOf<SkillFolder>()

        virtualFile.children?.forEach { child ->
            when {
                child.isDirectory -> {
                    val subFolders = buildSkillTree(child, rootPath, strictFilter)
                    if (subFolders.isNotEmpty()) {
                        result.add(
                            SkillFolder(
                                name = child.name,
                                path = child.path,
                                isDirectory = true,
                                skills = emptyList(),
                                subFolders = subFolders
                            )
                        )
                    }
                }

                (strictFilter && child.name == "SKILL.md") ||
                    (!strictFilter && child.extension?.lowercase() == "md") -> {
                    val skill = createSkillFromFile(child, rootPath)
                    result.add(
                        SkillFolder(
                            name = child.name,
                            path = child.path,
                            isDirectory = false,
                            skills = listOf(skill),
                            subFolders = emptyList()
                        )
                    )
                }
            }
        }

        return result
    }

    private fun createSkillFromFile(virtualFile: VirtualFile, rootPath: String): Skill {
        val content = try {
            String(virtualFile.contentsToByteArray())
        } catch (_: Exception) {
            ""
        }

        val description = parseDescription(content)
        val relativePath = virtualFile.path.removePrefix(rootPath).removePrefix("/")
            .removeSuffix("/${virtualFile.name}").ifBlank { virtualFile.name }

        return Skill(
            name = virtualFile.name,
            commandName = Skill.deriveCommandName(virtualFile.name),
            description = description,
            filePath = virtualFile.path,
            relativePath = relativePath,
            isFavorite = false // Will be updated by use case layer
        )
    }

    private fun parseDescription(content: String): String {
        if (content.isBlank()) return ""

        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return ""

        val headingLine = lines.firstOrNull { it.startsWith("description:") || it.startsWith("#") }
        if (headingLine != null) {
            return headingLine.removePrefix("description:").removePrefix("#").trim()
        }

        val paragraphLine = lines
            .dropWhile { it.startsWith("---") || it.startsWith("```") }
            .firstOrNull()

        return paragraphLine?.take(100) ?: ""
    }
}
