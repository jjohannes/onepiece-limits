package software.onepiece.limits.spec

class SuperContainerSpec(val projectName: String, val typeName: String, val containedType: Spec) : Spec {
    override fun projectName() = projectName
    override fun typeName() = typeName

    override fun generateEmpty() = "throw IllegalArgumentException()"
    override fun emptyCheck() = ".isEmpty()"
    override fun generate(packageName: String) = """
        package $packageName.entities.$projectName

        interface $typeName {
            fun dataHash() = hashCode()
        }
    """.trimIndent()
}