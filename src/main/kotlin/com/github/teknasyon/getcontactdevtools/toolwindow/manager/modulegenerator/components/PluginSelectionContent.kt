package com.github.teknasyon.getcontactdevtools.toolwindow.manager.modulegenerator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.components.GTCCheckbox
import com.github.teknasyon.getcontactdevtools.components.GTCText
import com.github.teknasyon.getcontactdevtools.data.PluginListItem
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PluginSelectionContent(
    availablePlugins: List<PluginListItem>,
    onPluginSelected: (PluginListItem) -> Unit,
) {
    if (availablePlugins.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = GTCTheme.colors.gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
        ) {
            GTCText(
                text = "Plugins",
                color = GTCTheme.colors.white,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.size(4.dp))
            GTCText(
                text = "Select plugins that your new module will use:",
                color = GTCTheme.colors.lightGray,
                style = TextStyle(fontSize = 13.sp),
            )
            Divider(
                color = GTCTheme.colors.lightGray,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availablePlugins.forEach { plugin ->
                        GTCCheckbox(
                            checked = plugin.isSelected,
                            label = plugin.name,
                            isBackgroundEnable = true,
                            color = GTCTheme.colors.blue,
                            onCheckedChange = { onPluginSelected(plugin) },
                        )
                    }
                }
            }
        }
    }
}