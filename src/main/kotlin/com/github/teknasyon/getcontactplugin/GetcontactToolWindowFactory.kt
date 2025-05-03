package com.github.teknasyon.getcontactplugin

import androidx.compose.foundation.layout.*
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactplugin.components.GetcontactButton
import com.github.teknasyon.getcontactplugin.theme.WidgetTheme
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefBrowser
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class GetcontactToolWindowFactory : ToolWindowFactory {

    private var selectedTabIndex by mutableStateOf(0)
    private val links = listOf(
        "https://gtc-zoo.test.mobylonia.com/tools/json-viewer",
        "https://gtc-rag.test.mobylonia.com/"
    )
    private var browser: JBCefBrowser? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(
            createToolWindowComponent(project),
            "",
            false
        )
        toolWindow.contentManager.addContent(content)
    }

    private fun createToolWindowComponent(project: Project): JComponent {
        val panel = JPanel(BorderLayout())

        ComposePanel().apply {
            setContent {
                WidgetTheme {
                    JungleTabs(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = { index ->
                            selectedTabIndex = index
                            browser?.loadURL(links[index])
                        }
                    )
                }
            }

            panel.add(this, BorderLayout.NORTH)
        }

        try {
            browser = JBCefBrowser().apply {
                loadURL(links[selectedTabIndex])
                panel.add(this.component, BorderLayout.CENTER)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ComposePanel().apply {
                setContent {
                    WidgetTheme {
                        Text(
                            modifier = Modifier.padding(32.dp),
                            text = "Browser is not available",
                            fontSize = 20.sp,
                        )
                    }
                }
                panel.add(this, BorderLayout.CENTER)
            }
        }

        ComposePanel().apply {
            setContent {
                WidgetTheme {
                    ButtonsRow(project)
                }
            }
            panel.add(this, BorderLayout.SOUTH)
        }

        return panel
    }

    @Composable
    private fun ButtonsRow(project: Project) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GetcontactButton(
                modifier = Modifier.weight(1f),
                text = "Create Module",
                onClick = {
                    ModuleMakerDialogWrapper(project, null).apply { showAndGet() }
                }
            )

            GetcontactButton(
                modifier = Modifier.weight(1f),
                text = "Create Feature",
                onClick = {
                    FeatureMakerDialogWrapper(project, null).apply { showAndGet() }
                }
            )
        }
    }

    @Composable
    private fun JungleTabs(
        selectedTabIndex: Int,
        onTabSelected: (Int) -> Unit,
    ) {
        val tabs = listOf("Json Viewer", "RAG Assistant")
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title) }
                )
            }
        }
    }
}