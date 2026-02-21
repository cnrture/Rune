package com.github.teknasyon.plugin.toolwindow.manager.featuregenerator.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.teknasyon.plugin.common.file.FileTree
import com.github.teknasyon.plugin.common.rootDirectoryString
import com.github.teknasyon.plugin.common.rootDirectoryStringDropLast
import com.github.teknasyon.plugin.common.toProjectFile
import com.github.teknasyon.plugin.components.TPFileTree
import com.github.teknasyon.plugin.theme.TPTheme
import com.intellij.openapi.project.Project
import java.io.File

@Composable
fun FileTreePanel(
    modifier: Modifier = Modifier,
    project: Project,
    onSelectedSrc: (String) -> Unit = {},
) {
    TPFileTree(
        modifier = modifier,
        model = FileTree(root = File(project.rootDirectoryString()).toProjectFile()),
        titleColor = TPTheme.colors.blue,
        containerColor = TPTheme.colors.black,
        onClick = { fileTreeNode ->
            val absolutePathAtNode = fileTreeNode.file.absolutePath
            val relativePath = absolutePathAtNode.removePrefix(project.rootDirectoryStringDropLast())
                .removePrefix(File.separator)
            if (fileTreeNode.file.isDirectory) {
                onSelectedSrc(relativePath)
            }
        }
    )
}