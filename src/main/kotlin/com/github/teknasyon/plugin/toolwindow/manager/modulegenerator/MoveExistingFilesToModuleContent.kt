package com.github.teknasyon.plugin.toolwindow.manager.modulegenerator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.github.teknasyon.plugin.common.Constants
import com.github.teknasyon.plugin.common.Utils
import com.github.teknasyon.plugin.common.file.FileWriter
import com.github.teknasyon.plugin.common.file.LibraryDependencyFinder
import com.github.teknasyon.plugin.components.TPActionCard
import com.github.teknasyon.plugin.components.TPActionCardType
import com.github.teknasyon.plugin.data.PluginListItem
import com.github.teknasyon.plugin.theme.TPTheme
import com.github.teknasyon.plugin.toolwindow.manager.modulegenerator.components.*
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

@Composable
fun MoveExistingFilesToModuleContent(
    modifier: Modifier = Modifier,
    project: Project,
    fileWriter: FileWriter,
    isAnalyzingState: Boolean,
    analysisResultState: String?,
    selectedSrc: String,
    libraryDependencyFinder: LibraryDependencyFinder,
    onAnalysisResultChange: (String?) -> Unit,
    onAnalyzingChange: (Boolean) -> Unit,
    onDetectedModulesLoaded: (List<String>) -> Unit,
    onSelectedModulesLoaded: (List<String>) -> Unit,
    detectedModules: List<String>,
    moduleType: String,
    packageName: String,
    onPackageNameChanged: (String) -> Unit,
    moduleNameState: String,
    onModuleNameChanged: (String) -> Unit,
    onModuleTypeSelected: (String) -> Unit,
    existingModules: List<String>,
    selectedModules: List<String>,
    onCheckedModule: (String) -> Unit,
    availableLibraries: List<String>,
    selectedLibraries: List<String>,
    onLibrarySelected: (String) -> Unit,
    libraryGroups: Map<String, List<String>>,
    expandedGroups: Map<String, Boolean>,
    onGroupExpandToggle: (String) -> Unit,
    showFileTreeDialog: Boolean,
    onFileTreeDialogStateChange: () -> Unit,
    onSelectedSrc: (String) -> Unit,
    availablePlugins: List<PluginListItem>,
    selectedPlugins: List<PluginListItem>,
    onPluginSelected: (PluginListItem) -> Unit,
) {
    val radioOptions = listOf(Constants.ANDROID, Constants.KOTLIN)
    Row(modifier = modifier) {
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.4f),
            visible = showFileTreeDialog,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
        ) {
            FileTreePanel(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.4f),
                project = project,
                onSelectedSrc = { onSelectedSrc(it) }
            )
        }
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .weight(0.6f),
            backgroundColor = TPTheme.colors.black,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    TPActionCard(
                        title = "Create",
                        icon = Icons.Rounded.Create,
                        actionColor = TPTheme.colors.blue,
                        type = TPActionCardType.MEDIUM,
                        onClick = {
                            if (Utils.validateModuleInput(packageName, moduleNameState) && selectedSrc.isNotEmpty()) {
                                Utils.createModule(
                                    project = project,
                                    fileWriter = fileWriter,
                                    selectedSrc = selectedSrc,
                                    packageName = packageName,
                                    moduleName = moduleNameState,
                                    moduleType = moduleType,
                                    isMoveFiles = true,
                                    libraryDependencyFinder = libraryDependencyFinder,
                                    selectedModules = selectedModules,
                                    selectedLibraries = selectedLibraries,
                                    selectedPlugins = selectedPlugins,
                                )
                            } else {
                                Utils.showInfo(
                                    message = "Please fill out required values",
                                    type = NotificationType.WARNING,
                                )
                            }
                        }
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
                Spacer(modifier = Modifier.size(16.dp))
                ModuleTypeNameContent(
                    moduleTypeSelectionState = moduleType,
                    packageName = packageName,
                    moduleNameState = moduleNameState,
                    radioOptions = radioOptions,
                    onPackageNameChanged = onPackageNameChanged,
                    onModuleTypeSelected = onModuleTypeSelected,
                    onModuleNameChanged = onModuleNameChanged,
                )

                Spacer(modifier = Modifier.size(32.dp))

                RootSelectionContent(
                    selectedSrc = selectedSrc,
                    showFileTreeDialog = showFileTreeDialog,
                    onChooseRootClick = { onFileTreeDialogStateChange() }
                )

                Spacer(modifier = Modifier.size(16.dp))

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

                Spacer(modifier = Modifier.size(16.dp))

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
}