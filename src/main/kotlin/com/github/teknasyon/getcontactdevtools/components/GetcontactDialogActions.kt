package com.github.teknasyon.getcontactdevtools.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme

@Composable
fun GetcontactDialogActions(
    modifier: Modifier = Modifier,
    positiveText: String = "Create",
    negativeText: String = "Cancel",
    onCancelClick: () -> Unit,
    onCreateClick: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
    ) {
        GetcontactOutlinedButton(
            text = negativeText,
            backgroundColor = GetcontactTheme.colors.blue,
            onClick = onCancelClick,
        )
        GetcontactButton(
            text = positiveText,
            backgroundColor = GetcontactTheme.colors.blue,
            onClick = onCreateClick,
        )
    }
}