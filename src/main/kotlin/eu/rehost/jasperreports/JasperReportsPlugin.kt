package eu.rehost.jasperreports

import eu.rehost.jasperreports.tasks.CompileReport
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

interface JasperReportsExtension {
    /** The source directory containing .jrxml files. */
    val source: DirectoryProperty
    /** The destination directory for compiled .jasper files. */
    val output: DirectoryProperty
}

class JasperReportsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("jasper", JasperReportsExtension::class.java)

        // Set default directories using lazy conventions
        extension.source.convention(project.layout.projectDirectory.dir("src/main/jasper"))
        extension.output.convention(project.layout.buildDirectory.dir("reports"))

        // Create a custom configuration for the JasperReports compiler and its dependencies.
        // This isolates the tool's classpath from the project's build/runtime classpaths.
        val compilerDeps = project.configurations.create("jasperCompiler") { conf ->
            conf.isCanBeConsumed = false
            conf.isCanBeResolved = true
            conf.description = "The JasperReports compiler dependencies used by the compile tasks."
            conf.defaultDependencies { deps ->
                // Add default dependencies. Users can override by adding their own to this configuration.
                deps.add(project.dependencies.create("net.sf.jasperreports:jasperreports:6.21.0"))
                // iText is a common dependency for PDF generation, included for convenience.
                deps.add(project.dependencies.create("com.lowagie:itext:2.1.7.js10"))
            }
        }

        // Register the main, default task.
        val compileJasperTask = project.tasks.register("compileJasper", CompileReport::class.java) { task ->
            task.description = "Compiles JasperReport source files (.jrxml)."
            // Connect the task's properties lazily to the extension's properties.
            task.sourceDirectory.set(extension.source)
            task.outputDirectory.set(extension.output)
        }

        // For all tasks of type CompileReport, apply common conventions.
        // This allows users to define their own custom compilation tasks easily.
        project.tasks.withType(CompileReport::class.java).configureEach { task ->
            task.group = "build"
            task.compilerClasspath.from(compilerDeps)
            task.validateXml.convention(true)
            task.removeJavaSources.convention(true)
            task.compiler.convention("net.sf.jasperreports.engine.design.JRJdtCompiler")
        }

        // Integrate with the Java plugin if it is applied
        project.plugins.withId("java") {
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).resources {
                // Add the output of the compilation to the main resources
                it.srcDir(compileJasperTask)
            }
        }
    }
}
