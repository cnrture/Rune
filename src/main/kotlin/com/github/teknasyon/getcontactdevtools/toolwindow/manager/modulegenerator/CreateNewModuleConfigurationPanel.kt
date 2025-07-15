package com.github.teknasyon.getcontactdevtools.toolwindow.manager.modulegenerator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Create
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.teknasyon.getcontactdevtools.common.Constants
import com.github.teknasyon.getcontactdevtools.common.Utils
import com.github.teknasyon.getcontactdevtools.common.file.FileWriter
import com.github.teknasyon.getcontactdevtools.common.file.LibraryDependencyFinder
import com.github.teknasyon.getcontactdevtools.components.GTCActionCard
import com.github.teknasyon.getcontactdevtools.components.GTCActionCardType
import com.github.teknasyon.getcontactdevtools.data.ModuleTemplate
import com.github.teknasyon.getcontactdevtools.data.PluginListItem
import com.github.teknasyon.getcontactdevtools.service.SettingsService
import com.github.teknasyon.getcontactdevtools.theme.GTCTheme
import com.github.teknasyon.getcontactdevtools.toolwindow.manager.modulegenerator.components.*
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateNewModuleConfigurationPanel(
    modifier: Modifier = Modifier,
    project: Project,
    fileWriter: FileWriter,
    selectedSrc: String,
    libraryDependencyFinder: LibraryDependencyFinder,
    moduleType: String,
    packageName: String,
    nameState: String,
    onNameChanged: (String) -> Unit,
    onPackageNameChanged: (String) -> Unit,
    moduleNameState: String,
    onModuleNameChanged: (String) -> Unit,
    onModuleTypeSelected: (String) -> Unit,
    availableLibraries: List<String>,
    selectedLibraries: List<String>,
    onLibrarySelected: (String) -> Unit,
    libraryGroups: Map<String, List<String>>,
    expandedGroups: Map<String, Boolean>,
    onGroupExpandToggle: (String) -> Unit,
    availablePlugins: List<PluginListItem>,
    selectedPlugins: List<PluginListItem>,
    onPluginSelected: (PluginListItem) -> Unit,
    templates: List<ModuleTemplate>,
    selectedTemplate: ModuleTemplate?,
    onTemplateSelected: (ModuleTemplate?) -> Unit,
    isAnalyzingState: Boolean,
    analysisResultState: String?,
    onAnalysisResultChange: (String?) -> Unit,
    onAnalyzingChange: (Boolean) -> Unit,
    onDetectedModulesLoaded: (List<String>) -> Unit,
    onSelectedModulesLoaded: (List<String>) -> Unit,
    detectedModules: List<String>,
    existingModules: List<String>,
    selectedModules: List<String>,
    onCheckedModule: (String) -> Unit,
) {
    val radioOptions = listOf(Constants.ANDROID, Constants.KOTLIN)
    val settings = SettingsService.getInstance()

    Scaffold(
        modifier = modifier,
        backgroundColor = GTCTheme.colors.black,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                GTCActionCard(
                    title = "Create",
                    icon = Icons.Rounded.Create,
                    actionColor = GTCTheme.colors.blue,
                    type = GTCActionCardType.MEDIUM,
                    onClick = {
                        if (Utils.validateModuleInput(packageName, moduleNameState) && selectedSrc.isNotEmpty()) {
                            Utils.createModule(
                                project = project,
                                fileWriter = fileWriter,
                                selectedSrc = selectedSrc,
                                packageName = packageName,
                                moduleName = moduleNameState,
                                name = nameState,
                                moduleType = moduleType,
                                isMoveFiles = false,
                                libraryDependencyFinder = libraryDependencyFinder,
                                selectedModules = selectedModules,
                                selectedLibraries = selectedLibraries,
                                selectedPlugins = selectedPlugins,
                                template = selectedTemplate,
                            )
                        } else {
                            Utils.showInfo(
                                message = "Please fill out required values",
                                type = NotificationType.WARNING,
                            )
                        }
                    },
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            ModuleTypeNameContent(
                moduleTypeSelectionState = moduleType,
                packageName = packageName,
                moduleNameState = moduleNameState,
                radioOptions = radioOptions,
                onPackageNameChanged = onPackageNameChanged,
                onModuleTypeSelected = onModuleTypeSelected,
                onModuleNameChanged = onModuleNameChanged,
            )
            Spacer(modifier = Modifier.height(32.dp))
            TemplateSelectionContent(
                templates = templates,
                selectedTemplate = selectedTemplate,
                nameState = nameState,
                defaultTemplateId = settings.state.defaultModuleTemplateId,
                onTemplateSelected = onTemplateSelected,
                onNameChanged = onNameChanged,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DetectedModulesContent(
                project = project,
                isAnalyzingState = isAnalyzingState,
                analysisResultState = analysisResultState,
                selectedSrc = selectedSrc,
                onAnalysisResultChange = onAnalysisResultChange,
                onAnalyzingChange = onAnalyzingChange,
                onDetectedModulesLoaded = onDetectedModulesLoaded,
                onSelectedModulesLoaded = onSelectedModulesLoaded,
                detectedModules = detectedModules,
                existingModules = existingModules,
                selectedModules = selectedModules,
                onCheckedModule = onCheckedModule,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PluginSelectionContent(
                    availablePlugins = availablePlugins,
                    onPluginSelected = onPluginSelected,
                )
                LibrarySelectionContent(
                    availableLibraries = availableLibraries,
                    selectedLibraries = selectedLibraries,
                    onLibrarySelected = onLibrarySelected,
                    libraryGroups = libraryGroups,
                    expandedGroups = expandedGroups,
                    onGroupExpandToggle = onGroupExpandToggle,
                )
            }
        }
    }
}
