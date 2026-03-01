package com.github.cnrture.rune.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.cnrture.rune.theme.RTheme

@Composable
fun RErrorBanner(
    error: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = RTheme.colors.red.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
    ) {
        RText(
            text = error,
            color = RTheme.colors.red,
            style = TextStyle(fontSize = 12.sp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        RActionCard(
            title = "Retry",
            icon = Icons.Rounded.Refresh,
            actionColor = RTheme.colors.red,
            type = RActionCardType.SMALL,
            onClick = onRetry,
        )
    }
}
