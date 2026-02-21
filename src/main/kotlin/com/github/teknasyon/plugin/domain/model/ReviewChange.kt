package com.github.teknasyon.plugin.domain.model

data class ReviewChange(
    val taskId: Int,
    val taskTitle: String,
    val filePath: String,
    val before: String,
    val after: String,
)
