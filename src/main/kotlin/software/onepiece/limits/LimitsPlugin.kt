package software.onepiece.limits

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.HasConvention
import software.onepiece.limits.spec.ContainerSpec
import software.onepiece.limits.spec.CoordinatesSpec
import software.onepiece.limits.spec.CoordinateSpec
import software.onepiece.limits.spec.TypeSpec
import java.io.File
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet


class LimitsPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("limits", LimitsPluginExtension::class.java)

        project.afterEvaluate {
            val allSpecs = extension.specs.map { spec -> collectSpecs(spec) }.flatten().distinct().groupBy { it.projectName() }

            allSpecs.forEach { projectName, specs ->
                val sub = project.findProject(":$projectName") ?:
                        throw RuntimeException("Project '$projectName' needs to be included in build")
                sub.projectDir.mkdirs()
                val generatedSrcFolder = File(sub.buildDir, "generated-src")

                sub.plugins.apply("org.jetbrains.kotlin.jvm")
                sub.dependencies.add("compile", "org.jetbrains.kotlin:kotlin-stdlib-jre8:1.2.21")

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
                        if (spec.refType.projectName() != spec.projectName && !spec.refType.projectName().isEmpty()) {
                            val projectDependency = sub.dependencies.project(mapOf("path" to ":${spec.refType.projectName()}"))
                            sub.dependencies.add("compile", projectDependency)
                        }
                        if (spec.containedType != spec.refType && spec.containedType.projectName() != spec.projectName && !spec.containedType.projectName().isEmpty()) {
                            val projectDependency = sub.dependencies.project(mapOf("path" to ":${spec.containedType.projectName()}"))
                            sub.dependencies.add("compile", projectDependency)
                        }
                        spec.attributes.forEach {
                            if (it.projectName() != spec.projectName && !it.projectName().isEmpty()) {
                                val projectDependency = sub.dependencies.project(mapOf("path" to ":${it.projectName()}"))
                                sub.dependencies.add("compile", projectDependency)
                            }
                        }
                    }
                }

            }
        }
    }

    private fun collectSpecs(spec: TypeSpec): Set<TypeSpec> =
            when(spec) {
                is CoordinateSpec -> setOf(spec)
                is CoordinatesSpec -> setOf(spec, spec.xType, spec.yType)
                is ContainerSpec -> setOf(spec) + collectSpecs(spec.refType) + collectSpecs(spec.coordinatesType) + collectSpecs(spec.containedType)
                else -> setOf()
            }
}