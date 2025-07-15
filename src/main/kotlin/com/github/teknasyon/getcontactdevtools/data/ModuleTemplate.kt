package com.github.teknasyon.getcontactdevtools.data

import com.github.teknasyon.getcontactdevtools.common.Constants
import kotlinx.serialization.Serializable

@Serializable
data class ModuleTemplate(
    val id: String,
    val name: String,
    val fileTemplates: List<FileTemplate> = emptyList(),
    val isDefault: Boolean = false,
) {
    companion object {
        val EMPTY = ModuleTemplate(
            id = Constants.EMPTY,
            name = Constants.EMPTY,
            fileTemplates = emptyList(),
            isDefault = false
        )
    }
}