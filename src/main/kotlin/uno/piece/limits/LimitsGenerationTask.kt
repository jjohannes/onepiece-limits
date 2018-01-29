package uno.piece.limits

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import uno.piece.limits.spec.TypeSpec
import java.io.File

open class LimitsGenerationTask : DefaultTask() {

    @Input
    lateinit var packageName: String

    @Input
    lateinit var specs: List<TypeSpec>

    @OutputDirectory
    lateinit var out: File

    @TaskAction
    fun generateAll() {
        specs.forEach { spec ->
            val content = spec.generate(packageName)

            val targetFolder = File(out, "entities/${spec.projectName()}/src/main/kotlin/${packageName.replace('.', '/')}/entities/${spec.projectName()}")
            targetFolder.mkdirs()

            File(targetFolder, "${spec.typeName()}.kt").printWriter().use {
                it.print(content)
            }
        }
    }

}