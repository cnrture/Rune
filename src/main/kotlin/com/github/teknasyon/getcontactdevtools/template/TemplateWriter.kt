package com.github.teknasyon.getcontactdevtools.template

import com.github.teknasyon.getcontactdevtools.common.Constants
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
        packageName: String,
        moduleFile: File,
        moduleType: String,
        moduleName: String,
        dependencies: List<String> = emptyList(),
    ): List<File> {
        try {
            val data: MutableMap<String, Any> = HashMap()
            data["packageName"] = packageName

            val gradleTemplate = when (moduleType) {
                Constants.ANDROID -> GradleTemplate.getAndroidModuleGradleTemplate(
                    packageName = packageName,
                    dependencies = buildDependenciesBlock(dependencies),
                    moduleName = moduleName,
                )

                else -> GradleTemplate.getKotlinModuleGradleTemplate()
            }

            val fileName = "build.gradle"
            val filePath = Paths.get(moduleFile.absolutePath, fileName).toFile()
            val file: Writer = FileWriter(Paths.get(moduleFile.absolutePath, fileName).toFile())
            file.write(gradleTemplate)
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

    private fun buildDependenciesBlock(dependencies: List<String>): String {
        if (dependencies.isEmpty()) return Constants.EMPTY
        return StringBuilder().apply {
            append("// Module Dependencies\n")
            dependencies.forEachIndexed { index, module ->
                val moduleName = module.removePrefix(":").replace(":", ".")
                append("    implementation(projects.$moduleName)")
                if (index != dependencies.lastIndex) append("\n")
            }
        }.toString()
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