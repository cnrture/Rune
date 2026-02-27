package com.github.teknasyon.plugin.common

import com.intellij.openapi.project.Project
import java.io.File
import com.github.teknasyon.plugin.common.file.File as ProjectFile

fun Project.getCurrentlySelectedFile(selectedSrc: String): File =
    File(rootDirectoryStringDropLast() + File.separator + selectedSrc)

fun Project.rootDirectoryStringDropLast(): String =
    basePath!!.split(File.separator).dropLast(1).joinToString(File.separator)

fun Project.rootDirectoryString(): String = basePath!!

fun File.toProjectFile(): ProjectFile = object : ProjectFile {
    private val numberOfFiles = listFiles()?.size ?: 0
    override val name: String = this@toProjectFile.name
    override val absolutePath: String = this@toProjectFile.absolutePath
    override val isDirectory: Boolean = this@toProjectFile.isDirectory
    override val hasChildren: Boolean = isDirectory && numberOfFiles > 0
    override val children: List<ProjectFile> = this@toProjectFile
        .listFiles { _, name -> !name.startsWith(".") }
        .orEmpty()
        .map { it.toProjectFile() }
}