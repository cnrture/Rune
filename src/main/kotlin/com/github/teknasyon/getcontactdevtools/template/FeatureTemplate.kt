package com.github.teknasyon.getcontactdevtools.template

object FeatureTemplate {
    fun getScreen(packageName: String, featureName: String) = """
        package $packageName
        
        import androidx.activity.compose.BackHandler
        import androidx.compose.foundation.background
        import androidx.compose.foundation.layout.Arrangement
        import androidx.compose.foundation.layout.Column
        import androidx.compose.foundation.layout.fillMaxSize
        import androidx.compose.foundation.layout.navigationBarsPadding
        import androidx.compose.foundation.layout.padding
        import androidx.compose.foundation.layout.statusBarsPadding
        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.tooling.preview.Preview
        import androidx.compose.ui.tooling.preview.PreviewParameter
        import app.source.getcontact.androidutility.ktx.collectAsUiEffectWithLifecycle
        import $packageName.${featureName}Contract.UiState
        import $packageName.${featureName}Contract.UiEffect
        import $packageName.${featureName}Contract.UiAction
        import app.source.getcontact.uikit.component.GetcontactText
        import app.source.getcontact.uikit.component.NonClickableFullScreenProgress
        import app.source.getcontact.uikit.component.scaffold.GetcontactScaffold
        import app.source.getcontact.uikit.component.toolbar.GetcontactToolbar
        import app.source.getcontact.uikit.theme.GetcontactTheme
        import kotlinx.coroutines.flow.Flow
        import kotlinx.coroutines.flow.emptyFlow

        @Composable
        fun ${featureName}Screen(
            uiState: UiState,
            uiEffect: Flow<UiEffect>,
            onAction: (UiAction) -> Unit
        ) {
            BackHandler { onAction(UiAction.OnBackClick) } 
       
            uiEffect.collectAsUiEffectWithLifecycle { effect ->
            }
    
            GetcontactScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GetcontactTheme.colors.background)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                topBar = {
                    GetcontactToolbar(
                        title = "$featureName",
                        onBackClick = { onAction(UiAction.OnBackClick) },
                    )
                },
            ) { paddingValues ->
                ${featureName}Content(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GetcontactTheme.colors.background)
                        .padding(paddingValues)
                        .padding(GetcontactTheme.dimensions.spacing4),
                    uiState = uiState,
                    onAction = onAction,
                )
                
                if (uiState.isLoading) NonClickableFullScreenProgress()
            }
        }

        @Composable
        private fun ${featureName}Content(
            modifier: Modifier = Modifier,
            uiState: UiState,
            onAction: (UiAction) -> Unit,
        ) {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GetcontactText(
                    modifier = Modifier.weight(1f),
                    text = "$featureName Feature",
                    testTag = "feature tag",
                    textColor = GetcontactTheme.colors.onSurface,
                    textStyle = GetcontactTheme.typography.labelMediumSemiBold,
                )
            }
        }
        
        @Preview
        @Composable
        private fun ${featureName}Preview(
            @PreviewParameter(${featureName}UiStateProvider::class) uiState: UiState,
        ) {
            GetcontactTheme {
                ${featureName}Screen(
                    uiState = uiState,
                    uiEffect = emptyFlow(),
                    onAction = {},
                )
            }
        }
    """.trimIndent()

    fun getViewModel(packageName: String, featureName: String) = """
        package $packageName
        
        import androidx.lifecycle.ViewModel
        import app.source.getcontact.event.EventManager
        import app.source.getcontact.event.di.AppEventManager
        import ${packageName}.${featureName}Contract.UiAction
        import ${packageName}.${featureName}Contract.UiEffect
        import ${packageName}.${featureName}Contract.UiState
        import app.source.getcontact.mvi.MVI
        import app.source.getcontact.mvi.mvi
        import app.source.getcontact.navigator.Navigator
        import app.source.getcontact.navigator.navigateBackUsingVMScope
        import app.source.getcontact.navigator.navigator
        import app.source.getcontact.permission.client.PermissionClient
        import app.source.getcontact.permission.client.permissionClient
        import app.source.getcontact.uikit.component.alert.AlertClient
        import app.source.getcontact.uikit.component.alert.alertClient
        import javax.inject.Inject

        class ${featureName}ViewModel @Inject constructor(
            @AppEventManager private val eventManager: EventManager,
        ) : ViewModel(),
            MVI<UiState, UiAction, UiEffect> by mvi(UiState.initial()),
            Navigator by navigator(),
            AlertClient by alertClient(),
            PermissionClient by permissionClient() {

            override fun onAction(uiAction: UiAction) {
                when (uiAction) {
                    is UiAction.OnBackClick -> navigateBackUsingVMScope()
                }
            }
        }
    """.trimIndent()

    fun getContract(packageName: String, featureName: String) = """
        package $packageName

        object ${featureName}Contract {
            data class UiState(
                val isLoading: Boolean,
            ) {
                companion object {
                    fun initial() = UiState(
                        isLoading = false,
                    )
                }
            }

            sealed interface UiAction {
                data object OnBackClick : UiAction
            }
            
            sealed interface UiEffect
        }
    """.trimIndent()

    fun getPreviewProvider(packageName: String, featureName: String) = """
        package $packageName

        import androidx.compose.ui.tooling.preview.PreviewParameterProvider

        class ${featureName}UiStateProvider : PreviewParameterProvider<${featureName}Contract.UiState> {
            override val values: Sequence<${featureName}Contract.UiState>
                get() = sequenceOf()
        }
    """.trimIndent()

    fun getComponentKey(packageName: String, featureName: String) = """
        package $packageName

        object ${featureName}ComponentKey {
            object Language {
            }

            object Test {
            }
        }
    """.trimIndent()
}