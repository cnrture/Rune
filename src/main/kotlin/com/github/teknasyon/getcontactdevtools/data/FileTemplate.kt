package com.github.teknasyon.getcontactdevtools.data

import kotlinx.serialization.Serializable

@Serializable
data class FileTemplate(
    val fileName: String = "",
    val filePath: String = "",
    val fileContent: String = "",
)