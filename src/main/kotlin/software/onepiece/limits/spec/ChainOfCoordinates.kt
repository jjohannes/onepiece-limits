package software.onepiece.limits.spec

import java.io.Serializable

class ChainOfCoordinates(val projectName: String, val typeName: String, val rootContainer: String, val components: List<CoordinatesSpec> = emptyList(), val componentTypes: Map<Int, String> = emptyMap(), val componentAccess: Map<Int, String> = emptyMap()) : Spec {

    override fun projectName() = projectName
    override fun typeName() = typeName

    override fun generateEmpty() = "$typeName.zero"

    override fun generate(packageName: String): String = """
        package $packageName.entities.$projectName

        ${generateImports(packageName)}

        data class $typeName(${(0 until components.size).joinToString { "val ${coordName(it)}: ${components[it].typeName()}" }}) {
            companion object {
                private val pool = mutableMapOf<Int, $typeName>()
                fun of(${(0 until components.size).joinToString { "${coordName(it)}: ${components[it].typeName()}" }}) : $typeName {
                    var key = 0
                    ${(0 until components.size).joinToString(separator = "\n                    ") { "key = 31 * key + ${coordName(it)}.hashCode()" }}
                    if (!pool.containsKey(key)) {
                        pool[key] = $typeName(${(0 until components.size).joinToString { coordName(it) }})
                    }
                    return pool[key]!!
                }

                val zero = of(${components.joinToString { "${it.typeName()}.zero" }})
            }

            operator fun plus(other: $typeName) = of(${(0 until components.size).joinToString { "${coordName(it)} + other.${coordName(it)}" }})

            operator fun minus(other: $typeName) = of(${(0 until components.size).joinToString { "${coordName(it)} - other.${coordName(it)}" }})
        }
    """.trimIndent()

    private fun generateImports(basePackageName: String) =
            components.joinToString(separator = "; ") { "import $basePackageName.entities.${it.projectName()}.*" }

    fun coordName(idx: Int) = if (componentTypes.containsKey(idx)) componentTypes[idx]!! + components[idx].typeName() else components[idx].propertyName()

}