package com.github.teknasyon.getcontactplugin.template

object ManifestTemplate {
    fun getManifestTemplate(moduleName: String) = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            package="app.source.getcontact.$moduleName">
        </manifest>
    """.trimIndent()
}