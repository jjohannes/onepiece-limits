package software.onepiece.limits.spec

class NativePrimitiveSpec(val name: String, val typeName: String, val emptyValue: String = """""""") : CoordinatesSpec {
    override fun generateSizeFieldsSum() = ""
    override fun generateIndexIteratorEntry() = ""
    override fun generateSizeFields() = ""
    override fun projectName() = ""
    override fun propertyName(count: Int) = name.decapitalize()

    override fun typeName() = typeName
    override fun generateEmpty() = emptyValue
    override fun dataHashCall() = ".hashCode()"
}