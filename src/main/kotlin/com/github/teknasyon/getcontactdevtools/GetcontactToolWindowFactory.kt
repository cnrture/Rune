package com.github.teknasyon.getcontactdevtools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.theme.GetcontactTheme
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
                GetcontactTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GetcontactTheme.colors.black),
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            text = "Getcontact DevTools",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        GetcontactTheme.colors.blue,
                                        GetcontactTheme.colors.purple,
                                    ),
                                    tileMode = TileMode.Mirror,
                                ),
                            )
                        )
                        JungleTabs(
                            selectedTabIndex = selectedTabIndex,
                            onTabSelected = { index ->
                                selectedTabIndex = index
                                browser?.loadURL(links[index])
                            }
                        )
                    }
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
                    GetcontactTheme {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(GetcontactTheme.colors.black),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                modifier = Modifier.padding(32.dp),
                                text = "Browser is not available",
                                fontSize = 20.sp,
                                color = GetcontactTheme.colors.white,
                            )
                        }
                    }
                }
                panel.add(this, BorderLayout.CENTER)
            }
        }

        ComposePanel().apply {
            setContent {
                GetcontactTheme {
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
                .background(GetcontactTheme.colors.black)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionCard(
                title = "Module Creator",
                icon = Icons.Filled.ViewModule,
                iconBg = GetcontactTheme.colors.orange,
                onClick = { ModuleMakerDialogWrapper(project, null).apply { showAndGet() } },
            )

            ActionCard(
                title = "Feature Creator",
                icon = Icons.Filled.Add,
                iconBg = GetcontactTheme.colors.blue,
                onClick = { FeatureMakerDialogWrapper(project, null).apply { showAndGet() } },
            )
        }
    }

    @Composable
    private fun JungleTabs(
        selectedTabIndex: Int,
        onTabSelected: (Int) -> Unit,
    ) {
        val tabs = listOf("JSON Viewer", "RAG Assistant")
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = GetcontactTheme.colors.gray,
            contentColor = GetcontactTheme.colors.white,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GetcontactTheme.colors.white,
                        )
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun RowScope.ActionCard(
        title: String,
        icon: ImageVector,
        iconBg: Color,
        onClick: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = iconBg,
                )
                .background(
                    color = GetcontactTheme.colors.gray,
                    shape = RoundedCornerShape(12.dp),
                )
                .border(
                    width = 3.dp,
                    color = iconBg.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GetcontactTheme.colors.white,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = GetcontactTheme.colors.white,
            )
        }
    }
}