package com.github.teknasyon.plugin.toolwindow.ai

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.teknasyon.plugin.domain.model.Skill

@Composable
fun SkillList(
    skills: List<Skill>,
    showRunButton: Boolean,
    onExecuteSkill: (Skill, String) -> Unit,
    onToggleFavorite: (Skill) -> Unit,
    onOpenFile: (Skill) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(skills, key = { it.filePath }) { skill ->
            SkillItemView(
                skill = skill,
                showRunButton = showRunButton,
                onExecute = { input -> onExecuteSkill(skill, input) },
                onToggleFavorite = { onToggleFavorite(skill) },
                onOpenFile = { onOpenFile(skill) }
            )
        }
    }
}
