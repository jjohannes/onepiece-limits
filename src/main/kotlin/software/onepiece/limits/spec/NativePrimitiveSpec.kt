package software.onepiece.limits.spec

class NativePrimitiveSpec(val name: String, val typeName: String) : CoordinatesSpec {
    override fun generateSizeFieldsSum() = ""
    override fun generateIndexIteratorEntry() = ""
    override fun generateSizeFields() = ""
    override fun projectName() = ""
    override fun propertyName(count: Int) = name

    override fun typeName() = typeName
    override fun generateEmpty() = if (typeName == "Int") "0" else """"""""
}