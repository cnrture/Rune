package com.github.teknasyon.getcontactdevtools.data

data class LibraryInfo(
    val alias: String,
    val group: String,
    val artifact: String,
    val versionRef: String? = null,
    val version: String? = null,
)