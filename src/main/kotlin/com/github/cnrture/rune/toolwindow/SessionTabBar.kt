package com.github.cnrture.rune.toolwindow

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.cnrture.rune.common.AppIcons
import com.github.cnrture.rune.components.TPActionCard
import com.github.cnrture.rune.components.TPActionCardType
import com.github.cnrture.rune.components.TPText
import com.github.cnrture.rune.theme.TPTheme

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
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        ) {
            TPActionCard(
                title = "Create Skill",
                icon = AppIcons.painter("auto_fix_high"),
                actionColor = TPTheme.colors.blue,
                type = TPActionCardType.EXTRA_SMALL,
                isBorderless = true,
                onClick = { onCreateSkillClick() },
            )
            TPActionCard(
                title = "Usage",
                icon = AppIcons.painter("data_usage"),
                actionColor = TPTheme.colors.blue,
                type = TPActionCardType.EXTRA_SMALL,
                isBorderless = true,
                onClick = { onUsageClick() },
            )
            TPActionCard(
                title = "Settings",
                icon = AppIcons.painter("settings"),
                actionColor = TPTheme.colors.purple,
                type = TPActionCardType.EXTRA_SMALL,
                isBorderless = true,
                onClick = { onSettingsClick() },
            )
        }
        Spacer(modifier = Modifier.size(4.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sessions.forEach { session ->
                val isActive = session.id == activeSessionId
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            color = if (isActive) TPTheme.colors.gray else TPTheme.colors.black,
                        )
                        .border(
                            width = if (isActive) 1.dp else 0.dp,
                            color = if (isActive) TPTheme.colors.blue else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
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
                    val closeHover = remember { MutableInteractionSource() }
                    val isCloseHovered by closeHover.collectIsHoveredAsState()
                    Icon(
                        painter = AppIcons.painter("close"),
                        contentDescription = "Close session",
                        tint = if (isCloseHovered) TPTheme.colors.red
                        else if (isActive) TPTheme.colors.lightGray
                        else TPTheme.colors.hintGray,
                        modifier = Modifier
                            .size(14.dp)
                            .hoverable(closeHover)
                            .clickable { onCloseSession(session.id) }
                    )
                }
            }
            @OptIn(ExperimentalFoundationApi::class)
            TooltipArea(
                tooltip = {
                    Box(
                        modifier = Modifier
                            .background(TPTheme.colors.gray, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        TPText(
                            text = "New session",
                            color = TPTheme.colors.white,
                            fontSize = 11.sp,
                        )
                    }
                },
                tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
            ) {
                val addHover = remember { MutableInteractionSource() }
                val isAddHovered by addHover.collectIsHoveredAsState()
                Icon(
                    painter = AppIcons.painter("add"),
                    contentDescription = "New session",
                    tint = if (isAddHovered) TPTheme.colors.blue else TPTheme.colors.lightGray,
                    modifier = Modifier
                        .size(24.dp)
                        .hoverable(addHover)
                        .clickable { onAddSession() }
                )
            }
        }
    }
}
