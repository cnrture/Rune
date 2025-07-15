package com.github.teknasyon.getcontactdevtools.toolwindow.manager.settings.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.components.GTCText
import com.github.teknasyon.getcontactdevtools.data.FeatureTemplate
import com.github.teknasyon.getcontactdevtools.data.ModuleTemplate
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
import com.github.teknasyon.getcontactdevtools.toolwindow.manager.settings.component.FileTemplateEditor

@Composable
fun FeatureTemplateReviewContent(
    template: FeatureTemplate,
    onCancelClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 80.dp, horizontal = 32.dp)
            .background(
                color = GTCTheme.colors.black,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.RemoveRedEye,
                contentDescription = null,
                tint = GTCTheme.colors.lightGray,
                modifier = Modifier.size(28.dp)
            )
            GTCText(
                text = template.name,
                color = GTCTheme.colors.white,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = GTCTheme.colors.lightGray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onCancelClick() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                GTCText(
                    modifier = Modifier.fillMaxWidth(),
                    text = template.name,
                    color = if (template.isDefault) GTCTheme.colors.lightGray.copy(alpha = 0.5f) else GTCTheme.colors.white,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.height(24.dp))
                template.fileTemplates.forEachIndexed { index, fileTemplate ->
                    FileTemplateEditor(
                        fileTemplate = fileTemplate,
                        isReview = true,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ModuleTemplateReviewContent(
    template: ModuleTemplate,
    onCancelClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 80.dp, horizontal = 32.dp)
            .background(
                color = GTCTheme.colors.black,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.RemoveRedEye,
                contentDescription = null,
                tint = GTCTheme.colors.lightGray,
                modifier = Modifier.size(28.dp)
            )
            GTCText(
                text = template.name,
                color = GTCTheme.colors.white,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = GTCTheme.colors.lightGray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onCancelClick() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                GTCText(
                    modifier = Modifier.fillMaxWidth(),
                    text = template.name,
                    color = if (template.isDefault) GTCTheme.colors.lightGray.copy(alpha = 0.5f) else GTCTheme.colors.white,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.height(24.dp))
                template.fileTemplates.forEachIndexed { index, fileTemplate ->
                    FileTemplateEditor(
                        fileTemplate = fileTemplate,
                        isReview = true,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}