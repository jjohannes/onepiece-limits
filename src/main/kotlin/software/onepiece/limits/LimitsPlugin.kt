package software.onepiece.limits

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.HasConvention
import java.io.File
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import software.onepiece.limits.spec.*


class LimitsPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("limits", LimitsPluginExtension::class.java)

        project.afterEvaluate {
            val collectedSpecs = extension.specs.map { spec -> collectSpecs(spec) }.flatten().distinct()
            collectedSpecs.forEach { spec ->
                if (spec is ContainerSpec) {
                    spec.attributes = spec.attributes.map { ref -> if (ref is SpecReference) collectedSpecs.find { it.typeName() == ref.typeName }!! else ref }
                }
            }
            val allSpecs = collectedSpecs.groupBy { it.projectName() }

            allSpecs.forEach { projectName, specs ->
                val sub = project.findProject(":$projectName") ?:
                        throw RuntimeException("Project '$projectName' needs to be included in build")
                sub.projectDir.mkdirs()
                val generatedSrcFolder = File(sub.buildDir, "generated-src")

                sub.plugins.apply("org.jetbrains.kotlin.jvm")
                sub.dependencies.add("compile", "org.jetbrains.kotlin:kotlin-stdlib:1.3.0")

                val sourceSets = sub.properties["sourceSets"] as SourceSetContainer
                val kotlinSourceSet = (sourceSets.getByName("main") as HasConvention).convention.getPlugin(KotlinSourceSet::class.java).kotlin
                kotlinSourceSet.srcDir(generatedSrcFolder)

                val generationTask = sub.tasks.create("generateLimitSources", LimitsGenerationTask::class.java)
                with(generationTask) {
                    packageName = extension.packageName
                    typeSpecs = specs
                    out = generatedSrcFolder
                }

                sub.tasks.findByName("compileKotlin")!!.dependsOn(generationTask)
                specs.forEach { spec ->
                    if (spec is ContainerSpec) {
                        if (spec.containedType.projectName() != spec.projectName && !spec.containedType.projectName().isEmpty()) {
                            val projectDependency = sub.dependencies.project(":${spec.containedType.projectName()}")
                            sub.dependencies.add("compile", projectDependency)
                        }
                        if (spec.coordinatesType.projectName() != spec.projectName && !spec.coordinatesType.projectName().isEmpty()) {
                            val projectDependency = sub.dependencies.project(":${spec.coordinatesType.projectName()}")
                            sub.dependencies.add("compile", projectDependency)
                        }
                        spec.containedSubTypes.forEach {
                            if (it.projectName() != spec.projectName && !it.projectName().isEmpty()) {
                                val projectDependency = sub.dependencies.project(":${it.projectName()}")
                                sub.dependencies.add("compile", projectDependency)
                            }
                        }
                        spec.attributes.forEach {
                            if (it.projectName() != spec.projectName && !it.projectName().isEmpty()) {
                                val projectDependency = sub.dependencies.project(":${it.projectName()}")
                                sub.dependencies.add("compile", projectDependency)
                            }
                        }
                    } else if (spec is ChainOfCoordinates) {
                        spec.components.forEach {
                            if (it.projectName() != spec.projectName && !it.projectName().isEmpty()) {
                                val projectDependency = sub.dependencies.project(":${it.projectName()}")
                                sub.dependencies.add("compile", projectDependency)
                            }
                        }
                    }
                }

            }
        }
    }

    private fun collectSpecs(spec: Spec?): Set<Spec> =
            when(spec) {
                is CoordinateSpec -> setOf(spec)
                is Coordinates2Spec -> setOf(spec, spec.xType, spec.yType)
                is ContainerSpec -> setOf(spec) + collectSpecs(spec.containedType) + spec.containedSubTypes.map { collectSpecs(it) }.flatten() + collectSpecs(spec.coordinatesType) + collectSpecs(spec.containedLocation) + spec.attributes.map { collectSpecs(it) }.flatten()
                is SuperContainerSpec -> setOf(spec)
                is AdapterSpec -> setOf(spec)
                is ChainOfCoordinates -> setOf(spec) + spec.components
                else -> setOf()
            }
}