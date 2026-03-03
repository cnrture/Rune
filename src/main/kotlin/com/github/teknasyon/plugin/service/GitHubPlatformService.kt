package com.github.teknasyon.plugin.service

import com.github.teknasyon.plugin.actions.dialog.CommentThread
import com.github.teknasyon.plugin.actions.dialog.Reply
import com.github.teknasyon.plugin.common.CliUtils
import com.github.teknasyon.plugin.common.VcsProvider
import com.google.gson.JsonParser
import java.io.File

class GitHubPlatformService(
    private val ghPath: String,
    private val dir: File,
    private val owner: String,
    private val repo: String,
) : VcsPlatformService {

    override val providerType = VcsProvider.GITHUB
    override val supportsLabels = true
    override val supportsAssignee = true

    override fun fetchReviewerCandidates(): Result<List<VcsUser>> {
        return try {
            val output = CliUtils.runProcess(
                dir,
                ghPath, "api", "repos/$owner/$repo/contributors",
                "--jq", ".[].login", "--paginate",
            )
            val users = output.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { VcsUser(displayName = it, username = it) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun fetchLabels(): Result<List<String>> {
        return try {
            val output = CliUtils.runProcess(
                dir,
                ghPath, "api", "repos/$owner/$repo/labels",
                "--jq", ".[].name", "--paginate",
            )
            val labels = output.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
            Result.success(labels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun createPullRequest(
        currentBranch: String,
        targetBranch: String,
        title: String,
        body: String,
        reviewers: List<VcsUser>,
        labels: List<String>,
    ): PrCreationResult {
        // Check if a tag with the same name as targetBranch exists on remote.
        // If so, gh pr create (GraphQL) fails with "Base ref must be a branch"
        val hasTagCollision = CliUtils.runGit(dir, "ls-remote", "--tags", "origin", targetBranch).isNotBlank()

        val prUrl = if (hasTagCollision) {
            createPRviaRestApi(currentBranch, targetBranch, body, reviewers, labels)
        } else {
            createPRviaGhCli(currentBranch, targetBranch, body, reviewers, labels)
        }

        if (prUrl != null) {
            return PrCreationResult(url = prUrl)
        }

        // Check if PR already exists
        val existingUrl = findExistingPr(currentBranch)
        if (existingUrl != null) {
            return PrCreationResult(url = existingUrl, errorMessage = "PR already exists")
        }

        return PrCreationResult(
            errorMessage = "PR creation failed. A tag with the same name as '$targetBranch' may exist on remote."
        )
    }

    private fun createPRviaGhCli(
        currentBranch: String,
        targetBranch: String,
        body: String,
        reviewers: List<VcsUser>,
        labels: List<String>,
    ): String? {
        val cmd = mutableListOf(
            ghPath,
            "pr", "create",
            "--assignee", "@me",
            "--base", targetBranch,
            "--head", currentBranch,
            "--title", currentBranch,
            "--body", body,
        )

        if (reviewers.isNotEmpty()) {
            cmd += "--reviewer"
            cmd += reviewers.joinToString(",") { it.username }
        }
        if (labels.isNotEmpty()) {
            cmd += "--label"
            cmd += labels.joinToString(",")
        }

        val prOutput = CliUtils.runProcess(dir, *cmd.toTypedArray())
        return prOutput.lines().firstOrNull { it.startsWith("https://github.com") }
    }

    private fun createPRviaRestApi(
        currentBranch: String,
        targetBranch: String,
        body: String,
        reviewers: List<VcsUser>,
        labels: List<String>,
    ): String? {
        val createCmd = mutableListOf(
            ghPath, "api",
            "repos/$owner/$repo/pulls",
            "--method", "POST",
            "-f", "title=$currentBranch",
            "-f", "head=$currentBranch",
            "-f", "base=$targetBranch",
            "-f", "body=$body",
            "--jq", ".html_url,.number",
        )

        val createOutput = CliUtils.runProcess(dir, *createCmd.toTypedArray()).trim()
        val outputLines = createOutput.lines()
        val prUrl = outputLines.firstOrNull { it.startsWith("https://github.com") } ?: return null
        val prNumber = outputLines.lastOrNull()?.trim() ?: return prUrl

        val editCmd = mutableListOf(ghPath, "pr", "edit", prNumber, "--add-assignee", "@me")
        if (reviewers.isNotEmpty()) {
            editCmd += "--add-reviewer"
            editCmd += reviewers.joinToString(",") { it.username }
        }
        if (labels.isNotEmpty()) {
            editCmd += "--add-label"
            editCmd += labels.joinToString(",")
        }
        CliUtils.runProcess(dir, *editCmd.toTypedArray())

        return prUrl
    }

    override fun createLabel(name: String): Result<Unit> {
        return try {
            CliUtils.runProcess(dir, ghPath, "label", "create", name, "--repo", "$owner/$repo")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun findExistingPr(currentBranch: String): String? {
        val output = CliUtils.runProcess(dir, ghPath, "pr", "view", "--json", "url", "--jq", ".url")
            .trim()
        return output.takeIf { it.startsWith("https://") }
    }

    override fun fetchPrTitle(prIdentifier: PrIdentifier): Result<String> {
        return try {
            val title = CliUtils.runProcess(
                dir,
                ghPath, "pr", "view", prIdentifier.number.toString(),
                "--repo", "${prIdentifier.owner}/${prIdentifier.repo}",
                "--json", "title", "--jq", ".title",
            )
            Result.success(title)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun fetchUnresolvedComments(prIdentifier: PrIdentifier): Result<List<CommentThread>> {
        return try {
            val query = """
                {
                  repository(owner: "${prIdentifier.owner}", name: "${prIdentifier.repo}") {
                    pullRequest(number: ${prIdentifier.number}) {
                      reviewThreads(first: 100) {
                        nodes {
                          isResolved
                          comments(first: 10) {
                            nodes {
                              body
                              path
                              line
                              originalLine
                              diffHunk
                              author { login }
                            }
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val graphqlOutput = CliUtils.runProcess(dir, ghPath, "api", "graphql", "-f", "query=$query")
            val threads = parseGitHubThreads(graphqlOutput)
            Result.success(threads)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGitHubThreads(json: String): List<CommentThread> {
        val root = JsonParser.parseString(json).asJsonObject
        val threads = root
            .getAsJsonObject("data")
            .getAsJsonObject("repository")
            .getAsJsonObject("pullRequest")
            .getAsJsonObject("reviewThreads")
            .getAsJsonArray("nodes")

        val result = mutableListOf<CommentThread>()
        var idCounter = 1L

        for (threadElement in threads) {
            val thread = threadElement.asJsonObject
            if (thread.get("isResolved").asBoolean) continue

            val comments = thread.getAsJsonObject("comments").getAsJsonArray("nodes")
            if (comments.size() == 0) continue

            val first = comments[0].asJsonObject
            val path = first.get("path")?.asString ?: ""
            val line = when {
                first.has("line") && !first.get("line").isJsonNull -> first.get("line").asInt
                first.has("originalLine") && !first.get("originalLine").isJsonNull -> first.get("originalLine").asInt
                else -> null
            }
            val body = first.get("body")?.asString ?: ""
            val reviewer = first.getAsJsonObject("author")?.get("login")?.asString ?: "unknown"
            val diffHunk = first.get("diffHunk")?.asString ?: ""

            val replies = (1 until comments.size()).map { i ->
                val reply = comments[i].asJsonObject
                Reply(
                    user = reply.getAsJsonObject("author")?.get("login")?.asString ?: "unknown",
                    body = reply.get("body")?.asString ?: "",
                )
            }

            result.add(
                CommentThread(
                    id = idCounter++,
                    path = path,
                    line = line,
                    body = body,
                    reviewer = reviewer,
                    diffHunk = diffHunk,
                    replies = replies,
                )
            )
        }

        return result
    }

    override fun parsePrUrl(url: String): PrIdentifier? {
        val match = Regex("""https?://github\.com/([^/]+)/([^/]+)/pull/(\d+)""").find(url) ?: return null
        return PrIdentifier(
            owner = match.groupValues[1],
            repo = match.groupValues[2],
            number = match.groupValues[3].toInt(),
        )
    }

    override fun getPlaceholderUrl(): String = "https://github.com/owner/repo/pull/123"
}
