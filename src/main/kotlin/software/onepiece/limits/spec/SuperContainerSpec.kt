package software.onepiece.limits.spec

class SuperContainerSpec(val projectName: String, val typeName: String) : Spec {
    override fun projectName() = projectName
    override fun typeName() = typeName

    override fun generateEmpty() = "$typeName.empty"
    override fun emptyCheck() = ".isEmpty()"
    override fun generate(packageName: String) = """
        package $packageName.entities.$projectName

        interface $typeName {
            fun dataHash() = hashCode()

            companion object {
                val empty = object: $typeName {}
            }
        }
    """.trimIndent()
}