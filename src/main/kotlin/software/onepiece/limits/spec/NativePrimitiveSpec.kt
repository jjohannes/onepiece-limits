package software.onepiece.limits.spec

import java.io.Serializable

data class NativePrimitiveSpec(val typeName: String) : CoordinatesSpec, Serializable {
    override fun generateSizeFieldsSum() = ""
    override fun generateIndexIteratorEntry() = ""
    override fun generateSizeFields() = ""
    override fun projectName() = ""

    override fun typeName() = typeName
    override fun generateEmpty() = if (typeName == "Int") "0" else """"""""
}