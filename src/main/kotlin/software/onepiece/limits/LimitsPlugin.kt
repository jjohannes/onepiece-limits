package software.onepiece.limits

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet


class LimitsPlugin: Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        val extension = extensions.create("limits", LimitsPluginExtension::class)

        val generationTask = tasks.register("generateLimitSources", LimitsGenerationTask::class) {
            packageName.set(extension.packageName)
            typeSpecs.set(extension.specs)
            out.convention(layout.buildDirectory.dir("generated-src"))
        }

        plugins.withId("org.jetbrains.kotlin.jvm") {
            val sourceSets = the<SourceSetContainer>()
            val kotlinSourceSet = sourceSets["main"].withConvention(KotlinSourceSet::class) { kotlin }
            kotlinSourceSet.srcDir(generationTask.map { it.out })
            dependencies.add("implementation", "com.fasterxml.jackson.core:jackson-annotations:2.12.1")
        }
    }

}