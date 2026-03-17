package com.github.teknasyon.plugin.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposePanel
import com.github.teknasyon.plugin.common.SkikoHelper
import com.github.teknasyon.plugin.theme.TPTheme
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JRootPane
import javax.swing.border.Border

abstract class TPDialogWrapper(
    width: Int = 0,
    height: Int = 0,
    modal: Boolean = true,
) : DialogWrapper(modal) {

    init {
        init()
        if (width > 0 && height > 0) setSize(width, height)
        window?.setLocationRelativeTo(null)

        if (!modal) {
            isModal = false
            window?.let { window ->
                window.isAlwaysOnTop = false
                window.isAutoRequestFocus = false
                window.focusableWindowState = true
            }
        }
    }

    @Composable
    abstract fun createDesign()

    override fun createCenterPanel(): JComponent {
        SkikoHelper.ensureNativeLibrary()
        return ComposePanel().apply {
            setContent {
                TPTheme {
                    createDesign()
                }
            }
        }
    }

    override fun createActions(): Array<Action> = emptyArray()

    override fun createSouthPanel(): JComponent {
        val southPanel = super.createSouthPanel()
        for (component in southPanel.components) {
            if (component is JComponent) component.isOpaque = true
        }
        return southPanel
    }

    override fun getRootPane(): JRootPane? {
        val rootPane = super.getRootPane()
        return rootPane
    }

    override fun createContentPaneBorder(): Border = JBUI.Borders.empty()
}