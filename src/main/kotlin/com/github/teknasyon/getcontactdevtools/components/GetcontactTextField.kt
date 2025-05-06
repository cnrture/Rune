package com.github.teknasyon.getcontactdevtools.components

import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme

@Composable
fun GetcontactTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    textStyle: TextStyle = TextStyle.Default,
    isSingleLine: Boolean = true,
) {
    OutlinedTextField(
        modifier = modifier,
        label = { label?.let { Text(it) } },
        placeholder = { placeholder?.let { Text(it) } },
        value = value,
        onValueChange = { onValueChange(it) },
        textStyle = textStyle,
        singleLine = isSingleLine,
        maxLines = if (isSingleLine) 1 else Int.MAX_VALUE,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedLabelColor = GetcontactTheme.colors.white,
            unfocusedLabelColor = GetcontactTheme.colors.white,
            cursorColor = GetcontactTheme.colors.white,
            textColor = GetcontactTheme.colors.white,
            unfocusedBorderColor = GetcontactTheme.colors.white,
            focusedBorderColor = GetcontactTheme.colors.white,
            placeholderColor = GetcontactTheme.colors.white,
        )
    )
}