package com.github.teknasyon.getcontactdevtools.toolwindow.manager.colorpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.getcontactdevtools.common.hsvToColor
import com.github.teknasyon.getcontactdevtools.common.toHSV
import com.github.teknasyon.getcontactdevtools.components.GTCActionCard
import com.github.teknasyon.getcontactdevtools.components.GTCActionCardType
import com.github.teknasyon.getcontactdevtools.components.GTCText
import com.github.teknasyon.getcontactdevtools.data.ColorInfo
import com.github.teknasyon.getcontactdevtools.service.SettingsService
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
import com.intellij.ui.JBColor
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.time.LocalTime
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

@Composable
fun ColorPickerContent() {
    val settings = SettingsService.getInstance()
    var colorHistory by remember { mutableStateOf(settings.getColorHistory()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GTCTheme.colors.black)
            .padding(24.dp)
    ) {
        GTCText(
            modifier = Modifier.fillMaxWidth(),
            text = "Color Picker",
            style = TextStyle(
                color = GTCTheme.colors.purple,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        )

        Spacer(modifier = Modifier.size(24.dp))

        GTCActionCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = "Pick Color",
            icon = Icons.Rounded.ColorLens,
            actionColor = GTCTheme.colors.purple,
            onClick = {
                startColorPicking { color ->
                    val colorInfo = createColorInfo(color)
                    settings.addColorToHistory(colorInfo)
                    colorHistory = settings.getColorHistory()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (colorHistory.isNotEmpty()) {
            GTCText(
                text = "Recent Colors",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = GTCTheme.colors.white,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(colorHistory) { colorInfo ->
                    ColorHistoryItem(
                        colorInfo = colorInfo,
                        onCopyHex = { copyToClipboard(colorInfo.hex) },
                        onCopyRgb = { copyToClipboard(colorInfo.rgb) },
                        onAdjustColorLightness = { percent ->
                            val adjustedColor = adjustColorLightness(colorInfo.color, percent)
                            val adjustedColorInfo = createColorInfo(adjustedColor)
                            settings.addColorToHistory(adjustedColorInfo)
                            colorHistory = settings.getColorHistory()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorInfoRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        GTCText(
            text = label,
            color = GTCTheme.colors.white,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        GTCText(
            text = value,
            color = GTCTheme.colors.purple,
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
        )
    }
}

@Composable
private fun ColorHistoryItem(
    colorInfo: ColorInfo,
    onCopyHex: () -> Unit,
    onCopyRgb: () -> Unit,
    onAdjustColorLightness: (percent: Float) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = GTCTheme.colors.gray,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colorInfo.color)
                    .border(1.dp, GTCTheme.colors.white, RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                ColorInfoRow(label = "HEX:", value = colorInfo.hex)
                Spacer(modifier = Modifier.height(8.dp))
                ColorInfoRow(label = "RGB:", value = colorInfo.rgb)
            }

            Column(
                modifier = Modifier.width(IntrinsicSize.Max)
            ) {
                GTCActionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "+20%",
                    icon = Icons.Rounded.ContentCopy,
                    actionColor = GTCTheme.colors.purple,
                    type = GTCActionCardType.SMALL,
                    onClick = { onAdjustColorLightness(1.2f) })
                Spacer(modifier = Modifier.height(8.dp))
                GTCActionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "-20%",
                    icon = Icons.Rounded.ContentCopy,
                    actionColor = GTCTheme.colors.purple,
                    type = GTCActionCardType.SMALL,
                    onClick = { onAdjustColorLightness(0.8f) })
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                GTCActionCard(
                    modifier = Modifier,
                    title = "HEX",
                    icon = Icons.Rounded.ContentCopy,
                    actionColor = GTCTheme.colors.purple,
                    type = GTCActionCardType.SMALL,
                    onClick = { onCopyHex() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                GTCActionCard(
                    modifier = Modifier,
                    title = "RGB",
                    icon = Icons.Rounded.ContentCopy,
                    actionColor = GTCTheme.colors.purple,
                    type = GTCActionCardType.SMALL,
                    onClick = { onCopyRgb() }
                )
            }
        }
    }
}

private fun startColorPicking(onColorPicked: (Color) -> Unit) {
    SwingUtilities.invokeLater {
        try {
            val robot = Robot()

            val screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
            val screenCapture = robot.createScreenCapture(screenRect)

            val frame = JFrame()
            frame.isUndecorated = true
            frame.isAlwaysOnTop = true
            frame.extendedState = JFrame.MAXIMIZED_BOTH
            frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            frame.cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)

            val panel = object : JPanel() {
                override fun paintComponent(g: Graphics?) {
                    super.paintComponent(g)
                    g?.drawImage(screenCapture, 0, 0, width, height, null)

                    val mousePos = mousePosition
                    mousePos?.let { pos ->
                        g?.color = JBColor.WHITE
                        g?.drawLine(pos.x - 10, pos.y, pos.x + 10, pos.y)
                        g?.drawLine(pos.x, pos.y - 10, pos.x, pos.y + 10)

                        g?.color = JBColor.BLACK
                        g?.drawLine(pos.x - 11, pos.y - 1, pos.x + 11, pos.y - 1)
                        g?.drawLine(pos.x - 11, pos.y + 1, pos.x + 11, pos.y + 1)
                        g?.drawLine(pos.x - 1, pos.y - 11, pos.x - 1, pos.y + 11)
                        g?.drawLine(pos.x + 1, pos.y - 11, pos.x + 1, pos.y + 11)

                        drawZoomPreview(g, pos, screenCapture, screenRect)
                    }
                }

                private fun drawZoomPreview(
                    g: Graphics?,
                    mousePos: Point,
                    screenCapture: BufferedImage,
                    screenRect: Rectangle,
                ) {
                    g?.let { graphics ->
                        val screenX = (mousePos.x.toFloat() / width * screenRect.width).toInt()
                        val screenY = (mousePos.y.toFloat() / height * screenRect.height).toInt()

                        val zoomSize = 120
                        val zoomRadius = zoomSize / 2
                        val captureSize = 20

                        val previewX = mousePos.x + 20
                        val previewY = mousePos.y - zoomSize - 10

                        val finalPreviewX = when {
                            previewX + zoomSize > width -> mousePos.x - zoomSize - 20
                            else -> previewX
                        }
                        val finalPreviewY = when {
                            previewY < 0 -> mousePos.y + 20
                            else -> previewY
                        }

                        val zoomX = (screenX - captureSize / 2).coerceIn(0, screenCapture.width - captureSize)
                        val zoomY = (screenY - captureSize / 2).coerceIn(0, screenCapture.height - captureSize)

                        val zoomArea = screenCapture.getSubimage(zoomX, zoomY, captureSize, captureSize)

                        val originalClip = graphics.clip
                        val circularShape = Ellipse2D.Double(
                            finalPreviewX.toDouble(),
                            finalPreviewY.toDouble(),
                            zoomSize.toDouble(),
                            zoomSize.toDouble()
                        )
                        (graphics as Graphics2D).clip(circularShape)

                        graphics.color = JBColor.BLACK
                        graphics.fillOval(finalPreviewX, finalPreviewY, zoomSize, zoomSize)

                        graphics.drawImage(
                            zoomArea,
                            finalPreviewX, finalPreviewY,
                            zoomSize, zoomSize,
                            null
                        )

                        graphics.color = JBColor.RED
                        val centerX = finalPreviewX + zoomRadius
                        val centerY = finalPreviewY + zoomRadius
                        graphics.drawLine(centerX - 5, centerY, centerX + 5, centerY)
                        graphics.drawLine(centerX, centerY - 5, centerX, centerY + 5)

                        graphics.clip = originalClip

                        graphics.color = JBColor.WHITE
                        graphics.drawOval(finalPreviewX, finalPreviewY, zoomSize, zoomSize)
                        graphics.color = JBColor.BLACK
                        graphics.drawOval(finalPreviewX - 1, finalPreviewY - 1, zoomSize + 2, zoomSize + 2)

                        val currentPixelColor = JBColor(
                            screenCapture.getRGB(screenX, screenY),
                            screenCapture.getRGB(screenX, screenY)
                        )
                        val hexColor = "#%02X%02X%02X".format(
                            currentPixelColor.red,
                            currentPixelColor.green,
                            currentPixelColor.blue
                        )

                        graphics.font = Font("Arial", Font.BOLD, 14)
                        val textY = finalPreviewY + zoomSize + 20

                        graphics.color = JBColor.BLACK
                        for (dx in -1..1) {
                            for (dy in -1..1) {
                                if (dx != 0 || dy != 0) {
                                    graphics.drawString(hexColor, finalPreviewX + dx, textY + dy)
                                }
                            }
                        }

                        graphics.color = JBColor.WHITE
                        graphics.drawString(hexColor, finalPreviewX, textY)
                    }
                }
            }

            panel.addMouseListener(object : MouseListener {
                override fun mouseClicked(e: MouseEvent?) {
                    e?.let { event ->
                        try {
                            val screenX = (event.x.toFloat() / panel.width * screenRect.width).toInt()
                            val screenY = (event.y.toFloat() / panel.height * screenRect.height).toInt()

                            val pixelColor = Color(screenCapture.getRGB(screenX, screenY))

                            frame.dispose()
                            onColorPicked(pixelColor)
                        } catch (_: Exception) {
                            frame.dispose()
                        }
                    }
                }

                override fun mousePressed(e: MouseEvent?) {}
                override fun mouseReleased(e: MouseEvent?) {}
                override fun mouseEntered(e: MouseEvent?) {}
                override fun mouseExited(e: MouseEvent?) {}
            })

            panel.addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseMoved(e: MouseEvent?) {
                    panel.repaint()
                }
            })

            panel.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent?) {
                    if (e?.keyCode == KeyEvent.VK_ESCAPE) {
                        frame.dispose()
                    }
                }
            })

            panel.isFocusable = true
            frame.add(panel)
            frame.isVisible = true
            panel.requestFocus()

        } catch (e: Exception) {
            println("❌ Error starting color picker: ${e.message}")
        }
    }
}

private fun createColorInfo(color: Color): ColorInfo {
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()

    val hex = "#%02X%02X%02X".format(r, g, b)
    val rgb = "rgb($r, $g, $b)"
    val timestamp = LocalTime.now().toString().substring(0, 8)

    return ColorInfo.from(color, hex, rgb, timestamp)
}

private fun copyToClipboard(text: String) {
    try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, null)
    } catch (e: Exception) {
        println("❌ Error copying to clipboard: ${e.message}")
    }
}

private fun adjustColorLightness(color: Color, percent: Float): Color {
    val hsv = color.toHSV()
    hsv[2] = (hsv[2] * percent).coerceAtMost(1f)
    val shiftedColor = Color.hsvToColor(hsv[0], hsv[1], hsv[2])
    return shiftedColor
}