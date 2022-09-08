package software.onepiece.limits

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import software.onepiece.limits.spec.NullSpec
import software.onepiece.limits.spec.Spec

abstract class LimitsGenerationTask : DefaultTask() {

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val typeSpecs: ListProperty<Spec>

    @get:OutputDirectory
    abstract val out: DirectoryProperty

    @TaskAction
    fun generateAll() {
        out.get().asFile.listFiles().forEach { it.delete() }

        val specs = typeSpecs.get().filter { it != NullSpec }
        specs.forEach { spec ->
            val targetFolder = out.get().dir("${packageName.get().replace('.', '/')}/entities/${spec.projectName()}")
            targetFolder.asFile.mkdirs()
            val content = spec.generate(packageName.get())
            targetFolder.file("${spec.typeName()}.kt").asFile.printWriter().use {
                it.print(content)
            }

            val commandFactoryContent = spec.generateCommandFactory(packageName.get())
            if (commandFactoryContent.isNotBlank()) {
                targetFolder.file("${spec.typeName()}Commands.kt").asFile.printWriter().use {
                    it.print(commandFactoryContent)
                }
            }

            val diffToolContent = spec.generateDiffTool(packageName.get())
            if (diffToolContent.isNotBlank()) {
                targetFolder.file("${spec.typeName()}DiffTool.kt").asFile.printWriter().use {
                    it.print(diffToolContent)
                }
            }
        }
    }
}