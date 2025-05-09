package com.github.teknasyon.getcontactdevtools.action

import com.github.teknasyon.getcontactdevtools.FeatureMakerDialogWrapper
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CreateElementActionBase
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import java.util.function.Consumer

class CreateFeatureAction : CreateElementActionBase(
    "Getcontact Feature",
    "Create a new feature module",
    AllIcons.Nodes.Folder,
) {

    override fun create(name: String, directory: PsiDirectory): Array<PsiElement> {
        return emptyArray()
    }

    override fun invokeDialog(
        project: Project,
        directory: PsiDirectory,
        elementsConsumer: Consumer<in Array<PsiElement>>
    ) {
        val project = directory.project
        val virtualFile = directory.virtualFile
        FeatureMakerDialogWrapper(project, virtualFile).show()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = project != null && virtualFile?.isDirectory == true
    }

    override fun getActionName(directory: PsiDirectory, newName: String) = "Create Getcontact Feature"
    override fun getErrorTitle(): String = "Error Creating Getcontact Feature"
    override fun startInWriteAction(): Boolean = false
}