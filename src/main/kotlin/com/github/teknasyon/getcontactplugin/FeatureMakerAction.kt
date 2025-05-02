package com.github.teknasyon.getcontactplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class FeatureMakerAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project: Project = event.project ?: return
        val startingLocation: VirtualFile? = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val shouldUseStartingLocation = startingLocation != null && startingLocation.isDirectory

        FeatureMakerDialogWrapper(
            project = project,
            startingLocation = if (shouldUseStartingLocation) startingLocation else null
        ).show()
    }
}