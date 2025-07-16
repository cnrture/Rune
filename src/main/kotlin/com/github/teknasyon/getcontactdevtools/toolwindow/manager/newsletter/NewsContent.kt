package com.github.teknasyon.getcontactdevtools.toolwindow.manager.newsletter

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
import com.github.teknasyon.getcontactdevtools.components.GTCActionCard
import com.github.teknasyon.getcontactdevtools.components.GTCActionCardType
import com.github.teknasyon.getcontactdevtools.components.GTCText
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
import java.awt.Desktop
import java.net.URI

@Composable
fun NewsletterContent() {
    GTCTheme {
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
                .background(GTCTheme.colors.black)
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
                    color = GTCTheme.colors.purple
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
        backgroundColor = GTCTheme.colors.gray,
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
                GTCText(
                    text = item.date,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GTCTheme.colors.blue,
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                GTCActionCard(
                    title = "Open in Slack",
                    actionColor = GTCTheme.colors.blue,
                    icon = Icons.Rounded.RocketLaunch,
                    type = GTCActionCardType.SMALL,
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
                        GTCText(
                            text = "•",
                            style = TextStyle(
                                fontSize = 24.sp,
                                color = GTCTheme.colors.blue
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        GTCText(
                            text = content,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = GTCTheme.colors.white
                            ),
                        )
                    }
                }
            }
        }
    }
}