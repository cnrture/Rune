package com.github.teknasyon.getcontactdevtools.toolwindow.manager.featuregenerator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.components.GTCButton
import com.github.teknasyon.getcontactdevtools.components.GTCText
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme

@Composable
fun RootSelectionContent(
    modifier: Modifier = Modifier,
    selectedSrc: String,
    showFileTreeDialog: Boolean,
    onChooseRootClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = GTCTheme.colors.gray,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
    ) {
        GTCText(
            text = "Selected: $selectedSrc",
            color = GTCTheme.colors.blue,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(modifier = Modifier.size(4.dp))
        GTCText(
            text = "Choose the root directory for your new module.",
            color = GTCTheme.colors.lightGray,
        )
        Spacer(modifier = Modifier.size(8.dp))
        GTCButton(
            text = if (showFileTreeDialog) "Close File Tree" else "Open File Tree",
            backgroundColor = GTCTheme.colors.blue,
            onClick = onChooseRootClick,
        )
    }
}