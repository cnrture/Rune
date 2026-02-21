package com.github.teknasyon.plugin.toolwindow.manager.settings.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.components.TPActionCard
import com.github.teknasyon.plugin.components.TPActionCardType
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.components.TPTextField
import com.github.teknasyon.plugin.data.FileTemplate
import com.github.teknasyon.plugin.theme.TPTheme

@Composable
fun FileTemplateEditor(
    fileTemplate: FileTemplate,
    isModuleEdit: Boolean = false,
    isReview: Boolean = false,
    onUpdate: (FileTemplate) -> Unit = {},
    onDelete: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = TPTheme.colors.gray,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isReview) {
                        TPTextField(
                            placeholder = "ex. Repository.kt",
                            color = TPTheme.colors.white,
                            value = fileTemplate.fileName,
                            onValueChange = { onUpdate(fileTemplate.copy(fileName = it)) }
                        )
                    } else {
                        TPText(
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = TPTheme.colors.white,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            text = fileTemplate.fileName,
                            color = TPTheme.colors.white,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            )
                        )
                    }
                }
                if (isModuleEdit) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TPTextField(
                            placeholder = "ex. domain.repository",
                            color = TPTheme.colors.white,
                            value = fileTemplate.filePath,
                            onValueChange = { onUpdate(fileTemplate.copy(filePath = it)) }
                        )
                    }
                }
            }
            if (isReview) {
                TPText(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = TPTheme.colors.white,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    text = fileTemplate.fileContent,
                    color = TPTheme.colors.white,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            } else {
                TPTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    placeholder = "package {FILE_PACKAGE}\n\ninterface {NAME}Repository {\n    // Define methods here\n}",
                    color = TPTheme.colors.white,
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    value = fileTemplate.fileContent,
                    onValueChange = { onUpdate(fileTemplate.copy(fileContent = it)) },
                    isSingleLine = false,
                )
                TPActionCard(
                    modifier = Modifier.align(Alignment.End),
                    title = "Delete File Template",
                    icon = Icons.Rounded.Delete,
                    type = TPActionCardType.SMALL,
                    actionColor = TPTheme.colors.blue,
                    onClick = onDelete
                )
            }
        }
    }
}