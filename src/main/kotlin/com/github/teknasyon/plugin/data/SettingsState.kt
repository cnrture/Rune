package com.github.teknasyon.plugin.data

import kotlinx.serialization.Serializable

@Serializable
data class SettingsState(
    var moduleTemplates: MutableList<ModuleTemplate> = mutableListOf(),
    var defaultModuleTemplateId: String = "candroid_template",
    var defaultFeatureTemplateId: String = "candroid_template",
    var isActionsExpanded: Boolean = true,
)