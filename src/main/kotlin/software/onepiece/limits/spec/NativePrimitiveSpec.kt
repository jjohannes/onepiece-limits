package software.onepiece.limits.spec

import java.util.Locale

class NativePrimitiveSpec(private val name: String, private val typeName: String, private val emptyValue: String = """""""") : CoordinatesSpec {
    override fun generateSizeFieldsSum() = ""
    override fun generateIndexIteratorEntry() = ""
    override fun generateSizeFields() = ""
    override fun projectName() = ""
    override fun propertyName(count: Int) = name.decapitalize(Locale.ROOT)

    override fun typeName() = typeName
    override fun generateEmpty() = emptyValue
    override fun emptyCheck() = " == $emptyValue"
    override fun dataHashCall() = ".hashCode()"
}