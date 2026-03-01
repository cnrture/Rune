package com.github.cnrture.rune.actions

import com.github.cnrture.rune.toolwindow.claude.ClaudeSessionService
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import java.util.function.Function
import javax.swing.JComponent

class SkillBestPracticesNotificationProvider : EditorNotificationProvider, DumbAware {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?> {
        if (!file.name.endsWith("SKILL.md")) {
            return Function { null }
        }

        return Function { fileEditor ->
            val panel = EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info)
            panel.text = "Check this skill against Claude platform best practices"
            panel.createActionLabel("Open best practices") {
                val url = "https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices.md"
                BrowserUtil.browse(url)
            }
            panel.createActionLabel("Check with Claude") {
                val document = fileEditor.file?.let {
                    FileDocumentManager.getInstance().getDocument(it)
                }
                val content = document?.text ?: return@createActionLabel

                val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Claude")
                toolWindow?.show()

                val service = ClaudeSessionService.getInstance(project)
                val hadSession = service.state.value.sessions.isNotEmpty()
                service.ensureSession()

                val prompt = buildPrompt(content)

                if (hadSession) {
                    service.sendToTerminal(prompt, autoRun = true)
                } else {
                    // New session needs time for claude CLI to start (~2s)
                    ApplicationManager.getApplication().executeOnPooledThread {
                        Thread.sleep(3000)
                        ApplicationManager.getApplication().invokeLater {
                            service.sendToTerminal(prompt, autoRun = true)
                        }
                    }
                }
            }
            panel
        }
    }

    private fun buildPrompt(skillContent: String): String {
        return """Review this SKILL.md file against Claude platform best practices (https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices.md):

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
