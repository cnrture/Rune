package com.github.teknasyon.plugin.toolwindow.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pending
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.domain.model.ReviewChange
import com.github.teknasyon.plugin.domain.model.ReviewTask
import com.github.teknasyon.plugin.domain.model.TaskStatus
import com.github.teknasyon.plugin.theme.TPTheme

@Composable
fun ReviewResultPanel(
    status: ReviewTrackerStatus,
    progressMessage: String,
    tasks: List<ReviewTask>,
    changes: List<ReviewChange>,
    error: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Change Request Tracker", fontWeight = FontWeight.Medium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                }
            },
            elevation = 4.dp,
        )

        if (status != ReviewTrackerStatus.DONE && status != ReviewTrackerStatus.ERROR) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (progressMessage.isNotBlank()) {
                Text(
                    text = progressMessage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.caption,
                    color = TPTheme.colors.white.copy(alpha = 0.7f),
                )
            }
        }

        if (error != null) {
            Text(
                text = error,
                modifier = Modifier.padding(16.dp),
                color = TPTheme.colors.red,
                style = MaterialTheme.typography.body2,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(tasks) { task ->
                TaskCard(
                    task = task,
                    change = changes.find { it.taskId == task.id },
                )
            }
        }
    }
}

@Composable
private fun TaskCard(task: ReviewTask, change: ReviewChange?) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskStatusIcon(task.status)
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        task.filePath,
                        style = MaterialTheme.typography.caption,
                        color = TPTheme.colors.white.copy(alpha = 0.6f),
                    )
                }
            }

            if (change != null) {
                Spacer(modifier = Modifier.height(8.dp))
                DiffView(before = change.before, after = change.after)
            }
        }
    }
}

@Composable
private fun TaskStatusIcon(status: TaskStatus) {
    when (status) {
        TaskStatus.PENDING -> Icon(
            Icons.Default.Pending,
            contentDescription = "Bekliyor",
            tint = TPTheme.colors.white.copy(alpha = 0.4f),
        )

        TaskStatus.IN_PROGRESS -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        TaskStatus.DONE -> Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Tamamlandı",
            tint = Color(0xFF4CAF50),
        )

        TaskStatus.FAILED -> Icon(
            Icons.Default.Error,
            contentDescription = "Hata",
            tint = TPTheme.colors.red,
        )
    }
}

@Composable
private fun DiffView(before: String, after: String) {
    val diffLines = computeDiff(before, after)
    if (diffLines.isEmpty()) return

    Surface(
        shape = MaterialTheme.shapes.small,
        color = TPTheme.colors.white,
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .background(TPTheme.colors.white.copy(alpha = 0.05f))
                .padding(8.dp),
        ) {
            for (line in diffLines) {
                val (prefix, text, bg) = when {
                    line.startsWith("+") -> Triple("+", line.drop(1), Color(0xFF1B5E20).copy(alpha = 0.2f))
                    line.startsWith("-") -> Triple("-", line.drop(1), Color(0xFFB71C1C).copy(alpha = 0.2f))
                    else -> Triple(" ", line, Color.Transparent)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg),
                ) {
                    Text(
                        text = prefix,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = when (prefix) {
                            "+" -> Color(0xFF4CAF50)
                            "-" -> Color(0xFFF44336)
                            else -> TPTheme.colors.white.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.width(16.dp),
                    )
                    Text(
                        text = text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TPTheme.colors.white.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

private fun computeDiff(before: String, after: String, context: Int = 2): List<String> {
    val beforeLines = before.lines()
    val afterLines = after.lines()

    // Simple LCS-based line diff
    val n = beforeLines.size
    val m = afterLines.size
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in n - 1 downTo 0) {
        for (j in m - 1 downTo 0) {
            dp[i][j] = if (beforeLines[i] == afterLines[j]) dp[i + 1][j + 1] + 1
            else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
    }

    // Backtrack to get edit operations: -1=removed, 0=common, 1=added
    data class Edit(val type: Int, val line: String)

    val edits = mutableListOf<Edit>()
    var i = 0
    var j = 0
    while (i < n || j < m) {
        when {
            i < n && j < m && beforeLines[i] == afterLines[j] -> {
                edits.add(Edit(0, beforeLines[i])); i++; j++
            }

            j < m && (i >= n || dp[i][j + 1] >= dp[i + 1][j]) -> {
                edits.add(Edit(1, afterLines[j])); j++
            }

            else -> {
                edits.add(Edit(-1, beforeLines[i])); i++
            }
        }
    }

    // Find changed regions with context
    val changedIndices = edits.indices.filter { edits[it].type != 0 }.toSet()
    if (changedIndices.isEmpty()) return emptyList()

    val visibleIndices = mutableSetOf<Int>()
    for (idx in changedIndices) {
        for (c in maxOf(0, idx - context)..minOf(edits.size - 1, idx + context)) {
            visibleIndices.add(c)
        }
    }

    val result = mutableListOf<String>()
    var lastIdx = -1
    for (idx in visibleIndices.sorted()) {
        if (lastIdx >= 0 && idx > lastIdx + 1) result.add("...")
        val edit = edits[idx]
        result.add(
            when (edit.type) {
                1 -> "+${edit.line}"
                -1 -> "-${edit.line}"
                else -> " ${edit.line}"
            }
        )
        lastIdx = idx
    }
    return result
}
