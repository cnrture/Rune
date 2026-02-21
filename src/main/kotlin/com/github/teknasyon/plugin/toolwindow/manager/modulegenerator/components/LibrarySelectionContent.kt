package com.github.teknasyon.plugin.toolwindow.manager.modulegenerator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.components.TPCheckbox
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.theme.TPTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibrarySelectionContent(
    availableLibraries: List<String>,
    selectedLibraries: List<String>,
    onLibrarySelected: (String) -> Unit,
    libraryGroups: Map<String, List<String>>,
    expandedGroups: Map<String, Boolean>,
    onGroupExpandToggle: (String) -> Unit,
) {
    if (availableLibraries.isNotEmpty()) {
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
                text = "Library Dependencies",
                color = TPTheme.colors.white,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.size(4.dp))
            TPText(
                text = "Select libraries that your new module will depend on:",
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
                libraryGroups.forEach { (groupName, groupLibraries) ->
                    val isExpanded = expandedGroups[groupName] ?: false
                    val allGroupSelected = groupLibraries.all { it in selectedLibraries }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = TPTheme.colors.blue,
                                shape = RoundedCornerShape(8.dp)
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onGroupExpandToggle(groupName) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TPText(
                                    text = groupName.replaceFirstChar { it.uppercase() },
                                    color = TPTheme.colors.white,
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = TPTheme.colors.white,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(if (isExpanded) 180f else 0f)
                                )
                            }

                            TPCheckbox(
                                checked = allGroupSelected,
                                color = TPTheme.colors.blue,
                                onCheckedChange = {
                                    if (allGroupSelected) {
                                        groupLibraries.forEach { library ->
                                            if (library in selectedLibraries) onLibrarySelected(library)
                                        }
                                    } else {
                                        groupLibraries.forEach { library ->
                                            if (library !in selectedLibraries) onLibrarySelected(library)
                                        }
                                    }
                                },
                            )
                        }
                        if (isExpanded) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                groupLibraries.forEach { library ->
                                    val isChecked = library in selectedLibraries
                                    TPCheckbox(
                                        checked = isChecked,
                                        label = library,
                                        isBackgroundEnable = true,
                                        color = TPTheme.colors.blue,
                                        onCheckedChange = { onLibrarySelected(library) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
