package com.github.teknasyon.plugin.service

import com.github.teknasyon.plugin.common.VcsProvider
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import java.awt.Color
import javax.swing.*

class PluginConfigurable(private val project: Project) : BoundConfigurable("Teknasyon Plugin Settings") {

    private val settingsService = PluginSettingsService.getInstance(project)
    private lateinit var skillsPathField: TextFieldWithBrowseButton
    private lateinit var agentsPathField: TextFieldWithBrowseButton
    private lateinit var commitPromptField: JTextArea
    private lateinit var commitMessageJiraUrlField: JCheckBox
    private lateinit var useReviewBranchField: JCheckBox
    private lateinit var jiraEmailField: JTextField
    private lateinit var jiraTokenField: JPasswordField

    // VCS / Bitbucket fields
    private lateinit var vcsProviderCombo: JComboBox<String>
    private lateinit var githubTokenField: JPasswordField
    private lateinit var bitbucketUsernameField: JTextField
    private lateinit var bitbucketTokenField: JPasswordField

    private val vcsProviderOptions = arrayOf("GitHub", "Bitbucket Cloud")

    override fun createPanel(): DialogPanel {
        return panel {
            group("Skills - Agents Directory") {
                twoColumnsRow(
                    column1 = {
                        skillsPathField = textFieldWithBrowseButton(
                            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                                .withTitle("Select Skills Directory"),
                            project
                        ) { it.path }.component
                        skillsPathField.text = settingsService.getSkillsRootPath()
                        rowComment("Directory containing your SKILLS.md files (default: .claude/skills)")
                    },
                    column2 = {
                        agentsPathField = textFieldWithBrowseButton(
                            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                                .withTitle("Select Agents Directory"),
                            project
                        ) { it.path }.component
                        agentsPathField.text = settingsService.getAgentsRootPath()
                        rowComment("Directory containing your agent files (default: .claude/agents)")
                    },
                )
            }

            group("Commit Message") {
                row {
                    commitMessageJiraUrlField = JCheckBox("Include Jira ticket URL in commit message").apply {
                        isSelected = settingsService.isIncludeJiraUrlInCommit()
                    }
                    cell(commitMessageJiraUrlField)
                }
                row {
                    commitPromptField = JTextArea(settingsService.getCommitMessagePrompt(), 4, 60).apply {
                        lineWrap = true
                        wrapStyleWord = true
                    }
                    scrollCell(commitPromptField)
                }
                row {
                    comment(
                        "Prompt sent to Claude when generating commit messages. " +
                            "Use <b>{diff}</b> as a placeholder for the git diff. " +
                            "You can customize the format, add Jira ticket references, or any other instructions."
                    )
                }
                row {
                    link("Reset to Default") {
                        commitPromptField.text = PluginSettingsService.DEFAULT_COMMIT_PROMPT
                    }
                }
            }

            group("Pull Request") {
                row {
                    useReviewBranchField = JCheckBox("Use review/ branch prefix for PRs").apply {
                        isSelected = settingsService.isUseReviewBranch()
                    }
                    cell(useReviewBranchField)
                }
                row {
                    comment(
                        "When enabled, PRs target a <b>review/{branch}</b> created from the base branch. " +
                            "When disabled, PRs target the base branch directly (e.g. develop, main)."
                    )
                }
            }

            group("VCS Provider") {
                row("Provider:") {
                    vcsProviderCombo = JComboBox(DefaultComboBoxModel(vcsProviderOptions)).apply {
                        selectedIndex = getVcsProviderIndex(settingsService.getVcsProvider())
                    }
                    cell(vcsProviderCombo)
                }

                val isGitHub = object : ComponentPredicate() {
                    override fun invoke(): Boolean = vcsProviderCombo.selectedIndex == 0
                    override fun addListener(listener: (Boolean) -> Unit) {
                        vcsProviderCombo.addItemListener { listener(invoke()) }
                    }
                }

                val isBitbucket = object : ComponentPredicate() {
                    override fun invoke(): Boolean = vcsProviderCombo.selectedIndex == 1
                    override fun addListener(listener: (Boolean) -> Unit) {
                        vcsProviderCombo.addItemListener { listener(invoke()) }
                    }
                }

                // GitHub fields
                if (GitHubCredentialService.hasCredentials()) {
                    row {
                        cell(savedCredentialLabel("Token saved"))
                    }.visibleIf(isGitHub)
                }
                row("Personal Access Token:") {
                    githubTokenField = JPasswordField(30)
                    cell(githubTokenField)
                }.visibleIf(isGitHub)
                row {
                    comment(
                        "GitHub Personal Access Token with <b>repo</b> scope. " +
                            "Create at: <b>github.com > Settings > Developer settings > Personal access tokens</b>"
                    )
                }.visibleIf(isGitHub)

                // Bitbucket fields
                if (BitbucketCredentialService.hasCredentials()) {
                    row {
                        cell(savedCredentialLabel("Token saved"))
                    }.visibleIf(isBitbucket)
                }
                row("Email:") {
                    bitbucketUsernameField = JTextField(BitbucketCredentialService.getUsername() ?: "", 30)
                    cell(bitbucketUsernameField)
                }.visibleIf(isBitbucket)
                row {
                    comment("Your Atlassian account email address")
                }.visibleIf(isBitbucket)
                row("API Token:") {
                    bitbucketTokenField = JPasswordField(30)
                    cell(bitbucketTokenField)
                }.visibleIf(isBitbucket)
                row {
                    comment(
                        "Atlassian API Token (same as Jira). " +
                            "Create at: <b>id.atlassian.com > Security > API tokens</b>"
                    )
                }.visibleIf(isBitbucket)
            }

            group("Jira Integration") {
                if (JiraService.hasCredentials()) {
                    row {
                        cell(savedCredentialLabel("Credentials saved (${JiraService.getEmail()})"))
                    }
                }
                row("Email:") {
                    jiraEmailField = JTextField(JiraService.getEmail() ?: "", 30)
                    cell(jiraEmailField)
                }
                row("API Token:") {
                    jiraTokenField = JPasswordField(30)
                    cell(jiraTokenField)
                }
                row { comment("Used to auto-select Fix Version labels from Jira tickets when creating PRs") }
            }
        }
    }

    override fun apply() {
        settingsService.setSkillsRootPath(skillsPathField.text.trim())
        settingsService.setAgentsRootPath(agentsPathField.text.trim())
        settingsService.setCommitMessagePrompt(
            commitPromptField.text.trim().ifBlank { PluginSettingsService.DEFAULT_COMMIT_PROMPT })
        settingsService.setIncludeJiraUrlInCommit(commitMessageJiraUrlField.isSelected)
        settingsService.setUseReviewBranch(useReviewBranchField.isSelected)

        // VCS settings
        settingsService.setVcsProvider(getVcsProviderFromIndex(vcsProviderCombo.selectedIndex))

        // GitHub credentials
        val ghToken = String(githubTokenField.password).trim()
        if (ghToken.isNotBlank()) {
            GitHubCredentialService.saveToken(ghToken)
        }

        // Bitbucket credentials (token yeterli, username opsiyonel)
        val bbUsername = bitbucketUsernameField.text.trim()
        val bbToken = String(bitbucketTokenField.password).trim()
        if (bbToken.isNotBlank()) {
            BitbucketCredentialService.saveCredentials(bbUsername, bbToken)
        }

        // Jira credentials
        val email = jiraEmailField.text.trim()
        val token = String(jiraTokenField.password).trim()
        if (email.isNotBlank() && token.isNotBlank()) {
            JiraService.saveCredentials(email, token)
        }
    }

    override fun isModified(): Boolean {
        val promptModified = commitPromptField.text.trim() != settingsService.getCommitMessagePrompt()
        val jiraUrlToggleModified = commitMessageJiraUrlField.isSelected != settingsService.isIncludeJiraUrlInCommit()
        val reviewBranchModified = useReviewBranchField.isSelected != settingsService.isUseReviewBranch()
        val pathsModified = skillsPathField.text.trim() != settingsService.getSkillsRootPath() ||
            agentsPathField.text.trim() != settingsService.getAgentsRootPath()
        val jiraModified = jiraEmailField.text.trim() != (JiraService.getEmail() ?: "") ||
            String(jiraTokenField.password).trim().isNotBlank()

        val vcsProviderModified =
            getVcsProviderFromIndex(vcsProviderCombo.selectedIndex) != settingsService.getVcsProvider()
        val ghTokenModified = String(githubTokenField.password).trim().isNotBlank()
        val bbCredModified = bitbucketUsernameField.text.trim() != (BitbucketCredentialService.getUsername() ?: "") ||
            String(bitbucketTokenField.password).trim().isNotBlank()

        return promptModified || jiraUrlToggleModified || reviewBranchModified || pathsModified ||
            jiraModified || vcsProviderModified || ghTokenModified || bbCredModified
    }

    override fun reset() {
        skillsPathField.text = settingsService.getSkillsRootPath()
        agentsPathField.text = settingsService.getAgentsRootPath()
        commitPromptField.text = settingsService.getCommitMessagePrompt()
        commitMessageJiraUrlField.isSelected = settingsService.isIncludeJiraUrlInCommit()
        useReviewBranchField.isSelected = settingsService.isUseReviewBranch()
        jiraEmailField.text = JiraService.getEmail() ?: ""
        jiraTokenField.text = ""

        vcsProviderCombo.selectedIndex = getVcsProviderIndex(settingsService.getVcsProvider())
        githubTokenField.text = ""
        bitbucketUsernameField.text = BitbucketCredentialService.getUsername() ?: ""
        bitbucketTokenField.text = ""
    }

    private fun savedCredentialLabel(text: String): JLabel {
        return JLabel(text).apply {
            foreground = Color(75, 181, 67)
        }
    }

    private fun getVcsProviderIndex(provider: VcsProvider?): Int {
        return when (provider) {
            VcsProvider.GITHUB, null -> 0
            VcsProvider.BITBUCKET_CLOUD -> 1
        }
    }

    private fun getVcsProviderFromIndex(index: Int): VcsProvider {
        return when (index) {
            1 -> VcsProvider.BITBUCKET_CLOUD
            else -> VcsProvider.GITHUB
        }
    }
}
