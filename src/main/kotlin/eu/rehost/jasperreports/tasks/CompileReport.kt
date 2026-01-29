package eu.rehost.jasperreports.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * Task to compile JasperReports .jrxml files into .jasper files.
 */
abstract class CompileReport @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:InputFiles
    @get:Classpath
    abstract val compilerClasspath: ConfigurableFileCollection

    @get:Input
    abstract val removeJavaSources: Property<Boolean>

    @get:Input
    abstract val validateXml: Property<Boolean>

    @get:Input
    abstract val compiler: Property<String>

    @TaskAction
    fun compile(inputChanges: InputChanges) {
        if (validateXml.get() != true) {
            logger.warn("XML validation is disabled. This may cause a SAXNotSupportedException with some JDKs.")
        }

        // Create a work queue that runs with an isolated classpath.
        // This is the core of the Worker API usage.
        val workQueue = workerExecutor.classLoaderIsolation { spec ->
            spec.classpath.from(compilerClasspath)
        }

        // Process each file change reported by Gradle.
        inputChanges.getFileChanges(sourceDirectory).forEach { change ->
            if (change.fileType == FileType.DIRECTORY || !change.file.name.endsWith(".jrxml")) {
                return@forEach // Skip directories and non-jrxml files
            }

            val relativePath = change.file.relativeTo(sourceDirectory.get().asFile)
            val outputFileName = relativePath.path.replace(".jrxml", ".jasper")
            val outputFile = outputDirectory.file(outputFileName).get().asFile

            when (change.changeType) {
                ChangeType.REMOVED -> {
                    project.delete(outputFile)
                }
                ChangeType.ADDED, ChangeType.MODIFIED -> {
                    // Ensure the parent directory for the output file exists.
                    outputFile.parentFile.mkdirs()

                    // Submit a work item to the queue for compilation.
                    // This work will be executed in a separate, isolated process.
                    workQueue.submit(CompileReportWork::class.java) { params ->
                        params.inputFile.set(change.file)
                        params.outputFile.set(outputFile)
                        params.removeJavaSources.set(this.removeJavaSources)
                        params.validateXml.set(this.validateXml)
                        params.compiler.set(this.compiler)
                        params.tempDir.set(temporaryDir)
                    }
                }
            }
        }
    }
}

/**
 * Parameters for the isolated work action.
 * These are the inputs required for the actual compilation.
 */
interface CompileReportParameters : WorkParameters {
    val inputFile: RegularFileProperty
    val outputFile: RegularFileProperty
    val removeJavaSources: Property<Boolean>
    val validateXml: Property<Boolean>
    val compiler: Property<String>
    val tempDir: Property<File>
}

/**
 * The isolated action that performs the compilation.
 * This class and its code will be executed in a worker process with the specified classpath.
 */
abstract class CompileReportWork : WorkAction<CompileReportParameters> {

    override fun execute() {
        // We use reflection because the JasperReports classes are NOT on the build script's classpath.
        // They are only present in the isolated classloader of this worker action.
        // This is intentional and is the key to classpath isolation.
        val contextClassLoader = Thread.currentThread().contextClassLoader

        try {
            // Set JasperReports system properties for the compilation context
            val defaultContextClass = contextClassLoader.loadClass("net.sf.jasperreports.engine.DefaultJasperReportsContext")
            val jrCompilerClass = contextClassLoader.loadClass("net.sf.jasperreports.engine.design.JRCompiler")
            val jrSaxParserFactoryClass = contextClassLoader.loadClass("net.sf.jasperreports.engine.xml.JRReportSaxParserFactory")

            val compilerPrefixTag = jrCompilerClass.getField("COMPILER_PREFIX").get(null) as String
            val keepJavaTag = jrCompilerClass.getField("COMPILER_KEEP_JAVA_FILE").get(null) as String
            val tempDirTag = jrCompilerClass.getField("COMPILER_TEMP_DIR").get(null) as String

            // COMPILER_XML_VALIDATION was removed in JasperReports 7.
            // We safely check for its existence.
            val validateXmlTagField = try {
                jrSaxParserFactoryClass.getField("COMPILER_XML_VALIDATION")
            } catch (e: NoSuchFieldException) {
                null
            }

            val contextInstance = defaultContextClass.getMethod("getInstance").invoke(null)
            val setPropertyMethod = defaultContextClass.getMethod("setProperty", String::class.java, String::class.java)

            if (validateXmlTagField != null) {
                val validateXmlTag = validateXmlTagField.get(null) as String
                setPropertyMethod.invoke(contextInstance, validateXmlTag, parameters.validateXml.get().toString())
            }

            // Handle Compiler Class Name Migration (JR 6 -> JR 7)
            var compilerClassName = parameters.compiler.get()
            // If the default JDT compiler is configured but not found, try the new JR 7 package structure
            if (compilerClassName == "net.sf.jasperreports.engine.design.JRJdtCompiler") {
                try {
                    contextClassLoader.loadClass(compilerClassName)
                } catch (e: ClassNotFoundException) {
                    val v7CompilerName = "net.sf.jasperreports.jdt.JRJdtCompiler"
                    try {
                        contextClassLoader.loadClass(v7CompilerName)
                        // If we found the new class, use it
                        compilerClassName = v7CompilerName
                    } catch (ignore: ClassNotFoundException) {
                        // Neither found, proceed with original to let it fail normally or be handled elsewhere
                    }
                }
            }

            setPropertyMethod.invoke(contextInstance, compilerPrefixTag, compilerClassName)
            setPropertyMethod.invoke(contextInstance, keepJavaTag, parameters.removeJavaSources.get().not().toString())
            setPropertyMethod.invoke(contextInstance, tempDirTag, parameters.tempDir.get().absolutePath)

            // Get the JasperCompileManager and invoke the compilation method
            val compileManagerClass = contextClassLoader.loadClass("net.sf.jasperreports.engine.JasperCompileManager")
            val compileMethod = compileManagerClass.getMethod("compileReportToFile", String::class.java, String::class.java)

            val sourcePath = parameters.inputFile.get().asFile.absolutePath
            val destPath = parameters.outputFile.get().asFile.absolutePath

            compileMethod.invoke(null, sourcePath, destPath)

        } catch (e: Exception) {
            // It's good practice to catch and rethrow with more context if something fails inside the worker.
            throw RuntimeException("Failed to compile JasperReport: ${parameters.inputFile.get().asFile.name}", e)
        }
    }
}