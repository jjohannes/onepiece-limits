package software.onepiece.limits.spec

class AdapterSpec(
        val projectName: String,
        val typeName: String,
        val superCoordinatesType: CoordinatesSpec = CoordinateSpec("DUMMY", 0, "DUMMY", "DUMMY"), //FIXME super types for coordinates
        val superType: SuperContainerSpec,
        var attributes: List<Spec> = emptyList()) : AbstractContainerSpec {

    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateEmpty() = "${typeName()}.empty"
    override fun coordinatesType() = superCoordinatesType
    override fun containedType() = superType
    override fun containedLocation(): Nothing? = null
    override fun attributes() = attributes
    override fun containedSubTypes() = emptyList()

    override fun generate(packageName: String) = """
        package $packageName.entities.$projectName

        data class $typeName private constructor(${attributes.joinToString { "val ${it.propertyName()}: ${it.typeName()} = ${it.generateEmpty()}"}}, var implementation: Implementation = NoOp): ${superType.typeName()} {

            interface Implementation  {
                operator fun get(x: Int, y: Int, container: Any) = Sticker.empty
                fun xMax() = 0
                fun yMax() = 0
            }

            private object NoOp : Implementation

            companion object {
                val empty = $typeName()
            }

            override fun get(x: Int, y: Int, container: Any) = implementation[x, y, container]

            override fun xMax() = implementation.xMax()
            override fun yMax() = implementation.yMax()

            ${attributes.joinToString(separator = "\n            ") { "fun with${it.propertyName().capitalize()}(${it.propertyName()}: ${it.typeName()}) = copy(${it.propertyName()} = ${it.propertyName()})" }}
        }
        """.trimIndent()
}