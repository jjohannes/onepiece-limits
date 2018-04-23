package software.onepiece.limits

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import software.onepiece.limits.spec.Spec
import java.io.File

open class LimitsGenerationTask : DefaultTask() {

    @Input
    lateinit var packageName: String

    @Input
    lateinit var typeSpecs: List<Spec>

    @OutputDirectory
    lateinit var out: File

    @TaskAction
    fun generateAll() {
        typeSpecs.forEach { spec ->
            val content = spec.generate(packageName)

            val targetFolder = File(out, "${packageName.replace('.', '/')}/entities/${spec.projectName()}")
            targetFolder.mkdirs()

            File(targetFolder, "${spec.typeName()}.kt").printWriter().use {
                it.print(content)
            }
        }
    }

}