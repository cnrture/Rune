package com.github.teknasyon.getcontactplugin.template

object AndroidModuleKtsTemplate {
    val data = """
        plugins {
            id("com.android.library")
        }

        android {
            namespace = "${'$'}{packageName}"
        }

        dependencies {

        }
    """.trimIndent()
}