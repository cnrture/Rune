package com.github.teknasyon.plugin.service

import com.github.teknasyon.plugin.data.FileTemplate
import com.github.teknasyon.plugin.data.ModuleTemplate
import com.github.teknasyon.plugin.data.SettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import kotlinx.serialization.json.Json
import java.io.File

@State(name = "TPDevToolsSettings", storages = [Storage("gtcDevToolsSettings.xml")])
@Service(Service.Level.APP)
class SettingsService : PersistentStateComponent<SettingsState> {
    private var myState = SettingsState()

    companion object {
        private fun getAutoBackupPath(): String {
            val userHome = System.getProperty("user.home")
            val separator = File.separator
            return "$userHome${separator}.gtcdevtools${separator}settings.json"
        }

        fun getInstance(): SettingsService {
            return ApplicationManager.getApplication().getService(SettingsService::class.java)
        }
    }

    init {
        loadFromAutoBackup()
        setDefaultTemplatesIfEmpty()
    }

    override fun getState(): SettingsState {
        setDefaultTemplatesIfEmpty()
        return myState
    }

    override fun loadState(state: SettingsState) {
        myState = state
        setDefaultTemplatesIfEmpty()
        saveToAutoBackup()
    }

    private fun loadFromAutoBackup() {
        try {
            val backupFile = File(getAutoBackupPath())
            if (backupFile.exists()) {
                val jsonContent = backupFile.readText()
                val importedState = Json.decodeFromString(SettingsState.serializer(), jsonContent)

                val updatedModuleTemplates = importedState.moduleTemplates.toMutableList()
                val moduleIndex = updatedModuleTemplates.indexOfFirst { it.id == "candroid_template" }
                if (moduleIndex != -1) {
                    updatedModuleTemplates[moduleIndex] = getDefaultModuleTemplates().first()
                }
                importedState.moduleTemplates.clear()
                importedState.moduleTemplates.addAll(updatedModuleTemplates)

                myState = importedState
            }
        } catch (_: Exception) {
            setDefaultTemplatesIfEmpty()
        }
    }

    private fun saveToAutoBackup() {
        try {
            val backupFile = File(getAutoBackupPath())
            backupFile.parentFile?.mkdirs()
            backupFile.writeText(exportSettings())
        } catch (_: Exception) {
        }
    }

    fun saveTemplate(template: ModuleTemplate) {
        val existingIndex = myState.moduleTemplates.indexOfFirst { it.id == template.id }
        if (existingIndex != -1) myState.moduleTemplates[existingIndex] = template
        else myState.moduleTemplates.add(template)
        saveToAutoBackup()
    }

    fun setDefaultModuleTemplate(templateId: String) {
        myState.defaultModuleTemplateId = templateId
        saveToAutoBackup()
    }

    fun getDefaultModuleTemplate() = myState.moduleTemplates.find { it.id == myState.defaultModuleTemplateId }

    fun exportSettings(): String = Json.encodeToString(SettingsState.serializer(), myState)

    fun getModuleTemplates(): List<ModuleTemplate> {
        if (myState.moduleTemplates.isEmpty()) myState.moduleTemplates.addAll(getDefaultModuleTemplates())
        return myState.moduleTemplates
    }

    fun removeTemplate(template: ModuleTemplate) {
        myState.moduleTemplates.removeAll { it.id == template.id }
        saveToAutoBackup()
    }

    fun addModuleTemplate(template: ModuleTemplate) {
        val existingIndex = myState.moduleTemplates.indexOfFirst { it.id == template.id }
        if (existingIndex != -1) {
            myState.moduleTemplates[existingIndex] = template
        } else {
            myState.moduleTemplates.add(template)
        }
        saveToAutoBackup()
    }

    private fun setDefaultTemplatesIfEmpty() {
        if (myState.moduleTemplates.isEmpty()) myState.moduleTemplates.addAll(getDefaultModuleTemplates())

        // Always ensure candroid_template is up-to-date
        updateCanDroidTemplates()
    }

    private fun updateCanDroidTemplates() {
        // Update candroid_template in module templates
        val moduleIndex = myState.moduleTemplates.indexOfFirst { it.id == "candroid_template" }
        if (moduleIndex != -1) {
            myState.moduleTemplates[moduleIndex] = getDefaultModuleTemplates().first()
        }
    }
}

fun getDefaultModuleTemplates(): List<ModuleTemplate> {
    return listOf(
        ModuleTemplate(
            id = "candroid_template",
            name = "Candroid's Module",
            fileTemplates = listOf(
                FileTemplate(
                    fileName = "{NAME}Screen.kt",
                    filePath = "ui",
                    fileContent = """
package {FILE_PACKAGE}

abc22222
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.sp
import {FILE_PACKAGE}.{NAME}Contract.UiState
import {FILE_PACKAGE}.{NAME}Contract.UiEffect
import {FILE_PACKAGE}.{NAME}Contract.UiAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun {NAME}Screen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit
) {
    {NAME}Content(
        modifier = Modifier.fillMaxSize(),
        uiState = uiState,
        onAction = onAction,
    )
}

@Composable
private fun {NAME}Content(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "{NAME} Content",
            fontSize = 24.sp,
        )
    }
}
        
@Preview(showBackground = true)
@Composable
fun {NAME}ScreenPreview(
    @PreviewParameter({NAME}ScreenPreviewProvider::class) uiState: UiState,
) {
    {NAME}Screen(
        uiState = uiState,
        uiEffect = emptyFlow(),
        onAction = {},
    )
}
""".trimIndent(),
                ),
                FileTemplate(
                    fileName = "{NAME}ViewModel.kt",
                    filePath = "ui",
                    fileContent = """
package {FILE_PACKAGE}

import androidx.lifecycle.ViewModel
import {FILE_PACKAGE}.{NAME}Contract.UiState
import {FILE_PACKAGE}.{NAME}Contract.UiEffect
import {FILE_PACKAGE}.{NAME}Contract.UiAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class {NAME}ViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEffect by lazy { Channel<UiEffect>() }
    val uiEffect: Flow<UiEffect> by lazy { _uiEffect.receiveAsFlow() }

    fun onAction(uiAction: UiAction) {
    }

    private fun updateUiState(block: UiState.() -> UiState) {
        _uiState.update(block)
    }

    private suspend fun emitUiEffect(uiEffect: UiEffect) {
        _uiEffect.send(uiEffect)
    }
}
""".trimIndent(),
                ),
                FileTemplate(
                    fileName = "{NAME}Contract.kt",
                    filePath = "ui",
                    fileContent = """
package {FILE_PACKAGE}

object {NAME}Contract {
    data class UiState(
        val isLoading: Boolean = false,
        val list: List<String> = emptyList(),
    )

    sealed interface UiAction
            
    sealed interface UiEffect
}
""".trimIndent(),
                ),
                FileTemplate(
                    fileName = "{NAME}PreviewParameterProvider.kt",
                    filePath = "ui",
                    fileContent = """
package {FILE_PACKAGE}
                       
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class {NAME}ScreenPreviewProvider : PreviewParameterProvider<{NAME}Contract.UiState> {
    override val values: Sequence<{NAME}Contract.UiState>
        get() = sequenceOf(
            {NAME}Contract.UiState(
                isLoading = true,
                list = emptyList(),
            ),
            {NAME}Contract.UiState(
                isLoading = false,
                list = emptyList(),
            ),
            {NAME}Contract.UiState(
                isLoading = false,
                list = listOf("Item 1", "Item 2", "Item 3")
            ),
        )
}
""".trimIndent(),
                ),
            ),
            isDefault = true,
        ),
    )
}
