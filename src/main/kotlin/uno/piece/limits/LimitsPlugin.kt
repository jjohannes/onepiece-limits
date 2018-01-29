package uno.piece.limits

import org.gradle.api.Plugin
import org.gradle.api.Project
import uno.piece.limits.spec.ContainerSpec
import uno.piece.limits.spec.CoordinateSpec
import uno.piece.limits.spec.PositionSpec
import uno.piece.limits.spec.TypeSpec

class LimitsPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("limits", LimitsPluginExtension::class.java)
        val generationTask = project.tasks.create("generateLimits", LimitsGenerationTask::class.java)

        project.afterEvaluate {
            val allSpecs = extension.specs.map { spec -> collectSpecs(spec) }.flatten()
            with(generationTask) {
                packageName = extension.packageName
                specs = allSpecs
                out = project.projectDir
            }
            allSpecs.forEach { spec ->
                val sub = project.findProject(":${spec.projectName()}") ?:
                        throw RuntimeException("Project '${spec.projectName()}' needs to be included in build")

                sub.plugins.apply("org.jetbrains.kotlin.jvm")
                sub.dependencies.add("compile", "org.jetbrains.kotlin:kotlin-stdlib-jre8:1.2.21")
                sub.tasks.findByName("compileKotlin")!!.dependsOn(generationTask)
                if (spec is ContainerSpec) {
                    if (spec.refType.projectName() != spec.projectName && !spec.refType.projectName().isEmpty()) {
                        val projectDependency = sub.dependencies.project(mapOf("path" to ":${spec.refType.projectName()}"))
                        sub.dependencies.add("compile", projectDependency)
                    }
                    if (spec.containedType != spec.refType && spec.containedType.projectName() != spec.projectName && !spec.containedType.projectName().isEmpty()) {
                        val projectDependency = sub.dependencies.project(mapOf("path" to ":${spec.containedType.projectName()}"))
                        sub.dependencies.add("compile", projectDependency)
                    }
                }
            }
        }
    }

    private fun collectSpecs(spec: TypeSpec): Set<TypeSpec> =
            when(spec) {
                is CoordinateSpec -> setOf(spec, spec.xType, spec.yType)
                is ContainerSpec -> setOf(spec) + collectSpecs(spec.coordinateType) + collectSpecs(spec.containedType)
                is PositionSpec -> setOf(spec)
                else -> setOf()
            }
}