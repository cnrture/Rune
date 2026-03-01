package com.github.cnrture.rune.actions

import com.github.cnrture.rune.toolwindow.claude.ClaudeSessionService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager

class AskClaudeAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText
        if (selectedText.isNullOrBlank()) return

        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val context = if (virtualFile != null) {
            val document = editor.document
            val startLine = document.getLineNumber(selectionModel.selectionStart) + 1
            val endLine = document.getLineNumber(selectionModel.selectionEnd) + 1
            val basePath = project.basePath ?: ""
            val relativePath = virtualFile.path
                .removePrefix(basePath)
                .removePrefix("/")
            "$relativePath:$startLine-$endLine "
        } else {
            "$selectedText\n"
        }

        // Open Claude tool window
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Claude")
        toolWindow?.show()

        val service = ClaudeSessionService.getInstance(project)
        service.ensureSession()
        service.setPendingInput(context)
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabledAndVisible = e.project != null && hasSelection
    }
}
