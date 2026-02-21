package com.github.teknasyon.plugin.toolwindow.manager.newsletter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.components.TPActionCard
import com.github.teknasyon.plugin.components.TPActionCardType
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.theme.TPTheme
import java.awt.Desktop
import java.net.URI

@Composable
fun NewsletterContent() {
    TPTheme {
        val newsletters = remember { mutableStateOf<List<NewsItem>>(emptyList()) }
        val newsService = remember { NewsService() }
        val isLoading = remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            isLoading.value = true
            try {
                newsletters.value = newsService.fetchNews()
            } catch (e: Exception) {
                println("Error loading team messages: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TPTheme.colors.black)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(newsletters.value) { newsletter ->
                    NewsletterCard(newsletter)
                }
            }

            if (isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = TPTheme.colors.purple
                )
            }
        }
    }
}

@Composable
private fun NewsletterCard(item: NewsItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = TPTheme.colors.gray,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TPText(
                    text = item.date,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TPTheme.colors.blue,
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                TPActionCard(
                    title = "Open in Slack",
                    actionColor = TPTheme.colors.blue,
                    icon = Icons.Rounded.RocketLaunch,
                    type = TPActionCardType.SMALL,
                    onClick = { Desktop.getDesktop().browse(URI(item.slackUrl)) },
                )
            }

            Column {
                item.content.forEach { content ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TPText(
                            text = "•",
                            style = TextStyle(
                                fontSize = 24.sp,
                                color = TPTheme.colors.blue
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TPText(
                            text = content,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = TPTheme.colors.white
                            ),
                        )
                    }
                }
            }
        }
    }
}