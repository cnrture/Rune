package com.github.cnrture.rune.actions

import com.github.cnrture.rune.common.Constants
import com.github.cnrture.rune.toolwindow.ClaudeSessionService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.wm.ToolWindowManager

class CheckSkillBestPracticesAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return
        val content = document.text

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Claude")
        toolWindow?.show()

        val service = ClaudeSessionService.getInstance(project)
        val hadSession = service.state.value.sessions.isNotEmpty()
        service.ensureSession()

        val prompt = buildPrompt(content)

        if (hadSession) {
            service.sendToTerminal(prompt, autoRun = true)
        } else {
            ApplicationManager.getApplication().executeOnPooledThread {
                Thread.sleep(Constants.DELAY_NEW_SESSION_CLI_MS)
                ApplicationManager.getApplication().invokeLater {
                    service.sendToTerminal(prompt, autoRun = true)
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = e.project != null && file?.name?.endsWith("SKILL.md") == true
    }

    private fun buildPrompt(skillContent: String): String {
        return """Review this SKILL.md file against Claude platform best practices (${Constants.CLAUDE_SKILL_BEST_PRACTICES_URL}):

<skill_content>
$skillContent
</skill_content>

Check for these categories:

1. **Frontmatter**: name (lowercase, hyphens, max 64 chars, no reserved words "anthropic"/"claude") and description (max 1024 chars, no XML tags, non-empty)
2. **Naming**: prefer gerund form (e.g. processing-pdfs), avoid vague names (helper, utils, tools, data, files)
3. **Description quality**: third person (no "I"/"You"/"We"), specific, includes "Use when..." trigger context
4. **Body size**: SKILL.md under 500 lines, concise (only context Claude doesn't already know)
5. **Structure**: progressive disclosure, file references one level deep from SKILL.md, table of contents for 100+ line files
6. **Workflows**: checklist pattern for complex tasks, feedback loops for quality-critical tasks
7. **Content**: no time-sensitive info (specific dates/years), consistent terminology, no Windows-style paths (backslashes)
8. **Code** (if present): explicit error handling, no magic numbers, dependencies listed, scripts solve problems rather than punt to Claude

For each issue found, specify the section, severity (Error/Warning/Hint), and a suggested fix. At the end, provide an overall score and a summary of improvements."""
    }
}
