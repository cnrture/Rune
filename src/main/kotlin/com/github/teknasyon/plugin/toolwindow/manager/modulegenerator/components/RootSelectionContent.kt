package com.github.teknasyon.plugin.toolwindow.manager.modulegenerator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.components.TPButton
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.theme.TPTheme

@Composable
fun RootSelectionContent(
    selectedSrc: String,
    showFileTreeDialog: Boolean,
    isFileTreeButtonEnabled: Boolean = true,
    onChooseRootClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = TPTheme.colors.gray,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            TPText(
                text = "Selected: $selectedSrc",
                color = TPTheme.colors.blue,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
            )
            if (isFileTreeButtonEnabled) {
                Spacer(modifier = Modifier.size(4.dp))
                TPText(
                    text = "Choose the root directory for your new module.",
                    color = TPTheme.colors.lightGray,
                    style = TextStyle(fontSize = 12.sp)
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isFileTreeButtonEnabled) {
                    TPButton(
                        text = if (showFileTreeDialog) "Close File Tree" else "Open File Tree",
                        backgroundColor = TPTheme.colors.blue,
                        onClick = onChooseRootClick,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        }
    }
}