package software.onepiece.limits

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import software.onepiece.limits.spec.ContainerSpec
import software.onepiece.limits.spec.CoordinateSpec
import software.onepiece.limits.spec.Coordinates2Spec
import software.onepiece.limits.spec.NullSpec
import software.onepiece.limits.spec.Spec
import software.onepiece.limits.spec.SpecReference
import software.onepiece.limits.spec.SuperContainerSpec

abstract class LimitsGenerationTask : DefaultTask() {

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val typeSpecs: ListProperty<Spec>

    @get:OutputDirectory
    abstract val out: DirectoryProperty

    @TaskAction
    fun generateAll() {
        val specs = typeSpecs.get().map { spec -> collectSpecs(spec) }.flatten().distinct().filter { it != NullSpec }
        specs.filterIsInstance<ContainerSpec>().forEach { spec ->
            spec.attributes = spec.attributes.map { ref -> if (ref is SpecReference) specs.find { it.typeName() == ref.typeName }!! else ref }
        }
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

    private fun collectSpecs(spec: Spec?): Set<Spec> =
        when(spec) {
            is CoordinateSpec -> setOf(spec)
            is Coordinates2Spec -> setOf(spec, spec.xType, spec.yType)
            is ContainerSpec -> setOf(spec) + collectSpecs(spec.containedType) + spec.containedSubTypes.map { collectSpecs(it) }.flatten() + collectSpecs(spec.coordinatesType) + spec.attributes.map { collectSpecs(it) }.flatten()
            is SuperContainerSpec -> setOf(spec)
            else -> setOf()
        }
}