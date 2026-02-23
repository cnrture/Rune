package com.github.teknasyon.plugin.toolwindow.template

object ManifestTemplate {
    fun getManifestTemplate(packageName: String) = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="https://schemas.android.com/apk/res/android"
            package="$packageName">
        </manifest>
    """.trimIndent()
}