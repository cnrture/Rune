package com.github.teknasyon.getcontactdevtools.toolwindow.manager.jungle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser

@Composable
fun JungleContent() {
    GTCTheme {
        var browserComponent by remember { mutableStateOf<JBCefBrowser?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            try {
                if (JBCefApp.isSupported()) {
                    val browser = JBCefBrowser()
                    browser.loadURL("https://gtc-zoo.test.mobylonia.com/")
                    browserComponent = browser
                } else {
                    errorMessage = "JCEF browser is not supported on this platform"
                }
            } catch (e: Exception) {
                errorMessage = "Failed to initialize browser: ${e.message}"
            }
        }

        when {
            browserComponent != null -> {
                SwingPanel(
                    modifier = Modifier.fillMaxSize(),
                    factory = { browserComponent!!.component }
                )
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GTCTheme.colors.black),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        modifier = Modifier.padding(32.dp),
                        text = errorMessage!!,
                        fontSize = 20.sp,
                        color = GTCTheme.colors.white,
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GTCTheme.colors.black),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        modifier = Modifier.padding(32.dp),
                        text = "Loading browser...",
                        fontSize = 20.sp,
                        color = GTCTheme.colors.white,
                    )
                }
            }
        }
    }
}