package com.github.teknasyon.plugin.data

data class PluginInfo(
    val alias: String,
    val id: String,
    val versionRef: String? = null,
    val version: String? = null,
)