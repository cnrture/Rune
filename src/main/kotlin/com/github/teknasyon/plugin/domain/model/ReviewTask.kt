package com.github.teknasyon.plugin.domain.model

enum class TaskStatus { PENDING, IN_PROGRESS, DONE, FAILED }

data class ReviewTask(
    val id: Int,
    val title: String,
    val filePath: String,
    val description: String,
    val status: TaskStatus = TaskStatus.PENDING,
)
