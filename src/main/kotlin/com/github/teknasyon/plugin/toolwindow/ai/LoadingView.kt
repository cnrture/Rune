package com.github.teknasyon.plugin.toolwindow.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.teknasyon.plugin.theme.TPTheme

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = TPTheme.colors.blue
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = "Loading skills...",
                style = MaterialTheme.typography.body2,
                color = TPTheme.colors.lightGray
            )
        }
    }
}
