package com.github.teknasyon.getcontactdevtools.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.common.Constants
import com.github.teknasyon.getcontactdevtools.components.GetcontactButton
import com.github.teknasyon.getcontactdevtools.components.GetcontactDialogWrapper
import com.github.teknasyon.getcontactdevtools.components.GetcontactText
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme

class MessageDialogWrapper(private val message: String) : GetcontactDialogWrapper() {

    @Composable
    override fun createDesign() {
        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GetcontactText(
                text = message,
                color = GetcontactTheme.colors.white,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
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