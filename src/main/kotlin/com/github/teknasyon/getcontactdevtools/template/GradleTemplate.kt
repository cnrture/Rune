package com.github.teknasyon.getcontactdevtools.template

object GradleTemplate {
    fun getAndroidModuleGradleTemplate(packageName: String, dependencies: String) = """
plugins {
    id 'com.android.library'
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

apply from: '../../gradle/config/flavorConfig.gradle'
apply from: '../../gradle/config/keyhider/config.gradle'

def configuration = rootProject.ext.configuration

android {
    namespace '$packageName'
    compileSdkVersion configuration.compileSdkVersion

    defaultConfig {
        minSdkVersion configuration.minSdkVersion
        targetSdkVersion configuration.targetSdkVersion
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig = true
    }

    with flavorConfig
}

dependencies {
    $dependencies
}""".trimIndent()

    fun getKotlinModuleGradleTemplate() = """
plugins {
    id 'java-library'
    id 'org.jetbrains.kotlin.jvm'
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(libs.ksp)
    detektPlugins libs.detekt.formatting
    detektPlugins libs.detekt.composeRules
}""".trimIndent()
}