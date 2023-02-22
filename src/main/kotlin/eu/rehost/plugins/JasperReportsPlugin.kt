package eu.rehost.plugins

import eu.rehost.tasks.CompileReport
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty

interface JasperReportsExtension {
    val source: DirectoryProperty
    val output: DirectoryProperty
}

class JasperReportsPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        // println("Applying jasperreports plugin!")
        val extension = project.extensions.create("jasper", JasperReportsExtension::class.java)
        extension.source.convention(project.layout.projectDirectory.dir("src/main/jasper"))
        extension.output.convention(project.layout.buildDirectory.dir("reports"))

        // Create a custom configuration to specify the desired version of jasperreports to use during compilation
        val compilerDeps = project.configurations.create("compileReports") {
            conf ->
            conf.isVisible = false
            conf.isCanBeConsumed = false
            conf.isCanBeResolved = true
            conf.description = "The specific Jasper Reports files used in the compilation tasks."
            conf.defaultDependencies {
                deps ->
                deps.add(project.dependencies.create("com.lowagie:itext:2.1.7"))
                deps.add(project.dependencies.create("net.sf.jasperreports:jasperreports:6.20.0"))
            }
        }

        val defaultTask = project.tasks.register("compileJasper", CompileReport::class.java) {
            task ->
            task.inputFiles.from(extension.source.get())
            task.outputDir.set(extension.output.get())
        }

        // Automatically route the specified compiler files to all tasks unless the user overrides them manually
        project.tasks.withType(CompileReport::class.java).configureEach {
            task ->
            task.compilerSources.from(compilerDeps)
            task.validateXml.set(true)
            task.removeSources.set(true)
            task.compilerPrefix.set("net.sf.jasperreports.engine.design.JRJdtCompiler")
        }
    }
    
}