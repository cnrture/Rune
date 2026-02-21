package com.github.teknasyon.plugin.toolwindow.ai

import com.github.teknasyon.plugin.domain.model.ReviewChange
import com.github.teknasyon.plugin.domain.model.ReviewTask

enum class ReviewTrackerStatus { IDLE, FETCHING, ANALYZING, PROCESSING, DONE, ERROR }

data class ReviewTrackerState(
    val isDialogVisible: Boolean = false,
    val status: ReviewTrackerStatus = ReviewTrackerStatus.IDLE,
    val progressMessage: String = "",
    val tasks: List<ReviewTask> = emptyList(),
    val changes: List<ReviewChange> = emptyList(),
    val error: String? = null,
)
