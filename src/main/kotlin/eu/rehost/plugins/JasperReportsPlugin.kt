package eu.rehost.plugins

import eu.rehost.tasks.CompileReport
import org.gradle.api.Plugin
import org.gradle.api.Project

class JasperReportsPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        // println("Applying jasperreports plugin!")

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