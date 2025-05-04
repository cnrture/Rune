package com.github.teknasyon.getcontactdevtools.common

import freemarker.template.Configuration
import freemarker.template.Version

object Constants {
    const val EMPTY = ""

    const val WINDOW_WIDTH = 1200
    const val WINDOW_HEIGHT = 800

    const val ANDROID = "Android"
    const val KOTLIN = "Kotlin / JVM"

    const val DEFAULT_MODULE_NAME = ":repository:database (as an example)"
    const val DEFAULT_SRC_VALUE = "EMPTY"

    const val DEFAULT_BASE_PACKAGE_NAME = "app.source.getcontact"

    const val DEFAULT_EXIT_CODE = 2

    val FREEMARKER_VERSION: Version = Configuration.VERSION_2_3_30
}