package com.github.teknasyon.getcontactdevtools.toolwindow.manager.modulegenerator.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.teknasyon.getcontactdevtools.common.file.FileTree
import com.github.teknasyon.getcontactdevtools.common.rootDirectoryString
import com.github.teknasyon.getcontactdevtools.common.rootDirectoryStringDropLast
import com.github.teknasyon.getcontactdevtools.common.toProjectFile
import com.github.teknasyon.getcontactdevtools.components.GTCFileTree
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
import com.intellij.openapi.project.Project
import java.io.File

@Composable
fun FileTreePanel(
    modifier: Modifier = Modifier,
    project: Project,
    onSelectedSrc: (String) -> Unit = {},
) {
    GTCFileTree(
        modifier = modifier,
        model = FileTree(root = File(project.rootDirectoryString()).toProjectFile()),
        titleColor = GTCTheme.colors.blue,
        containerColor = GTCTheme.colors.black,
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