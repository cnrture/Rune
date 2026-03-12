package com.github.teknasyon.plugin.toolwindow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
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

@Composable
internal fun SessionTabBar(
    sessions: List<ClaudeSession>,
    activeSessionId: Int,
    onSelectSession: (Int) -> Unit,
    onCloseSession: (Int) -> Unit,
    onAddSession: () -> Unit,
    onCreateSkillClick: () -> Unit,
    onUsageClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TPTheme.colors.black)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TPActionCard(
                title = "Create Skill",
                icon = Icons.Rounded.AutoFixHigh,
                actionColor = TPTheme.colors.blue,
                type = TPActionCardType.EXTRA_SMALL,
                isBorderless = true,
                onClick = { onCreateSkillClick() },
            )
            TPActionCard(
                title = "Usage",
                icon = Icons.Rounded.DataUsage,
                actionColor = TPTheme.colors.blue,
                type = TPActionCardType.EXTRA_SMALL,
                isBorderless = true,
                onClick = { onUsageClick() },
            )
            TPActionCard(
                title = "Settings",
                icon = Icons.Rounded.Settings,
                actionColor = TPTheme.colors.purple,
                type = TPActionCardType.EXTRA_SMALL,
                isBorderless = true,
                onClick = { onSettingsClick() },
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sessions.forEach { session ->
                val isActive = session.id == activeSessionId
                Row(
                    modifier = Modifier
                        .background(
                            color = if (isActive) TPTheme.colors.gray else TPTheme.colors.black,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectSession(session.id) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TPText(
                        text = session.title,
                        color = if (isActive) TPTheme.colors.white else TPTheme.colors.lightGray,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close session",
                        tint = if (isActive) TPTheme.colors.lightGray else TPTheme.colors.hintGray,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onCloseSession(session.id) }
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "New session",
                tint = TPTheme.colors.lightGray,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onAddSession() }
            )
        }
    }
}
