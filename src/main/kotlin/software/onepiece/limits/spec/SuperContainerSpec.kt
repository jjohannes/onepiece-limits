package software.onepiece.limits.spec

class SuperContainerSpec(val projectName: String, val typeName: String, val containedType: Spec) : Spec {
    override fun projectName() = projectName
    override fun typeName() = typeName

    override fun generateEmpty() = "throw IllegalArgumentException()"

    override fun generate(packageName: String) = """
        package $packageName.entities.$projectName

        interface $typeName {
            operator fun get(x: Int, y: Int, container: Any): ${containedType.typeName()}
            fun xMax(): Int
            fun yMax(): Int
        }
    """.trimIndent()
}