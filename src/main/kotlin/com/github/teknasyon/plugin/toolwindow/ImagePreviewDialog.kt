package com.github.teknasyon.plugin.toolwindow

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.theme.TPTheme
import java.io.File

@Composable
internal fun ImagePreviewDialog(
    imagePath: String,
    onDismiss: () -> Unit,
) {
    val fileName = imagePath.substringAfterLast("/")
    val imageBitmap = remember(imagePath) { loadImageBitmapFromPath(imagePath) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .background(TPTheme.colors.gray, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TPText(
                    text = fileName,
                    color = TPTheme.colors.white,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = TPTheme.colors.lightGray,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDismiss() },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = fileName,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TPTheme.colors.black),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TPTheme.colors.black),
                    contentAlignment = Alignment.Center,
                ) {
                    TPText(
                        text = "Image could not be loaded",
                        color = TPTheme.colors.hintGray,
                    )
                }
            }
        }
    }
}

internal fun loadImageBitmapFromPath(path: String): ImageBitmap? {
    return try {
        val file = File(path)
        if (!file.exists()) return null
        org.jetbrains.skia.Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}