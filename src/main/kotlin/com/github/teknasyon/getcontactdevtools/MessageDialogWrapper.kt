package com.github.teknasyon.getcontactdevtools

import com.github.teknasyon.getcontactdevtools.common.Constants
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

class MessageDialogWrapper(private val message: String) : DialogWrapper(true) {

    init {
        init()
    }

    @Nullable
    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel(BorderLayout())
        dialogPanel.preferredSize = Dimension(120, 100)

        JTextArea(message).apply {
            isEditable = false
            dialogPanel.add(this, BorderLayout.CENTER)
        }

        return dialogPanel
    }

    override fun createActions(): Array<Action> {
        return arrayOf(DialogWrapperExitAction("Okay", Constants.DEFAULT_EXIT_CODE))
    }
}