package eu.rehost.jasperreports.tasks

import net.sf.jasperreports.engine.JasperCompileManager
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File

abstract class CompileReport : DefaultTask() {

    @get:InputDirectory
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun compile(inputChanges: InputChanges) {
        val srcDir = sourceDirectory.get().asFile
        val outDir = outputDirectory.get().asFile

        inputChanges.getFileChanges(sourceDirectory).forEach { change ->
            if (change.fileType == FileType.DIRECTORY || !change.file.name.endsWith(".jrxml")) return@forEach

            val relPath = change.file.relativeTo(srcDir)
            val outputFile = File(outDir, relPath.path.replace(".jrxml", ".jasper"))

            if (change.changeType == ChangeType.REMOVED) {
                outputFile.delete()
            } else {
                outputFile.parentFile.mkdirs()

                try {
                    println("Compiling: ${change.file.name}")

                    JasperCompileManager.compileReportToFile(
                        change.file.absolutePath,
                        outputFile.absolutePath
                    )

                } catch (e: Exception) {
                    throw RuntimeException("Fehler beim Kompilieren von ${change.file.name}", e)
                }
            }
        }
    }
}