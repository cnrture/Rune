package com.github.teknasyon.getcontactdevtools.toolwindow.manager.modulegenerator.components

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
import com.github.teknasyon.getcontactdevtools.components.GTCRadioButton
import com.github.teknasyon.getcontactdevtools.components.GTCText
import com.github.teknasyon.getcontactdevtools.components.GTCTextField
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme

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
                color = GTCTheme.colors.gray,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
    ) {
        GTCText(
            text = "Module Configuration",
            color = GTCTheme.colors.white,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        GTCText(
            text = "Select module type, provide package name and module name",
            color = GTCTheme.colors.lightGray,
            style = TextStyle(fontSize = 12.sp)
        )

        Divider(
            color = GTCTheme.colors.lightGray,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Column {
                radioOptions.forEach { text ->
                    GTCRadioButton(
                        text = text,
                        selected = text == moduleTypeSelectionState,
                        color = GTCTheme.colors.blue,
                        onClick = { onModuleTypeSelected(text) },
                    )
                    if (text != radioOptions.last()) {
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
            Column {
                GTCTextField(
                    color = GTCTheme.colors.blue,
                    placeholder = "Package Name",
                    value = packageName,
                    onValueChange = { onPackageNameChanged(it) },
                )
                Spacer(modifier = Modifier.size(16.dp))
                GTCTextField(
                    color = GTCTheme.colors.blue,
                    placeholder = ":module",
                    value = moduleNameState,
                    onValueChange = { onModuleNameChanged(it) },
                )
            }
        }
    }
}