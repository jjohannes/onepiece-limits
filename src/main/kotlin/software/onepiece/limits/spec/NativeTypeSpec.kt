package software.onepiece.limits.spec

import java.io.Serializable

data class NativeTypeSpec(val typeName: String) : TypeSpec, Serializable {
    override fun projectName() = ""
    override fun typeName() = typeName
    override fun generateEmpty() = if (typeName == "Int") "0" else """"""""
}