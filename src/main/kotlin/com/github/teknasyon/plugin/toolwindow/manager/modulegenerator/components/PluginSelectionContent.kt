package com.github.teknasyon.plugin.toolwindow.manager.modulegenerator.components

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
import com.github.teknasyon.plugin.components.TPCheckbox
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.data.PluginListItem
import com.github.teknasyon.plugin.theme.TPTheme

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
                    color = TPTheme.colors.gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
        ) {
            TPText(
                text = "Plugins",
                color = TPTheme.colors.white,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.size(4.dp))
            TPText(
                text = "Select plugins that your new module will use:",
                color = TPTheme.colors.lightGray,
                style = TextStyle(fontSize = 13.sp),
            )
            Divider(
                color = TPTheme.colors.lightGray,
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
                        TPCheckbox(
                            checked = plugin.isSelected,
                            label = plugin.name,
                            isBackgroundEnable = true,
                            color = TPTheme.colors.blue,
                            onCheckedChange = { onPluginSelected(plugin) },
                        )
                    }
                }
            }
        }
    }
}