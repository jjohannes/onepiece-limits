package software.onepiece.limits

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


class LimitsPlugin: Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        plugins.apply("org.jetbrains.kotlin.jvm")
        plugins.apply("java-library")

        val sourceSets = the<SourceSetContainer>()
        val extension = extensions.create("limits", LimitsPluginExtension::class)

        val generatedSrcFolder = layout.buildDirectory.dir("generated-src")
        dependencies.add("implementation", "com.fasterxml.jackson.core:jackson-annotations:2.12.1")

        the<JavaPluginExtension>().apply {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        val kotlinSourceSet = sourceSets["main"].withConvention(KotlinSourceSet::class) { kotlin }
        kotlinSourceSet.srcDir(generatedSrcFolder)

        val generationTask = tasks.create("generateLimitSources", LimitsGenerationTask::class) {
            packageName.set(extension.packageName)
            typeSpecs.set(extension.specs)
            out.convention(generatedSrcFolder)
        }
        tasks.withType(KotlinCompile::class) {
            dependsOn(generationTask)
        }
        /*specs.forEach { spec ->
            if (spec is ContainerSpec) {
                if (spec.containedType.projectName() != spec.projectName && spec.containedType.projectName().isNotEmpty()) {
                    val projectDependency = dependencies.project(":${spec.containedType.projectName()}")
                    dependencies.add("api", projectDependency)
                }
                if (spec.coordinatesType.projectName() != spec.projectName && spec.coordinatesType.projectName().isNotEmpty()) {
                    val projectDependency = dependencies.project(":${spec.coordinatesType.projectName()}")
                    dependencies.add("api", projectDependency)
                }
                spec.containedSubTypes.forEach {
                    if (it.projectName() != spec.projectName && it.projectName().isNotEmpty()) {
                        val projectDependency = dependencies.project(":${it.projectName()}")
                        dependencies.add("api", projectDependency)
                    }
                }
                spec.attributes.forEach {
                    if (it.projectName() != spec.projectName && it.projectName().isNotEmpty()) {
                        val projectDependency = dependencies.project(":${it.projectName()}")
                        dependencies.add("api", projectDependency)
                    }
                }
            }
        }*/
    }

}