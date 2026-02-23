package com.github.teknasyon.plugin.toolwindow.manager.modulegenerator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.components.TPRadioButton
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.components.TPTextField
import com.github.teknasyon.plugin.theme.TPTheme

@Composable
fun ModuleTypeNameContent(
    moduleTypeSelectionState: String,
    packageName: String,
    moduleNameState: String,
    radioOptions: List<String>,
    onPackageNameChanged: (String) -> Unit,
    onModuleTypeSelected: (String) -> Unit,
    onModuleNameChanged: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = TPTheme.colors.gray,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
    ) {
        TPText(
            text = "Module Configuration",
            color = TPTheme.colors.white,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        )

        Spacer(modifier = Modifier.size(8.dp))

        TPText(
            text = "Select module type, provide package name and module name",
            color = TPTheme.colors.lightGray,
            style = TextStyle(fontSize = 12.sp)
        )

        Divider(
            color = TPTheme.colors.lightGray,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Column {
                radioOptions.forEach { text ->
                    TPRadioButton(
                        text = text,
                        selected = text == moduleTypeSelectionState,
                        color = TPTheme.colors.blue,
                        onClick = { onModuleTypeSelected(text) },
                    )
                    if (text != radioOptions.last()) {
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
            Column {
                TPTextField(
                    color = TPTheme.colors.blue,
                    placeholder = "Package Name",
                    value = packageName,
                    onValueChange = { onPackageNameChanged(it) },
                )
                Spacer(modifier = Modifier.size(16.dp))
                TPTextField(
                    color = TPTheme.colors.blue,
                    placeholder = ":module",
                    value = moduleNameState,
                    onValueChange = { onModuleNameChanged(it) },
                )
            }
        }
    }
}