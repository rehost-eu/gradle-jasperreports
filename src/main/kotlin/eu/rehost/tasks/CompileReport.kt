package eu.rehost.tasks

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
import java.net.URLClassLoader
import javax.inject.Inject

abstract class CompileReport @Inject constructor(private val executor: WorkerExecutor) : DefaultTask() {

    @get:Incremental
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputFiles
    abstract val inputFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:InputFiles
    abstract val compilerSources: ConfigurableFileCollection

    @get:Input
    abstract val removeSources: Property<Boolean>

    @get:Input
    abstract val validateXml: Property<Boolean>

    @get:Input
    abstract val compilerPrefix: Property<String>

    @TaskAction
    fun compile(changes: InputChanges) {
        if (validateXml.get() != true) {
            logger.warn("Setting xml validation to false is known to cause jasperreports to crash with a org.xml.sax.SAXNotSupportedException.\nThis task is very likely to fail!")
        }

//        val workerPool = executor.classLoaderIsolation() {
//            it.classpath.from(compilerSources)
//        }

        val ownLoader = Thread.currentThread().contextClassLoader
        val compilerURLs = compilerSources.map { it.toURI().toURL() }
        val customLoader = URLClassLoader(compilerURLs.toTypedArray(), ownLoader)

        val jrcompilerClass = customLoader.loadClass("net.sf.jasperreports.engine.design.JRCompiler")
        val compilerPrefixTag = jrcompilerClass.getDeclaredField("COMPILER_PREFIX").get(null)
        val keepJavaTag = jrcompilerClass.getDeclaredField("COMPILER_KEEP_JAVA_FILE").get(null)
        val tempDirTag = jrcompilerClass.getDeclaredField("COMPILER_TEMP_DIR").get(null)

        val jrXMLFactoryClass = customLoader.loadClass("net.sf.jasperreports.engine.xml.JRReportSaxParserFactory")
        val validateXMLTag = jrXMLFactoryClass.getField("COMPILER_XML_VALIDATION").get(null)

        val defaultContextClass = customLoader.loadClass("net.sf.jasperreports.engine.DefaultJasperReportsContext")
        val getDefaultCTXInstance = defaultContextClass.getMethod("getInstance")
        val setPropertyMethod = defaultContextClass.getMethod("setProperty", String::class.java, String::class.java)
        // val getPropertyMethod = defaultContextClass.getMethod("getProperty", String::class.java)
        val ctxInstance = getDefaultCTXInstance.invoke(null)
        setPropertyMethod.invoke(ctxInstance, validateXMLTag, validateXml.get().toString())
        setPropertyMethod.invoke(ctxInstance, compilerPrefixTag, compilerPrefix.get())
        setPropertyMethod.invoke(ctxInstance, keepJavaTag, removeSources.get().not().toString())
        setPropertyMethod.invoke(ctxInstance, tempDirTag, temporaryDir.absolutePath)

        val compilerManagerClass = customLoader.loadClass("net.sf.jasperreports.engine.JasperCompileManager")

        changes.getFileChanges(inputFiles).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach
            else if (change.file.extension != "jrxml") return@forEach

            // calculated the output file name and path based on the shortest relative input file path
            val bestBasePath = inputFiles.minBy { change.file.relativeTo(it).toPath().nameCount }
            val changedPath = change.file.relativeTo(bestBasePath)
            val outputTarget = outputDir.file(changedPath.path.substring(0, changedPath.path.length - 5) + "jasper").get().asFile

            // make sure the output path exists before jasperreports tries to write there
            outputTarget.parentFile.mkdirs()

            if (change.changeType == ChangeType.REMOVED) {
                outputTarget.delete()
            } else {
//                workerPool.submit(CompileReportWork::class.java) {
//                    it.removeSources.set(removeSources)
//                    it.validateXml.set(validateXml.get())
//                    it.compilerPrefix.set(compilerPrefix.get())
//                    it.tempDir.set(temporaryDir.canonicalPath)
//
//                    it.input.set(change.file)
//                    it.output.set(outputTarget)
//                }

                compilerManagerClass.getDeclaredMethod("compileReportToFile", String::class.java, String::class.java)
                    .invoke(null, change.file.absolutePath, outputTarget.absolutePath)
            }
        }
    }

    init {
        group = "build"
    }
}
interface CompileReportParameters : WorkParameters {
    val removeSources: Property<Boolean>
    val validateXml: Property<Boolean>
    val compilerPrefix: Property<String>
    val tempDir: Property<String>

    val input: RegularFileProperty
    val output: Property<File>
}

abstract class CompileReportWork : WorkAction<CompileReportParameters> {
    override fun execute() {
        val params = parameters
        val ownLoader = Thread.currentThread().contextClassLoader

        // Get all classes involved in setting up the default context of the jasperreports compiler
        val jrcompilerClass = ownLoader.loadClass("net.sf.jasperreports.engine.design.JRCompiler")
        val compilerPrefixTag = jrcompilerClass.getDeclaredField("COMPILER_PREFIX").get(null)
        val keepJavaTag = jrcompilerClass.getDeclaredField("COMPILER_KEEP_JAVA_FILE").get(null)
        val tempDirTag = jrcompilerClass.getDeclaredField("COMPILER_TEMP_DIR").get(null)

        val jrXMLFactoryClass = ownLoader.loadClass("net.sf.jasperreports.engine.xml.JRReportSaxParserFactory")
        val validateXMLTag = jrXMLFactoryClass.getField("COMPILER_XML_VALIDATION").get(null)

        val defaultContextClass = ownLoader.loadClass("net.sf.jasperreports.engine.DefaultJasperReportsContext")
        val getDefaultCTXInstance = defaultContextClass.getMethod("getInstance")
        val setPropertyMethod = defaultContextClass.getMethod("setProperty", String::class.java, String::class.java)
        // val getPropertyMethod = defaultContextClass.getMethod("getProperty", String::class.java)
        val ctxInstance = getDefaultCTXInstance.invoke(null)
        setPropertyMethod.invoke(ctxInstance, validateXMLTag, parameters.validateXml.get().toString())
        setPropertyMethod.invoke(ctxInstance, compilerPrefixTag, parameters.compilerPrefix.get())
        setPropertyMethod.invoke(ctxInstance, keepJavaTag, parameters.removeSources.get().not().toString())
        setPropertyMethod.invoke(ctxInstance, tempDirTag, parameters.tempDir.get())

        val compilerManagerClass = ownLoader.loadClass("net.sf.jasperreports.engine.JasperCompileManager")
        compilerManagerClass.getDeclaredMethod("compileReportToFile", String::class.java, String::class.java)
            .invoke(null, params.input.get().asFile.absolutePath, params.output.get().absolutePath)
    }
}