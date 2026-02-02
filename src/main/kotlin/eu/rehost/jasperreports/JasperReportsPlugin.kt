package eu.rehost.jasperreports

import eu.rehost.jasperreports.tasks.CompileReport
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.kotlin.dsl.register

interface JasperReportsExtension {
    val source: DirectoryProperty
    val output: DirectoryProperty
}

class JasperReportsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("jasper", JasperReportsExtension::class.java)

        extension.source.convention(project.layout.projectDirectory.dir("src/main/jasper"))
        extension.output.convention(project.layout.buildDirectory.dir("reports"))

        project.tasks.register<CompileReport>("compileJasper") {
            group = "build"
            description = "Compiles JasperReports .jrxml files (Pinned v6.21.5)"

            sourceDirectory.set(extension.source)
            outputDirectory.set(extension.output)
        }
    }
}