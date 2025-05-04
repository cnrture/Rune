package com.github.teknasyon.getcontactdevtools

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.common.Constants
import com.github.teknasyon.getcontactdevtools.components.GetcontactButton
import com.github.teknasyon.getcontactdevtools.components.GetcontactDialogWrapper
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme

class MessageDialogWrapper(private val message: String) : GetcontactDialogWrapper(Constants.EMPTY) {

    @Composable
    override fun createDesign() {
        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = message,
                color = GetcontactTheme.colors.white,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.size(24.dp))
            GetcontactButton(
                text = "Okay",
                onClick = { close(Constants.DEFAULT_EXIT_CODE) },
                backgroundColor = GetcontactTheme.colors.blue,
            )
        }
    }
}