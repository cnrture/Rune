package com.github.teknasyon.getcontactdevtools.toolwindow.manager.jungle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter

@Composable
fun JungleContent() {
    GTCTheme {
        var browserComponent by remember { mutableStateOf<JBCefBrowser?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            try {
                if (JBCefApp.isSupported()) {
                    JBCefBrowser().apply {
                        jbCefClient.cefClient.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
                            override fun onBeforePopup(
                                browser: CefBrowser,
                                frame: CefFrame,
                                targetUrl: String,
                                targetFrameName: String,
                            ): Boolean {
                                browser.loadURL(targetUrl)
                                return true
                            }
                        })

                        loadURL("https://gtc-zoo.test.mobylonia.com/")
                        browserComponent = this
                    }
                } else {
                    errorMessage = "JCEF browser is not supported on this platform"
                }
            } catch (e: Exception) {
                errorMessage = "Failed to initialize browser: ${e.message}"
            }
        }

        when {
            browserComponent != null -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GTCTheme.colors.gray)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                browserComponent?.cefBrowser?.goBack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = GTCTheme.colors.white
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Back",
                            color = GTCTheme.colors.white,
                            fontSize = 16.sp,
                        )
                    }

                    SwingPanel(
                        modifier = Modifier.fillMaxSize(),
                        factory = { browserComponent!!.component }
                    )
                }
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