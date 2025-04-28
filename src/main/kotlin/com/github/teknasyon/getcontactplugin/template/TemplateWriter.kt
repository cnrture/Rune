package com.github.teknasyon.getcontactplugin.template

import com.github.teknasyon.getcontactplugin.common.Constants
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateException
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.Writer
import java.nio.file.Paths

class TemplateWriter {

    private val cfg = Configuration(Constants.FREEMARKER_VERSION).apply {
        setClassLoaderForTemplateLoading(TemplateWriter::class.java.classLoader, "")
    }

    fun createGradleFile(
        moduleFile: File,
        moduleName: String,
        moduleType: String,
        gradleFileFollowModule: Boolean,
        packageName: String,
    ): List<File> {
        try {
            val data: MutableMap<String, Any> = HashMap()
            data["packageName"] = packageName

            val gradleTemplate: Template = when (moduleType) {
                Constants.KOTLIN -> Template(null, KotlinModuleKtsTemplate.data, cfg)
                Constants.ANDROID -> Template(null, AndroidModuleKtsTemplate.data, cfg)
                else -> throw IllegalArgumentException("Unknown module type")
            }

            val fileName = if (gradleFileFollowModule) {
                moduleName.plus(".gradle.kts")
            } else {
                "build".plus(".gradle.kts")
            }

            val filePath = Paths.get(moduleFile.absolutePath, fileName).toFile()

            val file: Writer = FileWriter(Paths.get(moduleFile.absolutePath, fileName).toFile())
            gradleTemplate.process(data, file)
            file.flush()
            file.close()

            return listOf(filePath)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: TemplateException) {
            e.printStackTrace()
        }

        return emptyList()
    }

    fun createReadmeFile(moduleFile: File, moduleName: String): List<File> {
        try {
            val manifestTemplate = Template(null, ModuleReadMeTemplate.data, cfg)
            val data: MutableMap<String, Any> = HashMap()
            data["moduleName"] = moduleName
            val manifestFile = Paths.get(moduleFile.absolutePath).toFile()
            val filePath = Paths.get(manifestFile.absolutePath, "README.md").toFile()
            manifestFile.mkdirs()

            val file: Writer = FileWriter(filePath)
            manifestTemplate.process(data, file)
            file.flush()
            file.close()

            return listOf(filePath)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: TemplateException) {
            e.printStackTrace()
        }

        return emptyList()
    }
}