package com.github.teknasyon.plugin.toolwindow.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.theme.TPTheme

/**
 * Search bar for filtering skills.
 */
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    BasicTextField(
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(
            color = TPTheme.colors.white,
            fontSize = 16.sp,
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Search,
                    tint = TPTheme.colors.lightGray,
                    contentDescription = "Search Icon"
                )
                Spacer(modifier = Modifier.size(12.dp))
                if (!isFocused && query.isEmpty()) {
                    Text(
                        text = "Search...",
                        style = TextStyle(
                            color = TPTheme.colors.hintGray,
                            fontSize = 16.sp,
                        ),
                    )
                }
                innerTextField()
            }
        }
    )
}
