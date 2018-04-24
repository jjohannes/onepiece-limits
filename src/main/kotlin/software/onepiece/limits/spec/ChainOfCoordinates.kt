package software.onepiece.limits.spec

import java.io.Serializable

data class ChainOfCoordinates(val projectName: String, val typeName: String, val rootContainer: String, val components: List<CoordinatesSpec> = emptyList()) : Serializable, Spec {

    override fun projectName() = projectName
    override fun typeName() = typeName

    override fun generateEmpty() = "$typeName.zero"

    override fun generate(packageName: String): String = """
        package $packageName.entities.$projectName

        ${generateImports(packageName)}

        data class $typeName(${components.joinToString { "val ${coordName(it)}: ${it.typeName()}" }}) {
            companion object {
                private val pool = mutableMapOf<Int, $typeName>()
                fun of(${components.joinToString { "${coordName(it)}: ${it.typeName()}" }}) : $typeName {
                    var key = 0
                    ${components.joinToString(separator = "\n                    ") { "key = 31 * key + ${coordName(it)}.hashCode()" }}
                    if (!pool.containsKey(key)) {
                        pool[key] = $typeName(${components.joinToString { coordName(it) }})
                    }
                    return pool[key]!!
                }

                val zero = of(${components.joinToString { "${it.typeName()}.zero" }})
            }

            operator fun plus(other: $typeName) = of(${components.joinToString { "${coordName(it)} + other.${coordName(it)}" }})

            operator fun minus(other: $typeName) = of(${components.joinToString { "${coordName(it)} - other.${coordName(it)}" }})
        }
    """.trimIndent()

    private fun generateImports(basePackageName: String) =
            components.joinToString(separator = "; ") { "import $basePackageName.entities.${it.projectName()}.*" }

    private fun coordName(spec: CoordinatesSpec) = spec.typeName().decapitalize()

}