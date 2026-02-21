package com.github.teknasyon.plugin.startup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.components.TPDialogWrapper
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.theme.TPTheme
import com.github.teknasyon.plugin.toolwindow.manager.newsletter.NewsItem
import com.github.teknasyon.plugin.toolwindow.manager.newsletter.NewsService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.awt.Desktop
import java.net.URI
import javax.swing.SwingUtilities

class NewsletterStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        // Startup'ta çalışacak kod
        try {
            val newsService = NewsService()
            val latestNews = newsService.fetchNews().firstOrNull()

            if (latestNews != null) {
                SwingUtilities.invokeLater {
                    showNewsletterPopup(latestNews)
                }
            }
        } catch (e: Exception) {
            println("Error loading newsletter for startup: ${e.message}")
        }
    }

    private fun showNewsletterPopup(newsItem: NewsItem) {
        NewsDialog(
            newsItem = newsItem,
            onOpenSlack = { slackUrl ->
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(slackUrl))
                }
            }
        ).show()
    }
}

class NewsDialog(
    private val newsItem: NewsItem,
    private val onOpenSlack: (String) -> Unit,
) : TPDialogWrapper(
    width = 600,
    height = 540,
) {
    @Composable
    override fun createDesign() {
        NewsletterPopupContent(
            newsItem = newsItem,
            onClose = { close(OK_EXIT_CODE) },
            onOpenSlack = onOpenSlack,
        )
    }
}

@Composable
private fun NewsletterPopupContent(
    newsItem: NewsItem,
    onClose: () -> Unit,
    onOpenSlack: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = TPTheme.colors.gray,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Article,
                        contentDescription = null,
                        tint = TPTheme.colors.blue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TPText(
                        text = "Latest Team Newsletter",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TPTheme.colors.white
                        )
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TPTheme.colors.lightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date
            TPText(
                text = newsItem.date,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = TPTheme.colors.blue,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(newsItem.content) { content ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        TPText(
                            text = "•",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = TPTheme.colors.purple
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TPText(
                            text = content,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = TPTheme.colors.white
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { onOpenSlack(newsItem.slackUrl) },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = TPTheme.colors.blue
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open in Slack")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = TPTheme.colors.gray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close")
                }
            }
        }
    }
}